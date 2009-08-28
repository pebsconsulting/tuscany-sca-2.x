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

package org.apache.tuscany.sca.core.invocation.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tuscany.sca.assembly.Endpoint;
import org.apache.tuscany.sca.assembly.EndpointReference;
import org.apache.tuscany.sca.core.context.ServiceReferenceExt;
import org.apache.tuscany.sca.core.factory.InstanceWrapper;
import org.apache.tuscany.sca.core.invocation.ThreadMessageContext;
import org.apache.tuscany.sca.core.scope.TargetResolutionException;
import org.apache.tuscany.sca.interfacedef.DataType;
import org.apache.tuscany.sca.interfacedef.Operation;
import org.apache.tuscany.sca.interfacedef.java.JavaOperation;
import org.apache.tuscany.sca.invocation.InvocationChain;
import org.apache.tuscany.sca.invocation.Invoker;
import org.apache.tuscany.sca.invocation.Message;
import org.apache.tuscany.sca.invocation.MessageFactory;
import org.apache.tuscany.sca.runtime.RuntimeWire;
import org.oasisopen.sca.SCARuntimeException;
import org.oasisopen.sca.ServiceReference;
import org.oasisopen.sca.ServiceRuntimeException;

/**
 * @version $Rev$ $Date$
 */
public class JDKInvocationHandler implements InvocationHandler, Serializable {
    private static final long serialVersionUID = -3366410500152201371L;

    protected boolean conversational;
    protected MessageFactory messageFactory;
    protected EndpointReference source;
    protected Endpoint target;
    protected RuntimeWire wire;
    protected ServiceReferenceExt<?> callableReference;
    protected Class<?> businessInterface;

    protected boolean fixedWire = true;

    protected transient Map<Method, InvocationChain> chains = new HashMap<Method, InvocationChain>();

    public JDKInvocationHandler(MessageFactory messageFactory, Class<?> businessInterface, RuntimeWire wire) {
        this.messageFactory = messageFactory;
        this.wire = wire;
        this.businessInterface = businessInterface;
        init(this.wire);
    }

    public JDKInvocationHandler(MessageFactory messageFactory, ServiceReference<?> callableReference) {
        this.messageFactory = messageFactory;
        this.callableReference = (ServiceReferenceExt<?>)callableReference;
        if (callableReference != null) {
            this.businessInterface = callableReference.getBusinessInterface();
            this.wire = ((ServiceReferenceExt<?>)callableReference).getRuntimeWire();
            if (wire != null) {
                init(wire);
            }
        }
    }

    protected void init(RuntimeWire wire) {
        // TODO - EPR not required for OASIS
        /*
        if (wire != null) {
            try {
                // Clone the endpoint reference so that reference parameters can be changed
                source = (EndpointReference)wire.getSource().clone();
            } catch (CloneNotSupportedException e) {
                throw new ServiceRuntimeException(e);
            }
            initConversational(wire);
        }
        */
    }

    /* TODO - EPR - not required for OASIS
    protected void initConversational(RuntimeWire wire) {
        InterfaceContract contract = wire.getSource().getInterfaceContract();
        this.conversational = contract.getInterface().isConversational();
    }
    */

    protected Object getCallbackID() {
//        if (callableReference != null) {
//            return callableReference.getCallbackID();
//        } else {
            return null;
//        }
    }

    /* TODO - EPR - Not reqiured for OASIS
    protected Object getCallbackObject() {
        if (callableReference != null && callableReference instanceof ServiceReference) {
            return ((ServiceReference)callableReference).getService();
        } else {
            return null;
        }
    }
    */

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            return invokeObjectMethod(method, args);
        }
        if (wire == null) {
            throw new ServiceRuntimeException("No runtime wire is available");
        }
        
        if (wire.isOutOfDate()) {
            wire.rebuild();
            chains.clear();
        }
        
        InvocationChain chain = getInvocationChain(method, wire);
        
        if (chain == null) {
            throw new IllegalArgumentException("No matching operation is found: " + method);
        }

        // The EndpointReference is not now resolved until the invocation chain 
        // is first created so reset the source here 
        source = wire.getEndpointReference();
        
        // send the invocation down the wire
        Object result = invoke(chain, args, wire, source);

        return result;
    }

    /**
     * Handle the methods on the Object.class
     * @param method
     * @param args
     */
    protected Object invokeObjectMethod(Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("toString".equals(name)) {
            return "[Proxy - " + toString() + "]";
        } else if ("equals".equals(name)) {
            Object obj = args[0];
            if (obj == null) {
                return false;
            }
            if (!Proxy.isProxyClass(obj.getClass())) {
                return false;
            }
            return equals(Proxy.getInvocationHandler(obj));
        } else if ("hashCode".equals(name)) {
            return hashCode();
        } else {
            return method.invoke(this);
        }
    }

    /**
     * Determines if the given operation matches the given method
     * 
     * @return true if the operation matches, false if does not
     */
    // FIXME: Should it be in the InterfaceContractMapper?
    @SuppressWarnings("unchecked")
    private static boolean match(Operation operation, Method method) {
        if (operation instanceof JavaOperation) {
            JavaOperation javaOp = (JavaOperation)operation;
            Method m = javaOp.getJavaMethod();
            if (!method.getName().equals(m.getName())) {
                return false;
            }
            if (method.equals(m)) {
                return true;
            }
        } else {
            if (!method.getName().equals(operation.getName())) {
                return false;
            }
        }

        // For remotable interface, operation is not overloaded. 
        if (operation.getInterface().isRemotable()) {
            return true;
        }

        Class<?>[] params = method.getParameterTypes();

        DataType<List<DataType>> inputType = null;
        if (operation.isWrapperStyle()) {
            inputType = operation.getWrapper().getUnwrappedInputType();
        } else {
            inputType = operation.getInputType();
        }
        List<DataType> types = inputType.getLogical();
        boolean matched = true;
        if (types.size() == params.length && method.getName().equals(operation.getName())) {
            for (int i = 0; i < params.length; i++) {
                Class<?> clazz = params[i];
                Class<?> type = types.get(i).getPhysical();
                // Object.class.isAssignableFrom(int.class) returns false
                if (type != Object.class && (!type.isAssignableFrom(clazz))) {
                    matched = false;
                }
            }
        } else {
            matched = false;
        }
        return matched;

    }

    protected synchronized InvocationChain getInvocationChain(Method method, RuntimeWire wire) {
        if (fixedWire && chains.containsKey(method)) {
            return chains.get(method);
        }
        InvocationChain found = null;
        for (InvocationChain chain : wire.getInvocationChains()) {
            Operation operation = chain.getSourceOperation();
            if (operation.isDynamic()) {
                operation.setName(method.getName());
                found = chain;
                break;
            } else if (match(operation, method)) {
                found = chain;
                break;
            }
        }
        if (fixedWire) {
            chains.put(method, found);
        }
        return found;
    }

    protected void setEndpoint(Endpoint endpoint) {
        this.target = endpoint;
    }

    protected Object invoke(InvocationChain chain, Object[] args, RuntimeWire wire, EndpointReference source)
                         throws Throwable {
        Message msg = messageFactory.createMessage();
        msg.setFrom(source);
        if (target != null) {
            msg.setTo(target);
        } else {
            msg.setTo(wire.getEndpoint());
        }
        Invoker headInvoker = chain.getHeadInvoker();
        Operation operation = chain.getTargetOperation();
        msg.setOperation(operation);
        msg.setBody(args);

        Message msgContext = ThreadMessageContext.getMessageContext();
        
        // TODO - EPR - not required for OASIS
        //Object currentConversationID = msgContext.getFrom().getReferenceParameters().getConversationID();
        //conversationPreinvoke(msg, wire);
        
        handleCallback(msg, wire);
        ThreadMessageContext.setMessageContext(msg);
        boolean abnormalEndConversation = false;
        try {
            // dispatch the wire down the chain and get the response
            Message resp = headInvoker.invoke(msg);
            Object body = resp.getBody();
            if (resp.isFault()) {
                /* TODO - EPR - not required in OASIS
                // mark the conversation as ended if the exception is not a business exception
                if (currentConversationID != null ){
                    try {
                        boolean businessException = false;
                        
                        for (DataType dataType : operation.getFaultTypes()){
                            if (dataType.getPhysical() == ((Throwable)body).getClass()){
                                businessException = true;
                                break;
                            }
                        }
                        
                        if (businessException == false){
                            abnormalEndConversation = true;
                        }
                    } catch (Exception ex){
                        // TODO - sure what the best course of action is here. We have
                        //        a system exception in the middle of a business exception 
                    }
                }
                */
                throw (Throwable)body;
            }
            return body;
        } finally {
            //conversationPostInvoke(msg, wire, abnormalEndConversation);
            ThreadMessageContext.setMessageContext(msgContext);
        }
    }

    /**
     * @param msg
     * @param wire
     * @param interfaze
     * @throws TargetResolutionException
     */
    private void handleCallback(Message msg, RuntimeWire wire)
        throws TargetResolutionException {
        
        //ReferenceParameters parameters = msg.getFrom().getReferenceParameters();
        //parameters.setCallbackID(getCallbackID());
        
        if (msg.getFrom() == null || msg.getFrom().getCallbackEndpoint() == null) {
            return;
        }

        /* TODO - EPR - not required for OASIS
        parameters.setCallbackReference(msg.getFrom().getCallbackEndpoint());

        // If we are passing out a callback target
        // register the calling component instance against this 
        // new conversation id so that stateful callbacks will be
        // able to find it
        Object callbackObject = getCallbackObject();
        if (conversational && callbackObject == null) {
            // the component instance is already registered
            // so add another registration
            ScopeContainer<Object> scopeContainer = getConversationalScopeContainer(wire);

            if (scopeContainer != null && currentConversationID != null) {
                scopeContainer.addWrapperReference(currentConversationID, conversation.getConversationID());
            }
        }
        
        Interface interfaze = msg.getFrom().getCallbackEndpoint().getInterfaceContract().getInterface();
        if (callbackObject != null) {
            if (callbackObject instanceof ServiceReference) {
                CallableReferenceExt<?> callableReference = (CallableReferenceExt<?>)callbackObject;
                EndpointReference callbackRef = callableReference.getEndpointReference();
                
                // TODO - EPR - create chains on the callback reference in case this hasn't already happened
                //        needed as the bindings are not now matched until the chanins are created
                callableReference.getRuntimeWire().getInvocationChains();
                
                parameters.setCallbackReference(callbackRef);
            } else {
                if (interfaze != null) {
                    if (!interfaze.isConversational()) {
                        throw new IllegalArgumentException(
                                                           "Callback object for stateless callback is not a ServiceReference");
                    } else {
                        if (!(callbackObject instanceof Serializable)) {
                            throw new IllegalArgumentException(
                                          "Callback object for stateful callback is not Serializable");
                        }
                        ScopeContainer scopeContainer = getConversationalScopeContainer(wire);
                        if (scopeContainer != null) {
                            InstanceWrapper wrapper = new CallbackObjectWrapper(callbackObject);
                            scopeContainer.registerWrapper(wrapper, conversation.getConversationID());
                        }
                        parameters.setCallbackObjectID(callbackObject);
                    }
                }
            }
        }
        */
    }

    /**
     * Pre-invoke for the conversation handling
     * @param msg
     * @throws TargetResolutionException
     */
    /* TODO - EPR - not required for OASIS
    private void conversationPreinvoke(Message msg, RuntimeWire wire) {
        if (!conversational) {
            // Not conversational or the conversation has been started
            return;
        }

        ConversationManager conversationManager = ((RuntimeWireImpl2)wire).getConversationManager();

        if (conversation == null || conversation.getState() == ConversationState.ENDED) {

            conversation = conversationManager.startConversation(getConversationID());
            
            // if this is a local wire then set up the conversation timeouts here based on the 
            // parameters from the component
            if (wire.getEndpoint().getComponent() != null){
                conversation.initializeConversationAttributes((RuntimeComponent)wire.getEndpoint().getComponent());
            }
            
            // connect the conversation to the CallableReference so it can be retrieve in the future
            if (callableReference != null) {
                ((CallableReferenceImpl)callableReference).attachConversation(conversation);
            }
        } else if (conversation.isExpired()) {
            throw new ConversationEndedException("Conversation " +  conversation.getConversationID() + " has expired.");
        }

        // if this is a local wire then schedule conversation timeouts based on the timeout
        // parameters from the service implementation. If this isn't a local wire then
        // the RuntimeWireInvoker will take care of this
        if (wire.getEndpoint().getComponent() != null){
            conversation.updateLastReferencedTime();
        }

        msg.getFrom().getReferenceParameters().setConversationID(conversation.getConversationID());

    }
    */

    /**
     * Post-invoke for the conversation handling
     * @param wire
     * @param operation
     * @throws TargetDestructionException
     */
    /* TODO - not required for OASIS 
    @SuppressWarnings("unchecked")
    private void conversationPostInvoke(Message msg, RuntimeWire wire, boolean abnormalEndConversation)
                     throws TargetDestructionException {
        Operation operation = msg.getOperation();
        ConversationSequence sequence = operation.getConversationSequence();
        // We check that conversation has not already ended as there is only one
        // conversation manager in the runtime and so, in the case of remote bindings, 
        // the conversation will already have been stopped when we get back to the client
        if ((sequence == ConversationSequence.CONVERSATION_END || abnormalEndConversation) &&
            (conversation.getState() != ConversationState.ENDED)) {

            // remove conversation id from scope container
            ScopeContainer scopeContainer = getConversationalScopeContainer(wire);

            if (scopeContainer != null) {
                scopeContainer.remove(conversation.getConversationID());
            }

            conversation.end();
        }
    }
   

    private ScopeContainer<Object> getConversationalScopeContainer(RuntimeWire wire) {
        ScopeContainer<Object> scopeContainer = null;

        RuntimeComponent runtimeComponent = (RuntimeComponent)wire.getEndpointReference().getComponent();

        if (runtimeComponent instanceof ScopedRuntimeComponent) {
            ScopedRuntimeComponent scopedRuntimeComponent = (ScopedRuntimeComponent)runtimeComponent;
            ScopeContainer<Object> tmpScopeContainer = scopedRuntimeComponent.getScopeContainer();

            if ((tmpScopeContainer != null) && (tmpScopeContainer.getScope() == Scope.CONVERSATION)) {
                scopeContainer = tmpScopeContainer;
            }
        }

        return scopeContainer;
    }
     */

    /**
     * Creates a new conversation id
     * 
     * @return the conversation id
     */
    /* TODO - EPR - not required for OASIS
    private Object createConversationID() {
        if (getConversationID() != null) {
            return getConversationID();
        } else {
            return UUID.randomUUID().toString();
        }
    }
    */

    /**
     * @return the callableReference
     */
    public ServiceReference<?> getCallableReference() {
        return callableReference;
    }

    /**
     * @param callableReference the callableReference to set
     */
    public void setCallableReference(ServiceReference<?> callableReference) {
        this.callableReference = (ServiceReferenceExt<?>)callableReference;
    }

    /**
     * Minimal wrapper for a callback object contained in a ServiceReference
     */
    private static class CallbackObjectWrapper<T> implements InstanceWrapper<T> {

        private T instance;

        private CallbackObjectWrapper(T instance) {
            this.instance = instance;
        }

        public T getInstance() {
            return instance;
        }

        public void start() {
            // do nothing
        }

        public void stop() {
            // do nothing
        }

    }

}
