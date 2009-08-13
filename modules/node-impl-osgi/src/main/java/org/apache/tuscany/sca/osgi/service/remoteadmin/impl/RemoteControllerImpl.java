/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.tuscany.sca.osgi.service.remoteadmin.impl;

import static org.apache.tuscany.sca.implementation.osgi.OSGiProperty.SERVICE_EXPORTED_INTERFACES;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.EXPORT_ERROR;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.EXPORT_REGISTRATION;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.EXPORT_UNREGISTRATION;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.EXPORT_WARNING;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.IMPORT_ERROR;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.IMPORT_REGISTRATION;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.IMPORT_UNREGISTRATION;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent.IMPORT_WARNING;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteConstants.SERVICE_IMPORTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.tuscany.sca.core.LifeCycleListener;
import org.apache.tuscany.sca.osgi.service.remoteadmin.EndpointDescription;
import org.apache.tuscany.sca.osgi.service.remoteadmin.EndpointListener;
import org.apache.tuscany.sca.osgi.service.remoteadmin.ExportRegistration;
import org.apache.tuscany.sca.osgi.service.remoteadmin.ImportRegistration;
import org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteServiceAdmin;
import org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent;
import org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Implementation of Remote Controller
 */
public class RemoteControllerImpl implements ListenerHook, RemoteAdminListener, EndpointListener,
    ServiceTrackerCustomizer, LifeCycleListener /*, EventHook */{
    private final static Logger logger = Logger.getLogger(RemoteControllerImpl.class.getName());
    public final static String ENDPOINT_LOCAL = "service.local";

    private BundleContext context;
    private ServiceTracker remoteAdmins;

    private ServiceRegistration registration;
    private ServiceRegistration endpointListener;

    private ServiceTracker remotableServices;

    // Service listeners keyed by the filter
    private MappedCollections<String, ListenerInfo> serviceListeners = new MappedCollections<String, ListenerInfo>();

    private MappedCollections<ServiceReference, ExportRegistration> exportedServices =
        new MappedCollections<ServiceReference, ExportRegistration>();
    private MappedCollections<EndpointDescription, ImportRegistration> importedServices =
        new MappedCollections<EndpointDescription, ImportRegistration>();

    private Filter remotableServiceFilter;

    public RemoteControllerImpl(BundleContext context) {
        this.context = context;
    }

    public void start() {
        String filter =
            "(& (!(" + SERVICE_IMPORTED
                + "=*)) ("
                + SERVICE_EXPORTED_INTERFACES
                + "=*) ("
                + SERVICE_EXPORTED_CONFIGS
                + "=sca) )";
        try {
            remotableServiceFilter = context.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            // Ignore
        }

        endpointListener = context.registerService(EndpointListener.class.getName(), this, null);
        remoteAdmins = new ServiceTracker(this.context, RemoteServiceAdmin.class.getName(), null);
        remoteAdmins.open();

        // DO NOT register EventHook.class.getName() as it cannot report existing services
        String interfaceNames[] = new String[] {ListenerHook.class.getName(), RemoteAdminListener.class.getName()};
        // The registration will trigger the added() method before registration is assigned
        registration = context.registerService(interfaceNames, this, null);

        remotableServices = new ServiceTracker(context, remotableServiceFilter, this);
        remotableServices.open(true);

    }

    /**
     * @see org.osgi.framework.hooks.service.EventHook#event(org.osgi.framework.ServiceEvent,
     *      java.util.Collection)
     */
    /*
    public void event(ServiceEvent event, Collection contexts) {
        ServiceReference reference = event.getServiceReference();
        if (!remotableServiceFilter.match(reference)) {
            // Only export remotable services that are for SCA
            return;
        }
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                exportService(reference);
                break;
            case ServiceEvent.UNREGISTERING:
                unexportService(reference);
                break;
            case ServiceEvent.MODIFIED:
            case ServiceEvent.MODIFIED_ENDMATCH:
                // First check if the property changes will impact
                // Call remote admin to update the service
                unexportService(reference);
                exportService(reference);
                break;
        }
    }
    */

    public Object addingService(ServiceReference reference) {
        exportService(reference);
        return reference.getBundle().getBundleContext().getService(reference);
    }

    public void modifiedService(ServiceReference reference, Object service) {
        unexportService(reference);
        exportService(reference);
    }

    public void removedService(ServiceReference reference, Object service) {
        unexportService(reference);
    }

    private void unexportService(ServiceReference reference) {
        // Call remote admin to unexport the service
        Collection<ExportRegistration> exportRegistrations = exportedServices.get(reference);
        if (exportRegistrations != null) {
            for (Iterator<ExportRegistration> i = exportRegistrations.iterator(); i.hasNext();) {
                ExportRegistration exported = i.next();
                exported.close();
                i.remove();
            }
        }
    }

    private void exportService(ServiceReference reference) {
        // Call remote admin to export the service
        Object[] admins = remoteAdmins.getServices();
        if (admins == null) {
            // Ignore
            logger.warning("No RemoteAdmin services are available.");
        } else {
            for (Object ra : admins) {
                RemoteServiceAdmin remoteAdmin = (RemoteServiceAdmin)ra;
                List<ExportRegistration> exportRegistrations = remoteAdmin.exportService(reference);
                if (exportRegistrations != null && !exportRegistrations.isEmpty()) {
                    exportedServices.putValue(reference, exportRegistrations);
                }
            }
        }
    }

    /**
     * @see org.osgi.framework.hooks.service.ListenerHook#added(java.util.Collection)
     */
    public void added(Collection listeners) {
        Collection<ListenerInfo> listenerInfos = (Collection<ListenerInfo>)listeners;
        boolean changed = false;
        for (ListenerInfo l : listenerInfos) {
            if (!l.isRemoved() && l.getBundleContext() != context) {
                String key = l.getFilter();
                if (key == null) {
                    // key = "";
                    // FIXME: It should always match, let's ignore it for now
                    logger.warning("Service listner without a filter is skipped: " + l);
                    continue;
                }
                Collection<ListenerInfo> infos = serviceListeners.get(key);
                if (infos == null) {
                    infos = new HashSet<ListenerInfo>();
                    serviceListeners.put(key, infos);
                }
                infos.add(l);
                changed = true;
            }
        }
        if (changed) {
            updateEndpointListenerScope();
        }
    }

    private void updateEndpointListenerScope() {
        Set<String> filterSet = serviceListeners.keySet();

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ENDPOINT_LISTENER_SCOPE, filterSet.toArray(new String[filterSet.size()]));
        endpointListener.setProperties(props);
    }

    private MappedCollections<Class<?>, ListenerInfo> findServiceListeners(EndpointDescription endpointDescription,
                                                                           String matchedFilter) {
        // First find all the listeners that have the matching filter
        Collection<ListenerInfo> listeners = serviceListeners.get(matchedFilter);
        if (listeners == null) {
            return null;
        }

        // Try to partition the listeners by the interface classes 
        List<String> interfaceNames = endpointDescription.getInterfaces();
        MappedCollections<Class<?>, ListenerInfo> interfaceToListeners =
            new MappedCollections<Class<?>, ListenerInfo>();
        for (String i : interfaceNames) {
            for (ListenerInfo listener : listeners) {
                try {
                    Class<?> interfaceClass = listener.getBundleContext().getBundle().loadClass(i);
                    interfaceToListeners.putValue(interfaceClass, listener);
                } catch (ClassNotFoundException e) {
                    // Ignore the listener as it cannot load the interface class
                }
            }
        }
        return interfaceToListeners;
    }

    /**
     * @see org.osgi.framework.hooks.service.ListenerHook#removed(java.util.Collection)
     */
    public void removed(Collection listeners) {
        Collection<ListenerInfo> listenerInfos = (Collection<ListenerInfo>)listeners;
        boolean changed = false;
        for (ListenerInfo l : listenerInfos) {
            if (registration != null && l.getBundleContext() != context) {
                String key = l.getFilter();
                if (key == null) {
                    continue;
                }
                if (serviceListeners.removeValue(key, l)) {
                    changed = true;
                }
            }
        }
        if (changed) {
            updateEndpointListenerScope();
        }
    }

    /**
     * @see org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminListener#remoteAdminEvent(org.apache.tuscany.sca.osgi.service.remoteadmin.RemoteAdminEvent)
     */
    public void remoteAdminEvent(RemoteAdminEvent event) {
        switch (event.getType()) {
            case EXPORT_ERROR:
            case EXPORT_REGISTRATION:
            case EXPORT_UNREGISTRATION:
            case EXPORT_WARNING:
                break;
            case IMPORT_ERROR:
            case IMPORT_REGISTRATION:
            case IMPORT_UNREGISTRATION:
            case IMPORT_WARNING:
                break;
        }
    }

    /**
     * @see org.apache.tuscany.sca.osgi.service.remoteadmin.EndpointListener#addEndpoint(org.apache.tuscany.sca.osgi.service.remoteadmin.EndpointDescription,
     *      java.lang.String)
     */
    public void addEndpoint(EndpointDescription endpoint, String matchedFilter) {
        importService(endpoint, matchedFilter);
    }

    /**
     * @see org.apache.tuscany.sca.osgi.service.remoteadmin.EndpointListener#removeEndpoint(org.apache.tuscany.sca.osgi.service.remoteadmin.EndpointDescription)
     */
    public void removeEndpoint(EndpointDescription endpoint) {
        unimportService(endpoint);
    }

    private void importService(EndpointDescription endpoint, String matchedFilter) {
        Object[] admins = remoteAdmins.getServices();
        if (admins == null) {
            logger.warning("No RemoteAdmin services are available.");
            return;
        }

        MappedCollections<Class<?>, ListenerInfo> interfaceToListeners = findServiceListeners(endpoint, matchedFilter);
        for (Map.Entry<Class<?>, Collection<ListenerInfo>> e : interfaceToListeners.entrySet()) {
            Class<?> interfaceClass = e.getKey();
            Collection<ListenerInfo> listeners = e.getValue();
            // Get a listener
            ListenerInfo listener = listeners.iterator().next();
            Bundle bundle = listener.getBundleContext().getBundle();

            Map<String, Object> props = new HashMap<String, Object>(endpoint.getProperties());
            props.put(Bundle.class.getName(), bundle);
            EndpointDescription description =
                new EndpointDescriptionImpl(Collections.singletonList(interfaceClass.getName()), props);

            if (admins != null) {
                for (Object ra : admins) {
                    RemoteServiceAdmin remoteAdmin = (RemoteServiceAdmin)ra;
                    ImportRegistration importRegistration = remoteAdmin.importService(description);
                    if (importRegistration != null) {
                        importedServices.putValue(endpoint, importRegistration);
                    }
                }
            }
        }
    }

    private void unimportService(EndpointDescription endpoint) {
        // Call remote admin to unimport the service
        Collection<ImportRegistration> importRegistrations = importedServices.get(endpoint);
        if (importRegistrations != null) {
            for (Iterator<ImportRegistration> i = importRegistrations.iterator(); i.hasNext();) {
                ImportRegistration imported = i.next();
                imported.close();
                i.remove();
            }
        }
    }

    public void stop() {
        remotableServices.close();

        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        if (remoteAdmins != null) {
            remoteAdmins.close();
            remoteAdmins = null;
        }
        for (Collection<ListenerInfo> infos : serviceListeners.values()) {
        }
        serviceListeners.clear();
    }

    private static class MappedCollections<K, V> extends HashMap<K, Collection<V>> {
        private static final long serialVersionUID = -8926174610229029369L;

        public boolean putValue(K key, V value) {
            Collection<V> collection = get(key);
            if (collection == null) {
                collection = new ArrayList<V>();
                put(key, collection);
            }
            return collection.add(value);
        }

        public boolean putValue(K key, Collection<? extends V> value) {
            Collection<V> collection = get(key);
            if (collection == null) {
                collection = new ArrayList<V>();
                put(key, collection);
            }
            return collection.addAll(value);
        }

        public boolean removeValue(K key, V value) {
            Collection<V> collection = get(key);
            if (collection == null) {
                return false;
            }
            return collection.remove(value);
        }

    }

}
