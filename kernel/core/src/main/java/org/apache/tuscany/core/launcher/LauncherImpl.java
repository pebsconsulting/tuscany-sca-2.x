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
package org.apache.tuscany.core.launcher;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;

import org.osoa.sca.CompositeContext;

import org.apache.tuscany.spi.bootstrap.ComponentNames;
import org.apache.tuscany.spi.bootstrap.RuntimeComponent;
import org.apache.tuscany.spi.component.CompositeComponent;
import org.apache.tuscany.spi.component.SCAObject;
import org.apache.tuscany.spi.component.AtomicComponent;
import org.apache.tuscany.spi.deployer.Deployer;
import org.apache.tuscany.spi.loader.LoaderException;
import org.apache.tuscany.spi.model.ComponentDefinition;
import org.apache.tuscany.spi.model.CompositeImplementation;
import org.apache.tuscany.spi.services.management.TuscanyManagementService;

import org.apache.tuscany.api.TuscanyException;
import org.apache.tuscany.core.bootstrap.Bootstrapper;
import org.apache.tuscany.core.bootstrap.DefaultBootstrapper;
import org.apache.tuscany.core.implementation.system.model.SystemCompositeImplementation;
import org.apache.tuscany.host.Launcher;
import org.apache.tuscany.host.MonitorFactory;
import org.apache.tuscany.host.RuntimeInfo;
import org.apache.tuscany.host.runtime.InitializationException;
import org.apache.tuscany.host.monitor.FormatterRegistry;

/**
 * Basic launcher implementation.
 *
 * @version $Rev$ $Date$
 */
public class LauncherImpl implements Launcher {
    /**
     * A conventional META-INF based location for the system SCDL.
     *
     * @see #bootRuntime(URL,MonitorFactory)
     */
    public static final String METAINF_SYSTEM_SCDL_PATH = "META-INF/tuscany/system.scdl";

    /**
     * A conventional META-INF based location for the application SCDL.
     */
    public static final String METAINF_APPLICATION_SCDL_PATH = "META-INF/sca/default.scdl";

    private ClassLoader applicationLoader;

    private RuntimeComponent runtime;

    private Deployer deployer;

    private CompositeComponent composite;

    public void bootRuntime(URL systemScdl, ClassLoader systemClassLoader, MonitorFactory monitor)
        throws TuscanyException {
        if (systemScdl == null) {
            throw new LoaderException("Null system SCDL URL");
        }

        XMLInputFactory xmlFactory = XMLInputFactory.newInstance("javax.xml.stream.XMLInputFactory", systemClassLoader);
        TuscanyManagementService managementService = null;
        Bootstrapper bootstrapper = new DefaultBootstrapper(monitor, xmlFactory, managementService);
        Deployer bootDeployer = bootstrapper.createDeployer();

        // create and start the core runtime
        runtime = bootstrapper.createRuntime();
        runtime.start();

        // initialize the runtime info
        CompositeComponent parent = runtime.getSystemComponent();
        RuntimeInfo runtimeInfo = new LauncherRuntimeInfo(getInstallDirectory(), getApplicationRootDirectory(), true);
        parent.registerJavaObject("RuntimeInfo", RuntimeInfo.class, runtimeInfo);

        // register the monitor factory
        if (monitor instanceof FormatterRegistry) {
            List<Class<?>> interfazes = new ArrayList<Class<?>>(2);
            interfazes.add(MonitorFactory.class);
            interfazes.add(FormatterRegistry.class);
            parent.registerJavaObject("MonitorFactory", interfazes, monitor);
        } else {
            parent.registerJavaObject("MonitorFactory", MonitorFactory.class, monitor);
        }
        parent.start();
        // create a ComponentDefinition to represent the component we are going to deploy
        SystemCompositeImplementation compositeImplementation = new SystemCompositeImplementation();
        compositeImplementation.setScdlLocation(systemScdl);
        compositeImplementation.setClassLoader(systemClassLoader);
        ComponentDefinition<SystemCompositeImplementation> definition =
            new ComponentDefinition<SystemCompositeImplementation>(
                ComponentNames.TUSCANY_SYSTEM, compositeImplementation);
        try {
            // deploy the component into the runtime under the system parent
            composite = (CompositeComponent) bootDeployer.deploy(parent, definition);
        } catch (TuscanyException e) {
            e.addContextName(definition.getName());
            throw e;
        }
        // start the system
        composite.start();

        SCAObject child = composite.getSystemChild(ComponentNames.TUSCANY_DEPLOYER);
        if (!(child instanceof AtomicComponent)) {
            throw new InitializationException("Deployer must be an atomic component");
        }
        deployer = (Deployer) ((AtomicComponent) child).getTargetInstance();
        runtime.getRootComponent().start();
    }

    /**
     * Shuts down the active runtime being managed by this instance.
     */
    public void shutdownRuntime() {
        if (composite != null) {
            composite.stop();
            composite = null;
        }

        if (runtime != null) {
            runtime.stop();
            runtime = null;
        }
    }

    public CompositeContext bootApplication(URL applicationScdl, ClassLoader applicationLoader) {
        // FIXME implement
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the classloader for application classes.
     *
     * @return the classloader for application classes
     */
    public ClassLoader getApplicationLoader() {
        return applicationLoader;
    }

    /**
     * Set the classloader to be used for application classes. You should almost always supply your own application
     * classloader, based on the hosting environment that the runtime is embedded in.
     *
     * @param applicationLoader the classloader to be used for application classes
     */
    public void setApplicationLoader(ClassLoader applicationLoader) {
        this.applicationLoader = applicationLoader;
    }

    /**
     * Boots the runtime defined by the specified SCDL.
     *
     * @param systemScdl a resource path to the SCDL defining the system.
     * @return a CompositeComponent for the newly booted runtime system
     * @throws LoaderException
     */
    @Deprecated
    public CompositeComponent bootRuntime(URL systemScdl, MonitorFactory monitor) throws TuscanyException {
        ClassLoader systemClassLoader = getClass().getClassLoader();
        bootRuntime(systemScdl, systemClassLoader, monitor);
        return composite;
    }

    /**
     * Boots the application defined by the specified SCDL.
     *
     * @param name    the name of the application component
     * @param appScdl URL to the SCDL defining the application
     * @return a CompositeComponent for the newly booted application
     * @throws LoaderException
     */
    @Deprecated
    public CompositeComponent bootApplication(String name, URL appScdl) throws TuscanyException {
        ClassLoader applicationLoader = getApplicationLoader();

        if (appScdl == null) {
            throw new LoaderException("No application scdl found");
        }

        // create a ComponentDefinition to represent the component we are going to deploy
        CompositeImplementation impl = new CompositeImplementation();
        impl.setScdlLocation(appScdl);
        impl.setClassLoader(applicationLoader);
        ComponentDefinition<CompositeImplementation> definition =
            new ComponentDefinition<CompositeImplementation>(name, impl);

        // deploy the component into the runtime under the system parent
        CompositeComponent parent = runtime.getRootComponent();
        // FIXME andyp -- this seems bogus when running inside an appserver
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        try {

            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return (CompositeComponent) deployer.deploy(parent, definition);
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    public File getInstallDirectory() {
        String property = System.getProperty("tuscany.installDir");
        if (property != null) {
            return new File(property);
        }

        // TODO: TUSCANY-648, should this throw an exception if it not running from a jar?

        URL url = getClass().getResource("LauncherImpl.class");
        String jarLocation = url.toString();
        if ("jar".equals(url.getProtocol())) {
            jarLocation = jarLocation.substring(4, jarLocation.lastIndexOf("!/"));
        }
        if (jarLocation.startsWith("file:")) {
            jarLocation = jarLocation.substring(5);
        }

        File jarFile = new File(jarLocation);
        return jarFile.getParentFile().getParentFile();
    }

    public File getApplicationRootDirectory() {
        String property = System.getProperty("tuscany.applicationRootDir");
        if (property != null) {
            return new File(property);
        }

        return new File(System.getProperty("user.dir"));
    }
}
