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

package org.apache.tuscany.implementation.java.xml;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.apache.tuscany.assembly.AssemblyFactory;
import org.apache.tuscany.assembly.Composite;
import org.apache.tuscany.assembly.builder.impl.DefaultCompositeBuilder;
import org.apache.tuscany.assembly.impl.DefaultAssemblyFactory;
import org.apache.tuscany.assembly.xml.CompositeProcessor;
import org.apache.tuscany.contribution.processor.DefaultStAXArtifactProcessor;
import org.apache.tuscany.contribution.processor.DefaultStAXArtifactProcessorExtensionPoint;
import org.apache.tuscany.contribution.resolver.ArtifactResolver;
import org.apache.tuscany.contribution.resolver.DefaultArtifactResolver;
import org.apache.tuscany.implementation.java.JavaImplementationFactory;
import org.apache.tuscany.implementation.java.impl.DefaultJavaImplementationFactory;
import org.apache.tuscany.implementation.java.introspect.DefaultJavaClassIntrospector;
import org.apache.tuscany.implementation.java.introspect.DefaultJavaClassIntrospectorExtensionPoint;
import org.apache.tuscany.implementation.java.introspect.JavaClassIntrospector;
import org.apache.tuscany.interfacedef.InterfaceContractMapper;
import org.apache.tuscany.interfacedef.impl.DefaultInterfaceContractMapper;
import org.apache.tuscany.policy.PolicyFactory;
import org.apache.tuscany.policy.impl.DefaultPolicyFactory;

/**
 * Test reading Java implementations.
 * 
 * @version $Rev$ $Date$
 */
public class ReadTestCase extends TestCase {

    XMLInputFactory inputFactory;
    DefaultStAXArtifactProcessorExtensionPoint staxProcessors;
    DefaultStAXArtifactProcessor staxProcessor;
    private AssemblyFactory factory;
    private PolicyFactory policyFactory;
    private InterfaceContractMapper mapper;
    
    public void setUp() throws Exception {
        factory = new DefaultAssemblyFactory();
        policyFactory = new DefaultPolicyFactory();
        mapper = new DefaultInterfaceContractMapper();
        inputFactory = XMLInputFactory.newInstance();
        staxProcessors = new DefaultStAXArtifactProcessorExtensionPoint();
        staxProcessor = new DefaultStAXArtifactProcessor(staxProcessors, XMLInputFactory.newInstance(), XMLOutputFactory.newInstance());
        
        JavaImplementationFactory javaImplementationFactory = new DefaultJavaImplementationFactory(factory);
        JavaClassIntrospector classIntrospector = new DefaultJavaClassIntrospector(new DefaultJavaClassIntrospectorExtensionPoint());
        
        CompositeProcessor compositeProcessor = new CompositeProcessor(factory, policyFactory, mapper, staxProcessor);
        staxProcessors.addArtifactProcessor(compositeProcessor);

        JavaImplementationProcessor javaProcessor = new JavaImplementationProcessor(factory, policyFactory, javaImplementationFactory, classIntrospector);
        staxProcessors.addArtifactProcessor(javaProcessor);
    }

    public void tearDown() throws Exception {
        inputFactory = null;
        staxProcessors = null;
        policyFactory = null;
        factory = null;
        mapper = null;
    }

    public void testReadComposite() throws Exception {
        CompositeProcessor compositeProcessor = new CompositeProcessor(factory, policyFactory, mapper, staxProcessor);
        InputStream is = getClass().getResourceAsStream("Calculator.composite");
        XMLStreamReader reader = inputFactory.createXMLStreamReader(is);
        Composite composite = compositeProcessor.read(reader);
        assertNotNull(composite);

        DefaultCompositeBuilder compositeUtil = new DefaultCompositeBuilder(factory, mapper, null);
        compositeUtil.build(composite);

    }

    public void testReadAndResolveComposite() throws Exception {
        CompositeProcessor compositeProcessor = new CompositeProcessor(factory, policyFactory, mapper, staxProcessor);
        InputStream is = getClass().getResourceAsStream("Calculator.composite");
        XMLStreamReader reader = inputFactory.createXMLStreamReader(is);
        Composite composite = compositeProcessor.read(reader);
        assertNotNull(composite);
        
        ArtifactResolver resolver = new DefaultArtifactResolver(getClass().getClassLoader());
        staxProcessor.resolve(composite, resolver);

        DefaultCompositeBuilder compositeUtil = new DefaultCompositeBuilder(factory, mapper, null);
        compositeUtil.build(composite);

        //new PrintUtil(System.out).print(composite);
    }

}
