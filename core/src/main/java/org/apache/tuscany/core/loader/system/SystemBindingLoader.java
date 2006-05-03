/**
 *
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tuscany.core.loader.system;

import org.apache.tuscany.core.config.ConfigurationLoadException;
import org.apache.tuscany.core.loader.StAXUtil;
import org.apache.tuscany.core.loader.LoaderContext;
import org.apache.tuscany.spi.loader.LoaderSupport;
import org.apache.tuscany.core.system.assembly.SystemBinding;
import org.osoa.sca.annotations.Scope;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @version $Rev$ $Date$
 */
@Scope("MODULE")
public class SystemBindingLoader extends LoaderSupport {
    public static final QName SYSTEM_BINDING = new QName("http://org.apache.tuscany/xmlns/system/0.9", "binding.system");

    protected QName getXMLType() {
        return SYSTEM_BINDING;
    }

    public SystemBinding load(XMLStreamReader reader, LoaderContext loaderContext) throws XMLStreamException, ConfigurationLoadException {
        assert SystemBindingLoader.SYSTEM_BINDING.equals(reader.getName());
        SystemBinding binding = factory.createSystemBinding();
        StAXUtil.skipToEndElement(reader);
        return binding;
    }
}
