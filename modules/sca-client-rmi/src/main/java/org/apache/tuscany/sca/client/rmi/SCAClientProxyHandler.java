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

package org.apache.tuscany.sca.client.rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import org.apache.tuscany.sca.assembly.AssemblyFactory;
import org.apache.tuscany.sca.assembly.Endpoint;
import org.apache.tuscany.sca.assembly.EndpointReference;
import org.apache.tuscany.sca.binding.rmi.provider.RMIBindingInvoker;
import org.apache.tuscany.sca.core.ExtensionPointRegistry;
import org.apache.tuscany.sca.core.FactoryExtensionPoint;
import org.apache.tuscany.sca.core.UtilityExtensionPoint;
import org.apache.tuscany.sca.host.rmi.ExtensibleRMIHost;
import org.apache.tuscany.sca.host.rmi.RMIHost;
import org.apache.tuscany.sca.host.rmi.RMIHostExtensionPoint;
import org.apache.tuscany.sca.node.Node;
import org.apache.tuscany.sca.node.NodeFactory;
import org.apache.tuscany.sca.node.impl.NodeFactoryImpl;
import org.apache.tuscany.sca.runtime.DomainRegistryFactory;
import org.apache.tuscany.sca.runtime.EndpointRegistry;
import org.oasisopen.sca.NoSuchServiceException;

public class SCAClientProxyHandler implements InvocationHandler {

    protected NodeFactoryImpl nodeFactory;
    protected ExtensionPointRegistry extensionsRegistry;
    protected EndpointRegistry endpointRegistry;
    protected EndpointReference endpointReference;
    protected String serviceName;
    protected RMIHost rmiHost;
    private String domainURI;
    
    public SCAClientProxyHandler(NodeFactoryImpl nodeFactory, String domainURI, String serviceName) {
        this.nodeFactory = nodeFactory;
        this.domainURI = domainURI;
        this.serviceName = serviceName;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Node node = null;
        try {

            nodeFactory = (NodeFactoryImpl)NodeFactory.newInstance();
            node = nodeFactory.createNode(URI.create(domainURI)).start();
            this.extensionsRegistry = nodeFactory.getExtensionPoints();
            RMIHostExtensionPoint rmiHosts = extensionsRegistry.getExtensionPoint(RMIHostExtensionPoint.class);
            this.rmiHost = new ExtensibleRMIHost(rmiHosts);

            FactoryExtensionPoint factories = extensionsRegistry.getExtensionPoint(FactoryExtensionPoint.class);
            AssemblyFactory assemblyFactory = factories.getFactory(AssemblyFactory.class);

            this.endpointReference = assemblyFactory.createEndpointReference();
            endpointReference.setReference(assemblyFactory.createComponentReference());
            Endpoint targetEndpoint = assemblyFactory.createEndpoint();
            targetEndpoint.setURI(serviceName);
            endpointReference.setTargetEndpoint(targetEndpoint);
            UtilityExtensionPoint utilities = extensionsRegistry.getExtensionPoint(UtilityExtensionPoint.class);
            DomainRegistryFactory domainRegistryFactory = utilities.getUtility(DomainRegistryFactory.class);
            this.endpointRegistry = domainRegistryFactory.getEndpointRegistry(null, domainURI);

            List<Endpoint> endpoints = endpointRegistry.findEndpoint(endpointReference);
            if (endpoints.size() <1 ) {
                throw new NoSuchServiceException(serviceName);
            }

            String uri = endpoints.get(0).getBinding().getURI();
            RMIBindingInvoker invoker = new RMIBindingInvoker(rmiHost, uri, method);

            return invoker.invokeTarget(args);

        } finally {
            if (node != null) {
                node.stop();
            }
        }
    }
}
