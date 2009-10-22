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

package org.apache.tuscany.sca.osgi.remoteserviceadmin.impl;

import static org.apache.tuscany.sca.assembly.Base.SCA11_TUSCANY_NS;
import static org.apache.tuscany.sca.implementation.osgi.OSGiProperty.SCA_BINDINGS;
import static org.apache.tuscany.sca.implementation.osgi.OSGiProperty.SERVICE_EXPORTED_INTENTS;
import static org.apache.tuscany.sca.implementation.osgi.OSGiProperty.SERVICE_EXPORTED_INTENTS_EXTRA;
import static org.apache.tuscany.sca.implementation.osgi.OSGiProperty.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.tuscany.sca.assembly.AssemblyFactory;
import org.apache.tuscany.sca.assembly.Base;
import org.apache.tuscany.sca.assembly.Binding;
import org.apache.tuscany.sca.assembly.Component;
import org.apache.tuscany.sca.assembly.ComponentReference;
import org.apache.tuscany.sca.assembly.ComponentService;
import org.apache.tuscany.sca.assembly.Composite;
import org.apache.tuscany.sca.assembly.Endpoint;
import org.apache.tuscany.sca.assembly.Reference;
import org.apache.tuscany.sca.assembly.Service;
import org.apache.tuscany.sca.contribution.Contribution;
import org.apache.tuscany.sca.contribution.ContributionFactory;
import org.apache.tuscany.sca.contribution.processor.ContributionReadException;
import org.apache.tuscany.sca.contribution.processor.ExtensibleStAXArtifactProcessor;
import org.apache.tuscany.sca.contribution.processor.ProcessorContext;
import org.apache.tuscany.sca.contribution.processor.StAXArtifactProcessor;
import org.apache.tuscany.sca.contribution.processor.StAXArtifactProcessorExtensionPoint;
import org.apache.tuscany.sca.contribution.resolver.ExtensibleModelResolver;
import org.apache.tuscany.sca.contribution.resolver.ModelResolver;
import org.apache.tuscany.sca.contribution.resolver.ModelResolverExtensionPoint;
import org.apache.tuscany.sca.core.ExtensionPointRegistry;
import org.apache.tuscany.sca.core.FactoryExtensionPoint;
import org.apache.tuscany.sca.core.UtilityExtensionPoint;
import org.apache.tuscany.sca.implementation.osgi.BindingDescriptions;
import org.apache.tuscany.sca.implementation.osgi.OSGiImplementation;
import org.apache.tuscany.sca.implementation.osgi.OSGiImplementationFactory;
import org.apache.tuscany.sca.implementation.osgi.OSGiProperty;
import org.apache.tuscany.sca.implementation.osgi.ServiceDescriptionsFactory;
import org.apache.tuscany.sca.interfacedef.java.JavaInterface;
import org.apache.tuscany.sca.interfacedef.java.JavaInterfaceContract;
import org.apache.tuscany.sca.interfacedef.java.JavaInterfaceFactory;
import org.apache.tuscany.sca.monitor.MonitorFactory;
import org.apache.tuscany.sca.osgi.remoteserviceadmin.EndpointDescription;
import org.apache.tuscany.sca.policy.Intent;
import org.apache.tuscany.sca.policy.PolicyFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Introspect an OSGi Service to create an SCA composite that contains a single component with
 * implementation.osgi
 */
public class EndpointIntrospector {
    private AssemblyFactory assemblyFactory;
    private ContributionFactory contributionFactory;
    private OSGiImplementationFactory implementationFactory;
    private ServiceDescriptionsFactory serviceDescriptionsFactory;
    private PolicyFactory policyFactory;
    private ExtensionPointRegistry registry;
    private FactoryExtensionPoint factories;
    private ModelResolverExtensionPoint modelResolvers;
    private XMLInputFactory xmlInputFactory;
    private XMLOutputFactory xmlOutputFactory;
    private JavaInterfaceFactory javaInterfaceFactory;
    private StAXArtifactProcessor processor;

    /**
     * @param intentName
     * @return
     */
    private static QName getQName(String intentName) {
        QName qname;
        if (intentName.startsWith("{")) {
            int i = intentName.indexOf('}');
            if (i != -1) {
                qname = new QName(intentName.substring(1, i), intentName.substring(i + 1));
            } else {
                throw new IllegalArgumentException("Invalid intent: " + intentName);
            }
        } else {
            // Default to SCA namespace
            qname = new QName(Base.SCA11_NS, intentName);
        }
        return qname;
    }

    /**
     * @param context TODO
     * @param registry
     */
    public EndpointIntrospector(BundleContext context, ExtensionPointRegistry registry) {
        super();
        this.registry = registry;
        this.factories = registry.getExtensionPoint(FactoryExtensionPoint.class);
        this.modelResolvers = registry.getExtensionPoint(ModelResolverExtensionPoint.class);
        this.assemblyFactory = factories.getFactory(AssemblyFactory.class);
        this.contributionFactory = factories.getFactory(ContributionFactory.class);
        this.policyFactory = factories.getFactory(PolicyFactory.class);
        this.implementationFactory = factories.getFactory(OSGiImplementationFactory.class);
        this.serviceDescriptionsFactory = factories.getFactory(ServiceDescriptionsFactory.class);
        this.xmlInputFactory = factories.getFactory(XMLInputFactory.class);
        this.xmlOutputFactory = factories.getFactory(XMLOutputFactory.class);
        this.javaInterfaceFactory = factories.getFactory(JavaInterfaceFactory.class);
        StAXArtifactProcessorExtensionPoint processors =
            registry.getExtensionPoint(StAXArtifactProcessorExtensionPoint.class);
        UtilityExtensionPoint utilities = this.registry.getExtensionPoint(UtilityExtensionPoint.class);
        MonitorFactory monitorFactory = utilities.getUtility(MonitorFactory.class);
        processor = new ExtensibleStAXArtifactProcessor(processors, xmlInputFactory, xmlOutputFactory);
    }

    private Intent getIntent(String intent) {
        QName name = getQName(intent);
        Intent i = policyFactory.createIntent();
        i.setName(name);
        return i;
    }

    private List<Intent> getIntents(String[] intents) {
        if (intents == null || intents.length == 0) {
            return Collections.emptyList();
        }
        List<Intent> intentList = new ArrayList<Intent>();
        for (String i : intents) {
            Intent intent = getIntent(i);
            if (intent != null) {
                intentList.add(intent);
            }
        }
        return intentList;
    }

    /**
     * Any property in the map overrides the service reference properties, regardless of
     * case. That is, if the map contains a key then it will override any case variant
     * of this key in the Service Reference.<p>
     * If the map contains the objectClass or service. id property key in any case
     * variant, then these properties must not override the Service Reference�s value. This 
     * implies that the map can provide the service.exported. interfaces, property allowing 
     * the Topology Manager to export any registered service, also services not specifically 
     * marked to be exported.
     * @param reference
     * @param props
     * @return
     */
    private Map<String, Object> getProperties(ServiceReference reference, Map<String, Object> props) {
        String[] names = reference.getPropertyKeys();
        Map<String, Object> properties = new HashMap<String, Object>();
        if (names != null) {
            for (String name : names) {
                properties.put(name, reference.getProperty(name));
            }
        }
        if (props != null) {
            // Create a map of names (key = lowcase name, value = name)
            Map<String, String> nameMap = new HashMap<String, String>();
            if (names != null) {
                for (String name : names) {
                    nameMap.put(name.toLowerCase(), name);
                }
            }
            for (Map.Entry<String, Object> p : props.entrySet()) {
                if (Constants.OBJECTCLASS.equalsIgnoreCase(p.getKey())) {
                    throw new IllegalArgumentException(Constants.OBJECTCLASS + " property cannot be overridden.");
                } else if (Constants.SERVICE_ID.equalsIgnoreCase(p.getKey())) {
                    throw new IllegalArgumentException(Constants.SERVICE_ID + " property cannot be overridden.");
                }
                String key = nameMap.get(p.getKey().toLowerCase());
                if (key != null) {
                    properties.put(key, p.getValue());
                } else {
                    properties.put(p.getKey(), p.getValue());
                }
            }
        }
        return properties;
    }

    /**
     * Parse the Stringp[] to support values that are separated by comma
     * @param interfaces
     * @return
     */
    private String[] parse(String[] interfaces) {
        if (interfaces == null) {
            return null;
        }
        List<String> names = new ArrayList<String>();
        for (String i : interfaces) {
            String[] parts = i.split(",");
            for (String p : parts) {
                names.add(p.trim());
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /**
     * Introspect a local OSGi Service represented by the ServiceReference to create 
     * an SCA service with the required intents and bindings 
     * @param reference The service reference for a local OSGi service 
     * @param props Addiontal properties
     * @return An SCA contribution with a deployable composite for the SCA service
     * @throws Exception
     */
    public Contribution introspect(ServiceReference reference, Map<String, Object> props) throws Exception {
        Map<String, Object> properties = getProperties(reference, props);

        OSGiProperty serviceID = implementationFactory.createOSGiProperty();
        serviceID.setName(SERVICE_ID);
        // The service.id is Long
        serviceID.setValue(String.valueOf(reference.getProperty(SERVICE_ID)));

        String id = "osgi.service." + UUID.randomUUID();
        Composite composite = assemblyFactory.createComposite();
        composite.setName(new QName(SCA11_TUSCANY_NS, id));

        Component component = assemblyFactory.createComponent();
        component.setName(id);
        component.setAutowire(Boolean.TRUE);

        composite.getComponents().add(component);

        Bundle bundle = reference.getBundle();
        OSGiImplementation implementation = implementationFactory.createOSGiImplementation();

        implementation.setBundle(bundle);
        component.setImplementation(implementation);
        implementation.setUnresolved(false);

        String[] remoteInterfaces = getStrings(reference.getProperty(SERVICE_EXPORTED_INTERFACES));
        if (remoteInterfaces == null || remoteInterfaces.length > 0 && "*".equals(remoteInterfaces[0])) {
            remoteInterfaces = getStrings(reference.getProperty(OBJECTCLASS));
        } else {
            remoteInterfaces = parse(remoteInterfaces);
            String[] objectClasses = getStrings(reference.getProperty(OBJECTCLASS));
            Set<String> objectClassSet = new HashSet<String>(Arrays.asList(objectClasses));
            if (!objectClassSet.containsAll(Arrays.asList(remoteInterfaces))) {
                throw new IllegalArgumentException("The exported interfaces are not a subset of the types" 
                                                   + " listed in the objectClass service property from the Service Reference");
            }
        }
        for (String intf : remoteInterfaces) {
            Service service = assemblyFactory.createService();
            JavaInterfaceContract interfaceContract = javaInterfaceFactory.createJavaInterfaceContract();
            Class<?> interfaceClass = bundle.loadClass(intf);
            JavaInterface javaInterface = javaInterfaceFactory.createJavaInterface(interfaceClass);
            interfaceContract.setInterface(javaInterface);
            if (javaInterface.getCallbackClass() != null) {
                interfaceContract.setCallbackInterface(javaInterfaceFactory.createJavaInterface(javaInterface
                    .getCallbackClass()));
            }

            service.setName(interfaceClass.getSimpleName());
            service.setInterfaceContract(interfaceContract);

            service.getExtensions().add(serviceID);

            implementation.getServices().add(service);

            ComponentService componentService = assemblyFactory.createComponentService();
            componentService.setName(service.getName());
            component.getServices().add(componentService);
            componentService.setService(service);
        }

        String[] requiredIntents = getStrings(properties.get(SERVICE_EXPORTED_INTENTS));
        List<Intent> intents = getIntents(requiredIntents);
        String[] requiredIntentsExtra = getStrings(properties.get(SERVICE_EXPORTED_INTENTS_EXTRA));
        List<Intent> extraIntents = getIntents(requiredIntentsExtra);

        String[] bindingDocuments = getStrings(properties.get(SCA_BINDINGS));
        List<Binding> bindings = loadBindings(reference.getBundle(), bindingDocuments);

        for (ComponentService componentService : component.getServices()) {
            componentService.getRequiredIntents().addAll(intents);
            componentService.getRequiredIntents().addAll(extraIntents);
            componentService.getBindings().addAll(bindings);
        }

        // FIXME: Should we scan the owning bundle to create the SCA contribution?
        Contribution contribution = contributionFactory.createContribution();
        contribution.setURI("urn:" + id);
        contribution.setLocation(bundle.getEntry("/").toString());
        contribution.getDeployables().add(composite);
        ModelResolver modelResolver = new ExtensibleModelResolver(contribution, modelResolvers, factories);
        contribution.setModelResolver(modelResolver);
        contribution.setUnresolved(true);
        return contribution;
    }

    public Contribution introspect(Bundle bundle, EndpointDescription endpoint) throws Exception {
        Endpoint ep = (Endpoint) endpoint.getProperties().get(Endpoint.class.getName());
        if (ep != null) {
            return introspect(bundle, ep);
        }
        Map<String, Object> properties = endpoint.getProperties();
        List<String> remoteInterfaces = endpoint.getInterfaces();

        String id = "osgi.reference." + UUID.randomUUID();
        Composite composite = assemblyFactory.createComposite();
        composite.setName(new QName(Base.SCA11_TUSCANY_NS, id));

        Component component = assemblyFactory.createComponent();
        component.setName(id);
        // component.setAutowire(Boolean.TRUE);

        composite.getComponents().add(component);

        OSGiImplementation implementation = implementationFactory.createOSGiImplementation();

        implementation.setBundle(bundle);
        component.setImplementation(implementation);
        implementation.setUnresolved(false);

        int count = 0;
        for (String intf : remoteInterfaces) {
            Reference reference = assemblyFactory.createReference();
            JavaInterfaceContract interfaceContract = javaInterfaceFactory.createJavaInterfaceContract();
            Class<?> interfaceClass = bundle.loadClass(intf);
            JavaInterface javaInterface = javaInterfaceFactory.createJavaInterface(interfaceClass);
            interfaceContract.setInterface(javaInterface);
            if (javaInterface.getCallbackClass() != null) {
                interfaceContract.setCallbackInterface(javaInterfaceFactory.createJavaInterface(javaInterface
                    .getCallbackClass()));
            }

            reference.setName("ref" + (count++));
            reference.setInterfaceContract(interfaceContract);

            implementation.getReferences().add(reference);

            ComponentReference componentReference = assemblyFactory.createComponentReference();
            componentReference.setName(reference.getName());
            component.getReferences().add(componentReference);
            componentReference.setReference(reference);
            componentReference.setWiredByImpl(true);
        }

        String[] requiredIntents = getStrings(properties.get(SERVICE_EXPORTED_INTENTS));
        List<Intent> intents = getIntents(requiredIntents);

        String[] bindingDocuments = getStrings(properties.get(SCA_BINDINGS));
        List<Binding> bindings = loadBindings(bundle, bindingDocuments);

        List<Binding> bindingList = new ArrayList<Binding>(bindings);
        BindingDescriptions bindingDescriptions =
            (BindingDescriptions)properties.get(BindingDescriptions.BINDINGS_QNAME.toString());
        if (bindingDescriptions != null) {
            bindingList.addAll(bindingDescriptions);
        }

        for (ComponentReference componentReference : component.getReferences()) {
            componentReference.getRequiredIntents().addAll(intents);
            componentReference.getBindings().addAll(bindingList);
        }
        // FIXME: Should we scan the owning bundle to create the SCA contribution?
        Contribution contribution = contributionFactory.createContribution();
        contribution.setURI("urn:" + id);
        contribution.setLocation(bundle.getEntry("/").toString());
        contribution.getDeployables().add(composite);
        ModelResolver modelResolver = new ExtensibleModelResolver(contribution, modelResolvers, factories);
        contribution.setModelResolver(modelResolver);
        contribution.setUnresolved(true);
        return contribution;
    }

    public Contribution introspect(Bundle bundle, Endpoint endpoint) throws Exception {
        String id = "osgi.reference." + UUID.randomUUID();
        Composite composite = assemblyFactory.createComposite();
        composite.setName(new QName(Base.SCA11_TUSCANY_NS, id));

        Component component = assemblyFactory.createComponent();
        component.setName(id);
        // component.setAutowire(Boolean.TRUE);

        composite.getComponents().add(component);

        OSGiImplementation implementation = implementationFactory.createOSGiImplementation();

        implementation.setBundle(bundle);
        component.setImplementation(implementation);
        implementation.setUnresolved(false);

        Reference reference = assemblyFactory.createReference();
        Service service = endpoint.getService().getService();
        reference.setInterfaceContract(service.getInterfaceContract());
        reference.setName("ref");

        reference.getBindings().add(endpoint.getBinding());

        /*
        reference.getRequiredIntents().addAll(service.getRequiredIntents());
        reference.getPolicySets().addAll(service.getPolicySets());
        */

        implementation.getReferences().add(reference);

        ComponentReference componentReference = assemblyFactory.createComponentReference();
        component.getReferences().add(componentReference);
        componentReference.setReference(reference);
        componentReference.setName(reference.getName());
        componentReference.setWiredByImpl(true);

        // FIXME: Should we scan the owning bundle to create the SCA contribution?
        Contribution contribution = contributionFactory.createContribution();
        contribution.setURI("urn:" + id);
        contribution.setLocation(bundle.getEntry("/").toString());
        contribution.getDeployables().add(composite);
        ModelResolver modelResolver = new ExtensibleModelResolver(contribution, modelResolvers, factories);
        contribution.setModelResolver(modelResolver);
        contribution.setUnresolved(true);
        return contribution;
    }

    private List<Binding> loadBindings(Bundle bundle, String[] bindingDocuments) throws IOException,
        ContributionReadException {
        if (bindingDocuments == null || bindingDocuments.length == 0) {
            return Collections.emptyList();
        }
        List<Binding> bindings = new ArrayList<Binding>();
        for (String doc : bindingDocuments) {
            URL url = locate(bundle, doc);
            if (url == null) {
                throw new IOException("Entry " + doc + " cannot be found in bundle " + bundle);
            }
            bindings.addAll(loadBindings(url));
        }
        return bindings;
    }

    private List<Binding> loadBindings(URL url) throws ContributionReadException, IOException {
        InputStream is = url.openStream();
        try {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);
            reader.nextTag();
            Object model = processor.read(reader, new ProcessorContext(registry));
            if (model instanceof BindingDescriptions) {
                return ((BindingDescriptions)model);
            } else {
                return Collections.emptyList();
            }
        } catch (XMLStreamException e) {
            throw new ContributionReadException(e);
        } finally {
            is.close();
        }
    }

    private URL locate(Bundle bundle, String location) throws MalformedURLException {
        URI uri = URI.create(location);
        if (uri.isAbsolute()) {
            return uri.toURL();
        }
        return bundle.getEntry(location);
    }

    /**
     * In OSGi, the value of String+ can be a single String, String[] or Collection<String>
     * @param value
     * @return
     */
    private String[] getStrings(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return new String[] {(String)value};
        } else if (value instanceof Collection) {
            Collection<String> collection = (Collection)value;
            return collection.toArray(new String[collection.size()]);
        }
        return (String[])value;

    }

}