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

package org.apache.tuscany.sca.itest.conversational;

import org.apache.tuscany.sca.node.Contribution;
import org.apache.tuscany.sca.node.ContributionLocationHelper;
import org.apache.tuscany.sca.node.Node;
import org.apache.tuscany.sca.node.NodeFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConversationLifetimeTestCase {

    private static Node node;

    @BeforeClass
    public static void setUp() throws Exception {
        String location = ContributionLocationHelper.getContributionLocation("conversationLifetime.composite");
        node = NodeFactory.newInstance().createNode("conversationLifetime.composite", new Contribution("c1", location));
        node.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (node != null) {
            node.stop();
        }
    }

    /**
     * Following a clarification re. the wording of the Java Common Annotations and APIs 1.00 Specification
     * (see TUSCANY-2055) the following is accepted to be the intended operation
     * 
     * Whether the conversation ID is chosen by the user or is generated by the system, the client
     * may access the conversation ID by calling getConversationID() on the current conversation object.
     * 
     * ServiceReference.getConversationID() - Returns the id supplied by the user that will be associated with
     * future conversations initiated through this reference, or null if no ID has been set by the user.
     * 
     * ServiceReference.setConversationID(Object conversationId) - Set an ID, supplied by the user, to associate with any future conversation
     * started through this reference. If the value supplied is null then the id will be generated
     * by the implementation. Throws an IllegalStateException if a conversation is currently
     * associated with this reference. 
     * 
     */

    /**
     * Verify that ServiceReference.getConversationID() returns null before a conversation
     * ID has been set manually.
     */
    @Test
    public void getConversationID() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.getState();
        Assert.assertNull(service.getConversationID());
    }

    /**
     * Verify that ServiceReference.getConversationID() returns any value previous set through the
     * setConversationID() API.
     */
    @Test
    public void getConversationID2() {
        String userProvidedID = "A conversation ID";
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.setConversationID(userProvidedID);
        service.getState();
        Assert.assertEquals(service.getConversationID(), userProvidedID);
    }

    /**
     * Whether the conversation ID is chosen by the user or is generated by the system, the client
     * may access the conversation ID by calling getConversationID() on the current conversation object.
     * Here test the manually set conversationID
     */
    @Test
    public void getConversationID3() {
        String userProvidedID = "A conversation ID 3";
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.setConversationID(userProvidedID);
        service.getState();
        Assert.assertEquals(service.getConversationObjectConversationId(), userProvidedID);
    }

    /**
     * Whether the conversation ID is chosen by the user or is generated by the system, the client
     * may access the conversation ID by calling getConversationID() on the current conversation object.
     * Here test the auto generated conversationId
     */
    @Test
    public void getConversationID4() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.getState();
        Assert.assertNotNull(service.getConversationObjectConversationId());
    }

    /**
     * Java Common Annotations and APIs 1.00 Specification line 494-495 Verify:
     * If a method is invoked on a service reference after an
     * "@EndsConversation" method has been called then a new conversation will
     * automatically be started.
     */
    @Test
    public void implicitStartNewConversationAfterEnd() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.getState();
        Object firstID = service.getConversationObjectConversationId();
        service.endConversationViaAnnotatedMethod();
        service.getState();
        Assert.assertNotSame(firstID, service.getConversationObjectConversationId());
    }

    /**
     * Java Common Annotations and APIs 1.00 Specification line 495-495 Verify: If a
     * method is invoked on a service reference after an "@EndsConversation"
     * method has been called then a new conversation will automatically be
     * started. Note: Uses Conversation.end() rather than "@EndsConversation"
     */
    @Test
    public void implicitStartNewConversationAfterEnd2() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.getState();
        Object firstID = service.getConversationObjectConversationId();
        service.endConversation();
        service.getState();
        Assert.assertNotSame(firstID, service.getConversationObjectConversationId());
    }

    /**
     * Java Common Annotations and APIs 1.00 Specification line 495-497 Verify:
     * If ServiceReference.getConversationID() is called after the
     * "@EndsConversation" method is called, but before the next conversation
     * has been started, it will return null.
     */
    @Test
    public void nullConversationIDAfterEndConversation() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.getState();
        service.endConversationViaAnnotatedMethod();
        Assert.assertNull(service.getConversationObjectConversationId());
    }

    /**
     * Java Common Annotations and APIs 1.00 Specification line 495-497 Verify:
     * If ServiceReference.getConversationID() is called after the
     * "@EndsConversation" method is called, but before the next conversation
     * has been started, it will return null.  Note: Uses explicit set of Conversation ID
     */
    @Test
    public void nullConversationIDAfterEndConversation1a() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.setConversationID("User provided ID");
        service.getState();
        service.endConversationViaAnnotatedMethod();
        Assert.assertNull(service.getConversationObjectConversationId());
    }

    /**
     * Java Common Annotations and APIs 1.00 Specification line 495-497 Verify: If
     * ServiceReference.getConversationID() is called after the
     * "@EndsConversationmethod" is called, but before the next conversation has
     * been started, it will return null. Note: Uses Conversation.end() rather
     * than "@EndsConversation"
     */
    @Test
    public void nullConversationIDAfterEndConversation2() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.getState();
        service.endConversation();
        Assert.assertNull(service.getConversationObjectConversationId());
    }

    /**
     * Java Common Annotations and APIs 1.00 Specification line 495-497 Verify: If
     * ServiceReference.getConversationID() is called after the
     * "@EndsConversationmethod" is called, but before the next conversation has
     * been started, it will return null. Note: Uses Conversation.end() rather
     * than "@EndsConversation".  Note 2: Uses explicit set of Conversation ID
     */
    @Test
    public void nullConversationIDAfterEndConversation2a() {
        CService service = node.getService(CService.class, "ConversationalCComponent");
        service.setConversationID("User provided ID");
        service.getState();
        service.endConversation();
        Assert.assertNull(service.getConversationObjectConversationId());
    }

}
