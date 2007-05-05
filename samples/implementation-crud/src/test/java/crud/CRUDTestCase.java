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

package crud;

import junit.framework.TestCase;

import org.apache.tuscany.host.embedded.SCADomain;

/**
 * @version $Rev$ $Date$
 */
public class CRUDTestCase extends TestCase {

    private SCADomain domain;
    private CRUD crudService;
    
    /**
     * @throws java.lang.Exception
     */
    protected void setUp() throws Exception {
        domain = SCADomain.newInstance("crud.composite");
        crudService = domain.getService(CRUD.class, "CRUDServiceComponent");

    }

    /**
     * @throws java.lang.Exception
     */
    protected void tearDown() throws Exception {
        domain.close();
    }

    
    public void testCRUD() throws Exception {
        String id = crudService.create("ABC");
        Object result = crudService.retrieve(id);
        assertEquals("ABC", result);
        crudService.update(id, "EFG");
        result = crudService.retrieve(id);
        assertEquals("EFG", result);
        crudService.delete(id);
        result = crudService.retrieve(id);
        assertNull(result);
    }


}
