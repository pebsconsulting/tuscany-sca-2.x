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

package org.apache.tuscany.sca.binding.rmi;

import javax.xml.namespace.QName;

import org.apache.tuscany.sca.assembly.xml.Constants;
import org.apache.tuscany.sca.rmi.RMIHost;
import org.apache.tuscany.sca.runtime.RuntimeComponent;
import org.apache.tuscany.sca.runtime.RuntimeComponentReference;
import org.apache.tuscany.sca.runtime.RuntimeComponentService;
import org.apache.tuscany.sca.spi.BindingActivator;
import org.apache.tuscany.sca.spi.InvokerFactory;
import org.apache.tuscany.sca.spi.ComponentLifecycle;

public class RMIBindingActivator implements BindingActivator<RMIBinding> {

    private RMIHost rmiHost;

    private QName BINDING_RMI_QNAME = new QName(Constants.SCA10_NS, "binding.rmi");

    public RMIBindingActivator(RMIHost rmiHost) {
        this.rmiHost = rmiHost;
    }

    public QName getSCDLQName() {
        return BINDING_RMI_QNAME;
    }

    public Class<RMIBinding> getBindingClass() {
        return RMIBinding.class;
    }

    public InvokerFactory createInvokerFactory(RuntimeComponent rc, RuntimeComponentReference rcr, RMIBinding binding) {
        return new RMIReferenceInvokerFactory(rc, rcr, binding, rmiHost);
    }

    public ComponentLifecycle createService(RuntimeComponent rc, RuntimeComponentService rcs, RMIBinding binding) {
        return new RMIService(rc, rcs, binding, rmiHost);
    }

}
