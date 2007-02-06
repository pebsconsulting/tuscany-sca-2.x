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

package org.apache.tuscany.core.databinding.impl;

import java.util.HashMap;
import java.util.Map;

import org.osoa.sca.annotations.EagerInit;

import org.apache.tuscany.core.databinding.javabeans.JavaBeansDataBinding;
import org.apache.tuscany.spi.databinding.DataBinding;
import org.apache.tuscany.spi.databinding.DataBindingRegistry;
import org.apache.tuscany.spi.model.DataType;

/**
 * The default implementation of a data binding registry
 *
 * @version $Rev$ $Date$
 */
@EagerInit
public class DataBindingRegistryImpl implements DataBindingRegistry {
    private final Map<String, DataBinding> bindings = new HashMap<String, DataBinding>();

    public DataBinding getDataBinding(String id) {
        if (id == null) {
            return null;
        }
        return bindings.get(id.toLowerCase());
    }

    public void register(DataBinding dataBinding) {
        bindings.put(dataBinding.getName().toLowerCase(), dataBinding);
    }

    public DataBinding unregister(String id) {
        if (id == null) {
            return null;
        }
        return bindings.remove(id.toLowerCase());
    }

    public DataType introspectType(Class<?> javaType) {
        DataType dataType = null;
        for (DataBinding binding : bindings.values()) {
            //don't introspect for JavaBeansDatabinding as all javatypes will anyways match to its basetype 
            //which is java.lang.Object.  Default to this only if no databinding results
            if (!binding.getName().equals(JavaBeansDataBinding.NAME)) {
                dataType = binding.introspect(javaType);
            }
            
            if (dataType != null) {
                return dataType;
            }
        }
        return new DataType<Class>(JavaBeansDataBinding.NAME, Object.class, javaType);
    }

    public DataType introspectType(Object value) {
        DataType dataType = null;
        for (DataBinding binding : bindings.values()) {
            //don't introspect for JavaBeansDatabinding as all javatypes will anyways match to its basetype 
            //which is java.lang.Object.  Default to this only if no databinding results
            if (!binding.getName().equals(JavaBeansDataBinding.NAME)) {
                dataType = binding.introspect(value);
            }
            if (dataType != null) {
                return dataType;
            }
        }
        return new DataType<Class>(JavaBeansDataBinding.NAME, Object.class, value.getClass());
    }

}
