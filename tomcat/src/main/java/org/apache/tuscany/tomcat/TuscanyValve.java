/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.tuscany.tomcat;

import java.io.IOException;
import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.osoa.sca.CurrentModuleContext;
import org.osoa.sca.ModuleContext;
import org.osoa.sca.SCA;

import org.apache.tuscany.core.context.AggregateContext;
import org.apache.tuscany.core.context.EventContext;

/**
 * Valve that can be added to a pipeline to
 *
 * @version $Rev$ $Date$
 */
public class TuscanyValve extends ValveBase {
    /**
     * Name of the note that contains the request id
     */
    private static final String REQUEST_ID = "org.apache.tuscany.tomcat.REQUEST_ID";

    private static final ContextBinder BINDER = new ContextBinder();

    private final AggregateContext moduleComponentContext;

    public TuscanyValve(AggregateContext moduleComponentContext) {
        this.moduleComponentContext = moduleComponentContext;
    }

    public void invoke(Request request, Response response) throws IOException, ServletException {
        Object oldRequestId = request.getNote(REQUEST_ID);
        ModuleContext oldContext = CurrentModuleContext.getContext();
        try {
            // bind the current module context to the thread
            BINDER.setContext((ModuleContext) moduleComponentContext);

            // if we have not notified the runtime about this request, fire a request start event
            if (oldRequestId == null) {
                Object requestId = new Object();
                request.setNote(REQUEST_ID, requestId);
                moduleComponentContext.fireEvent(EventContext.REQUEST_START, requestId);
            }

            // invoke the next valve in the pipeline
            next.invoke(request, response);
        } catch (IOException e) {
            // rethrow exception from next valve
            throw e;
        } catch (ServletException e) {
            // rethrow exception from next valve
            throw e;
        } catch (Exception e) {
            // wrap other (unchecked) exceptions potentially thrown by the runtime
            throw new ServletException(e.getMessage(), e);
        } finally {
            // if we sent the request start notification, fire the request end event
            if (oldRequestId == null) {
                Object requestId = request.getNote(REQUEST_ID);
                moduleComponentContext.fireEvent(EventContext.REQUEST_END, requestId);
                request.removeNote(REQUEST_ID);
            }

            // restore the previous module context onto the thread
            BINDER.setContext(oldContext);
        }
    }

    private static class ContextBinder extends SCA {
        public void setContext(ModuleContext context) {
            setModuleContext(context);
        }

        public void start() {
            throw new AssertionError();
        }

        public void stop() {
            throw new AssertionError();
        }
    }
}
