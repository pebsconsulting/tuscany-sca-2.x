/*
 * (c) Copyright BEA Systems, Inc., Cape Clear Software, International Business Machines Corp, Interface21, IONA Technologies,
 * Oracle, Primeton Technologies, Progress Software, Red Hat, Rogue Wave Software, SAP AG., Siemens AG., Software AG., Sybase
 * Inc., TIBCO Software Inc., 2005, 2007. All rights reserved.
 * 
 * see http://www.osoa.org/display/Main/Service+Component+Architecture+Specifications
 */
package org.oasisopen.sca;

/**
 * Interface representing a Conversation providing access to the conversation id and and a mechanism
 * to terminate the conversation.
 * 
 * @version $Rev$ $Date$
 */
public interface Conversation {
    /**
     * Returns the identifier for this conversation.
     * If a user-defined identity had been supplied for this reference then its value will be returned;
     * otherwise the identity generated by the system when the conversation was initiated will be returned.
     *
     * @return the identifier for this conversation
     */
    Object getConversationID();

    /**
     * End this conversation.
     */
    void end();
}
