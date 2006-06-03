/**
 *
 * Copyright 2006 The Apache Software Foundation or its licensors as applicable
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
package org.apache.tuscany.core.bootstrap;

import junit.framework.TestCase;
import org.apache.tuscany.core.deployer.DeployerImpl;
import org.apache.tuscany.core.monitor.NullMonitorFactory;

/**
 * @version $Rev$ $Date$
 */
public class BootstrapperTestCase extends TestCase {
    private DefaultBootstrapper bootstrapper;

    public void testDeployerBootstrap() {
        DeployerImpl deployer = (DeployerImpl) bootstrapper.createDeployer();
    }

    protected void setUp() throws Exception {
        super.setUp();
        bootstrapper = new DefaultBootstrapper(new NullMonitorFactory());
    }
}
