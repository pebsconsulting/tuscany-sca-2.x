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
package org.apache.tuscany.sca.implementation.bpel.impl;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.tuscany.sca.assembly.xml.Constants;
import org.apache.tuscany.sca.contribution.ModelFactoryExtensionPoint;
import org.apache.tuscany.sca.contribution.processor.StAXArtifactProcessor;
import org.apache.tuscany.sca.contribution.resolver.ModelResolver;
import org.apache.tuscany.sca.contribution.service.ContributionReadException;
import org.apache.tuscany.sca.contribution.service.ContributionResolveException;
import org.apache.tuscany.sca.contribution.service.ContributionWriteException;
import org.apache.tuscany.sca.implementation.bpel.BPELFactory;
import org.apache.tuscany.sca.implementation.bpel.BPELImplementation;
import org.apache.tuscany.sca.implementation.bpel.DefaultBPELFactory;

/**
 * Implements a STAX artifact processor for BPEL implementations.
 * 
 * The artifact processor is responsible for processing <implementation.bpel>
 * elements in SCA assembly XML composite files and populating the BPEL
 * implementation model, resolving its references to other artifacts in the SCA
 * contribution, and optionally write the model back to SCA assembly XML.
 * 
 *  @version $Rev$ $Date$
 */
public class BPELImplementationProcessor implements StAXArtifactProcessor<BPELImplementation> {
    private static final QName IMPLEMENTATION_BPEL = new QName(Constants.SCA10_NS, "implementation.bpel");
    
    private BPELFactory bpelFactory;
    
    public BPELImplementationProcessor(ModelFactoryExtensionPoint modelFactories) {
        this.bpelFactory = new DefaultBPELFactory(modelFactories);
    }

    public QName getArtifactType() {
        // Returns the qname of the XML element processed by this processor
        return IMPLEMENTATION_BPEL;
    }

    public Class<BPELImplementation> getModelType() {
        // Returns the type of model processed by this processor
        return BPELImplementation.class;
    }

    public BPELImplementation read(XMLStreamReader reader) throws ContributionReadException, XMLStreamException {
        assert IMPLEMENTATION_BPEL.equals(reader.getName());
        
        // Read an <implementation.bpel> element

        // Read the process attribute. 
        QName process = getAttributeValueNS(reader, "process");


        // Create an initialize the BPEL implementation model
        BPELImplementation implementation = bpelFactory.createBPELImplementation();
        implementation.setProcess(process);
        //FIXME:lresende
        //implementation.setCompiledProcess(compiledProcess.toByteArray());
        //implementation.setUnresolved(false);
        
        // Skip to end element
        while (reader.hasNext()) {
            if (reader.next() == END_ELEMENT && IMPLEMENTATION_BPEL.equals(reader.getName())) {
                break;
            }
        }
        
        return implementation;
    }

    public void resolve(BPELImplementation impl, ModelResolver resolver) throws ContributionResolveException {
        System.out.println("IN RESOLVE");
        if( impl != null && impl.isUnresolved()) {
            
            impl.setUnresolved(false);
        }
        
    }

    public void write(BPELImplementation model, XMLStreamWriter outputSource) throws ContributionWriteException {
        //FIXME Implement
    }

    private QName getAttributeValueNS(XMLStreamReader reader, String attribute) {
        String fullValue = reader.getAttributeValue(null, "process");
        if (fullValue.indexOf(":") < 0)
            throw new ODEProcessException("Attribute " + attribute + " with value " + fullValue +
                    " in your composite should be prefixed (process=\"prefix:name\").");
        String prefix = fullValue.substring(0, fullValue.indexOf(":"));
        String name = fullValue.substring(fullValue.indexOf(":") + 1);
        String nsUri = reader.getNamespaceContext().getNamespaceURI(prefix);
        if (nsUri == null)
            throw new ODEProcessException("Attribute " + attribute + " with value " + fullValue +
                    " in your composite has un unrecognized namespace prefix.");
        return new QName(nsUri, name, prefix);
    }
}
