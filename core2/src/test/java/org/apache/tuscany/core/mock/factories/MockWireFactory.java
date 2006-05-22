package org.apache.tuscany.core.mock.factories;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

import org.apache.tuscany.core.mock.component.SimpleSource;
import org.apache.tuscany.core.mock.component.SimpleTarget;
import org.apache.tuscany.core.mock.component.SimpleSourceImpl;
import org.apache.tuscany.core.mock.component.SimpleTargetImpl;
import org.apache.tuscany.core.mock.component.AsyncTarget;
import org.apache.tuscany.core.mock.component.AsyncSource;
import org.apache.tuscany.core.mock.component.AsyncSourceImpl;
import org.apache.tuscany.core.mock.component.AsyncTargetImpl;
import org.apache.tuscany.core.mock.context.MockAtomicContext;
import org.apache.tuscany.core.injection.PojoObjectFactory;
import org.apache.tuscany.core.wire.jdk.JDKSourceWire;
import org.apache.tuscany.core.wire.jdk.JDKTargetWire;
import org.apache.tuscany.core.wire.SourceInvocationChainImpl;
import org.apache.tuscany.core.wire.TargetInvocationChainImpl;
import org.apache.tuscany.core.wire.InvokerInterceptor;
import org.apache.tuscany.spi.context.ScopeContext;
import org.apache.tuscany.spi.wire.Interceptor;
import org.apache.tuscany.spi.wire.MessageHandler;
import org.apache.tuscany.spi.wire.SourceWire;
import org.apache.tuscany.spi.wire.SourceInvocationChain;
import org.apache.tuscany.spi.wire.TargetWire;
import org.apache.tuscany.spi.QualifiedName;

/**
 * A factory for constructing a wire between a {@link org.apache.tuscany.core.mock.component.SimpleSource} and
 * {@link org.apache.tuscany.core.mock.component.SimpleTarget}
 *
 * @version $$Rev$$ $$Date$$
 */
public class MockWireFactory {

    private MockWireFactory() {
    }

    public static MockAtomicContext<SimpleSource> setupSource(ScopeContext scopeContext, List<Interceptor> interceptors,
                                                               List<MessageHandler> requestHandlers,
                                                               List<MessageHandler> responseHandlers) throws NoSuchMethodException {
        Method echo = SimpleTarget.class.getMethod("echo", String.class);

        List<Class<?>> sourceInterfaces = new ArrayList<Class<?>>();
        sourceInterfaces.add(SimpleSource.class);
        Constructor<SimpleSourceImpl> sourceCtr = SimpleSourceImpl.class.getConstructor();
        Map<String, Member> members = new HashMap<String, Member>();
        members.put("target", SimpleSourceImpl.class.getMethod("setTarget", SimpleTarget.class));
        MockAtomicContext<SimpleSource> source = new MockAtomicContext<SimpleSource>("source", sourceInterfaces, new PojoObjectFactory<SimpleSourceImpl>(sourceCtr),
                scopeContext.getScope(), members);
        SourceWire<SimpleTarget> sourceWire = new JDKSourceWire<SimpleTarget>();
        sourceWire.setBusinessInterface(SimpleTarget.class);
        sourceWire.setReferenceName("target");
        sourceWire.setTargetName(new QualifiedName("target/Target"));
        SourceInvocationChain chain = new SourceInvocationChainImpl(echo);
        if (interceptors != null) {
            for (Interceptor interceptor : interceptors) {
                chain.addInterceptor(interceptor);
            }
        }
        if (requestHandlers != null) {
            for (MessageHandler handler : requestHandlers) {
                chain.addRequestHandler(handler);
            }
        }
        if (responseHandlers != null) {
            for (MessageHandler handler : responseHandlers) {
                chain.addResponseHandler(handler);
            }
        }
        sourceWire.addInvocationChain(echo, chain);
        source.addSourceWire(sourceWire);
        source.setScopeContext(scopeContext);
        return source;
    }

    public static MockAtomicContext<SimpleTarget> setupTarget(ScopeContext scopeContext, List<Interceptor> interceptors,
                                                               List<MessageHandler> requestHandlers,
                                                               List<MessageHandler> responseHandlers) throws NoSuchMethodException {
        Method echo = SimpleTarget.class.getMethod("echo", String.class);

        List<Class<?>> targetInterfaces = new ArrayList<Class<?>>();
        targetInterfaces.add(SimpleTarget.class);
        Constructor<SimpleTargetImpl> targetCtr = SimpleTargetImpl.class.getConstructor();
        MockAtomicContext<SimpleTarget> target = new MockAtomicContext<SimpleTarget>("target", targetInterfaces, new PojoObjectFactory<SimpleTargetImpl>(targetCtr),
                scopeContext.getScope(), null);
        TargetWire<SimpleTarget> targetWire = new JDKTargetWire<SimpleTarget>();
        targetWire.setBusinessInterface(SimpleTarget.class);
        targetWire.setServiceName("Target");
        TargetInvocationChainImpl chain = new TargetInvocationChainImpl(echo);
        if (interceptors != null) {
            for (Interceptor interceptor : interceptors) {
                chain.addInterceptor(interceptor);
            }
        }
        if (requestHandlers != null) {
            for (MessageHandler handler : requestHandlers) {
                chain.addRequestHandler(handler);
            }
        }
        if (responseHandlers != null) {
            for (MessageHandler handler : responseHandlers) {
                chain.addResponseHandler(handler);
            }
        }
        chain.addInterceptor(new InvokerInterceptor()); // add tail interceptor
        targetWire.addInvocationChain(echo, chain);
        target.addTargetWire(targetWire);
        target.setScopeContext(scopeContext);
        return target;
    }


    public static MockAtomicContext<AsyncSource> setupAsyncSource(ScopeContext scopeContext, List<Interceptor> interceptors,
                                                               List<MessageHandler> requestHandlers,
                                                               List<MessageHandler> responseHandlers) throws NoSuchMethodException {
        Method setter = AsyncTarget.class.getMethod("setString", String.class);

        List<Class<?>> sourceInterfaces = new ArrayList<Class<?>>();
        sourceInterfaces.add(AsyncSource.class);
        Constructor<AsyncSourceImpl> sourceCtr = AsyncSourceImpl.class.getConstructor();
        Map<String, Member> members = new HashMap<String, Member>();
        members.put("target", AsyncSourceImpl.class.getMethod("setTarget", AsyncTarget.class));
        MockAtomicContext<AsyncSource> source = new MockAtomicContext<AsyncSource>("source", sourceInterfaces, new PojoObjectFactory<AsyncSourceImpl>(sourceCtr),
                scopeContext.getScope(), members);
        SourceWire<AsyncTarget> sourceWire = new JDKSourceWire<AsyncTarget>();
        sourceWire.setBusinessInterface(AsyncTarget.class);
        sourceWire.setReferenceName("target");
        sourceWire.setTargetName(new QualifiedName("target/AsyncTarget"));
        SourceInvocationChain chain = new SourceInvocationChainImpl(setter);
        if (interceptors != null) {
            for (Interceptor interceptor : interceptors) {
                chain.addInterceptor(interceptor);
            }
        }
        if (requestHandlers != null) {
            for (MessageHandler handler : requestHandlers) {
                chain.addRequestHandler(handler);
            }
        }
        if (responseHandlers != null) {
            for (MessageHandler handler : responseHandlers) {
                chain.addResponseHandler(handler);
            }
        }
        sourceWire.addInvocationChain(setter, chain);
        source.addSourceWire(sourceWire);
        source.setScopeContext(scopeContext);
        return source;
    }

    public static MockAtomicContext<AsyncTarget> setupAsyncTarget(ScopeContext scopeContext, List<Interceptor> interceptors,
                                                               List<MessageHandler> requestHandlers,
                                                               List<MessageHandler> responseHandlers) throws NoSuchMethodException {
        Method echo = AsyncTarget.class.getMethod("setString", String.class);

        List<Class<?>> targetInterfaces = new ArrayList<Class<?>>();
        targetInterfaces.add(SimpleTarget.class);
        Constructor<AsyncTargetImpl> targetCtr = AsyncTargetImpl.class.getConstructor();
        MockAtomicContext<AsyncTarget> target = new MockAtomicContext<AsyncTarget>("target", targetInterfaces,
                new PojoObjectFactory<AsyncTargetImpl>(targetCtr),
                scopeContext.getScope(), null);
        TargetWire<AsyncTarget> targetWire = new JDKTargetWire<AsyncTarget>();
        targetWire.setBusinessInterface(AsyncTarget.class);
        targetWire.setServiceName("AsyncTarget");
        TargetInvocationChainImpl chain = new TargetInvocationChainImpl(echo);
        if (interceptors != null) {
            for (Interceptor interceptor : interceptors) {
                chain.addInterceptor(interceptor);
            }
        }
        if (requestHandlers != null) {
            for (MessageHandler handler : requestHandlers) {
                chain.addRequestHandler(handler);
            }
        }
        if (responseHandlers != null) {
            for (MessageHandler handler : responseHandlers) {
                chain.addResponseHandler(handler);
            }
        }
        chain.addInterceptor(new InvokerInterceptor()); // add tail interceptor
        targetWire.addInvocationChain(echo, chain);
        target.addTargetWire(targetWire);
        target.setScopeContext(scopeContext);
        return target;
    }



}
