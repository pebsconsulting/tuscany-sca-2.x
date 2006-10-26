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
package org.apache.tuscany.core.implementation.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.tuscany.spi.component.WorkContext;
import org.apache.tuscany.spi.extension.ExecutionMonitor;
import org.apache.tuscany.spi.wire.InboundWire;
import org.apache.tuscany.spi.wire.Message;
import org.apache.tuscany.spi.wire.MessageImpl;

import junit.framework.Assert;
import junit.framework.TestCase;
import static org.apache.tuscany.core.implementation.java.mock.MockFactory.createJavaComponent;
import org.apache.tuscany.core.implementation.java.mock.components.AsyncTarget;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.easymock.classextension.EasyMock;

/**
 * Verifies dispatching invocations to a Java implementation instance
 */
public class JavaTargetInvokerNonBlockingInvocationTestCase extends TestCase {

    private Method echoMethod;
    private Method arrayMethod;
    private Method nullParamMethod;
    private Method primitiveMethod;
    private Method checkedMethod;
    private Method runtimeMethod;
    private InboundWire wire;
    private WorkContext context;
    private ExecutionMonitor monitor;

    public JavaTargetInvokerNonBlockingInvocationTestCase(String arg0) {
        super(arg0);
    }

    public void testInvoke() throws Exception {
        AsyncTarget target = createMock(AsyncTarget.class);
        target.invoke();
        expectLastCall().once();
        replay(target);
        JavaAtomicComponent component = createJavaComponent(target);
        ExecutionMonitor monitor = createMock(ExecutionMonitor.class);
        replay(monitor);

        Message msg = new MessageImpl();
        Object id = new Object();
        msg.setMessageId(id);

        WorkContext context = createMock(WorkContext.class);
        context.setCurrentMessageId(null);
        context.setCurrentCorrelationId(id);
        replay(context);
        Method method = AsyncTarget.class.getMethod("invoke");
        method.setAccessible(true);
        InboundWire wire = createMock(InboundWire.class);
        JavaTargetInvoker invoker = new JavaTargetInvoker(method, component, wire, context, monitor);
        invoker.invoke(msg);
        verify(target);
    }

    public void setUp() throws Exception {
        echoMethod = TestBean.class.getDeclaredMethod("echo", String.class);
        arrayMethod = TestBean.class.getDeclaredMethod("arrayEcho", String[].class);
        nullParamMethod = TestBean.class.getDeclaredMethod("nullParam", (Class[]) null);
        primitiveMethod = TestBean.class.getDeclaredMethod("primitiveEcho", Integer.TYPE);
        checkedMethod = TestBean.class.getDeclaredMethod("checkedException", (Class[]) null);
        runtimeMethod = TestBean.class.getDeclaredMethod("runtimeException", (Class[]) null);
        wire = EasyMock.createNiceMock(InboundWire.class);
        context = EasyMock.createNiceMock(WorkContext.class);
        monitor = EasyMock.createNiceMock(ExecutionMonitor.class);
        assertNotNull(echoMethod);
        assertNotNull(checkedMethod);
        assertNotNull(runtimeMethod);
    }

    public void testObjectInvoke() throws Throwable {
        TestBean bean = new TestBean();
        JavaAtomicComponent component = EasyMock.createMock(JavaAtomicComponent.class);
        EasyMock.expect(component.getTargetInstance()).andReturn(bean);
        EasyMock.replay(component);
        JavaTargetInvoker invoker = new JavaTargetInvoker(echoMethod, component, wire, context, monitor);
        Object ret = invoker.invokeTarget("foo");
        Assert.assertEquals("foo", ret);
    }

    public void testArrayInvoke() throws Throwable {
        TestBean bean = new TestBean();
        JavaAtomicComponent component = EasyMock.createMock(JavaAtomicComponent.class);
        EasyMock.expect(component.getTargetInstance()).andReturn(bean);
        EasyMock.replay(component);
        JavaTargetInvoker invoker = new JavaTargetInvoker(arrayMethod, component, wire, context, monitor);

        String[] args = new String[]{"foo", "bar"};
        Object ret = invoker.invokeTarget(new Object[]{args});
        String[] retA = (String[]) ret;
        Assert.assertNotNull(retA);
        Assert.assertEquals(2, retA.length);
        Assert.assertEquals("foo", retA[0]);
        Assert.assertEquals("bar", retA[1]);
    }

    public void testNullInvoke() throws Throwable {
        TestBean bean = new TestBean();
        JavaAtomicComponent component = EasyMock.createMock(JavaAtomicComponent.class);
        EasyMock.expect(component.getTargetInstance()).andReturn(bean);
        EasyMock.replay(component);
        JavaTargetInvoker invoker = new JavaTargetInvoker(nullParamMethod, component, wire, context, monitor);
        Object ret = invoker.invokeTarget(null);
        String retS = (String) ret;
        Assert.assertEquals("foo", retS);
    }

    public void testPrimitiveInvoke() throws Throwable {
        TestBean bean = new TestBean();
        JavaAtomicComponent component = EasyMock.createMock(JavaAtomicComponent.class);
        EasyMock.expect(component.getTargetInstance()).andReturn(bean);
        EasyMock.replay(component);
        JavaTargetInvoker invoker = new JavaTargetInvoker(primitiveMethod, component, wire, context, monitor);
        Object ret = invoker.invokeTarget(new Integer[]{1});
        Integer retI = (Integer) ret;
        Assert.assertEquals(1, retI.intValue());
    }

    public void testInvokeCheckedException() throws Throwable {
        TestBean bean = new TestBean();
        JavaAtomicComponent component = EasyMock.createMock(JavaAtomicComponent.class);
        EasyMock.expect(component.getTargetInstance()).andReturn(bean);
        EasyMock.replay(component);
        JavaTargetInvoker invoker = new JavaTargetInvoker(checkedMethod, component, wire, context, monitor);
        try {
            invoker.invokeTarget(null);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null && TestException.class.equals(e.getCause().getClass())) {
                return;
            }
        } catch (Throwable e) {
            //ok
        }
        fail(TestException.class.getName() + " should have been thrown");
    }

    public void testInvokeRuntimeException() throws Throwable {
        TestBean bean = new TestBean();
        JavaAtomicComponent component = EasyMock.createMock(JavaAtomicComponent.class);
        EasyMock.expect(component.getTargetInstance()).andReturn(bean);
        EasyMock.replay(component);
        JavaTargetInvoker invoker = new JavaTargetInvoker(runtimeMethod, component, wire, context, monitor);
        try {
            invoker.invokeTarget(null);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null && e.getCause() instanceof TestRuntimeException) {
                return;
            }
        }
        fail(TestException.class.getName() + " should have been thrown");
    }

    private class TestBean {

        public String echo(String msg) throws Exception {
            Assert.assertEquals("foo", msg);
            return msg;
        }

        public String[] arrayEcho(String[] msg) throws Exception {
            Assert.assertNotNull(msg);
            Assert.assertEquals(2, msg.length);
            Assert.assertEquals("foo", msg[0]);
            Assert.assertEquals("bar", msg[1]);
            return msg;
        }

        public String nullParam() throws Exception {
            return "foo";
        }

        public int primitiveEcho(int i) throws Exception {
            return i;
        }

        public void checkedException() throws TestException {
            throw new TestException();
        }

        public void runtimeException() throws TestRuntimeException {
            throw new TestRuntimeException();
        }
    }

    public class TestException extends Exception {
    }

    public class TestRuntimeException extends RuntimeException {
    }
}
