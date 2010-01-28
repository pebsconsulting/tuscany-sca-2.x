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

package test.scaclient;

import itest.HelloworldService;

import java.net.URI;

import junit.framework.TestCase;

import org.apache.tuscany.sca.node.Node;
import org.apache.tuscany.sca.node.NodeFactory;
import org.junit.Test;
import org.oasisopen.sca.client.SCAClientFactory;

/**
 * Test SCADomain.newInstance and invocation of a service.
 *
 * @version $Rev: 904064 $ $Date: 2010-01-28 12:31:36 +0000 (Thu, 28 Jan 2010) $
 */
public class SCAClientTestCase extends TestCase {

    private Node node;

    @Test
    public void testDefault() throws Exception {

        node = NodeFactory.getInstance().createNode((String)null, new String[] {"target/classes"});
        node.start();

        HelloworldService service = SCAClientFactory.newInstance(URI.create("default")).getService(HelloworldService.class, "HelloworldComponent");
        assertEquals("Hello petra", service.sayHello("petra"));
    }

    @Test
    public void testExplicit() throws Exception {
        node = NodeFactory.getInstance().createNode(URI.create("myFooDomain"), new String[] {"target/classes"});
        node.start();

        HelloworldService service = SCAClientFactory.newInstance(URI.create("myFooDomain")).getService(HelloworldService.class, "HelloworldComponent");
        String s=  service.sayHello("petra");
        assertEquals("Hello petra", service.sayHello("petra"));
    }

    @Test
    public void testExplicitRemote() throws Exception {
        node = NodeFactory.newInstance().createNode(URI.create("tuscany:myFooDomain?listen=127.0.0.1:14821"), new String[] {"target/classes"});
        node.start();

        HelloworldService service = SCAClientFactory.newInstance(URI.create("tuscany:myFooDomain?remotes=127.0.0.1:14821")).getService(HelloworldService.class, "HelloworldComponent");
        assertEquals("Hello petra", service.sayHello("petra"));
    }

    @Test
    public void testExplicitRemote2() throws Exception {
        node = NodeFactory.getInstance().createNode(URI.create("tuscany:myFooDomain?listen=127.0.0.1:14821"), new String[] {"target/classes"});
        node.start();

        HelloworldService service = SCAClientFactory.newInstance(URI.create("tuscany:myFooDomain?remotes=127.0.0.1:14821")).getService(HelloworldService.class, "HelloworldComponent");
        assertEquals("Hello petra", service.sayHello("petra"));
    }

    @Override
    protected void tearDown() throws Exception {
        node.stop();
    }

}
