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
package org.apache.tuscany.binding.axis2;

import org.apache.tuscany.binding.ws.WebServiceBinding;
import org.apache.tuscany.binding.ws.WebServiceBindingFactory;
import org.apache.tuscany.http.ServletHost;

/**
 * A factory for the WSDL model.
 */
public class Axis2BindingFactory implements WebServiceBindingFactory {

    private ServletHost servletHost;

    public Axis2BindingFactory(ServletHost servletHost) {
        this.servletHost = servletHost;
    }

    public WebServiceBinding createWebServiceBinding() {
        return new Axis2BindingProviderFactory(servletHost);
    }

}
