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
package org.apache.tuscany.sca.idl.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.tuscany.sca.idl.Interface;
import org.apache.tuscany.sca.idl.Operation;

/**
 * Represents a service interface.
 *
 *  @version $Rev$ $Date$
 */
public class InterfaceImpl implements Interface {
	
	private boolean remotable;
	private List<Operation> operations = new ArrayList<Operation>();
	private boolean unresolved = false;

	public boolean isRemotable() {
		return remotable;
	}

	public void setRemotable(boolean local) {
		this.remotable = local;
	}

	public List<Operation> getOperations() {
		return operations;
	}

	public boolean isUnresolved() {
		return unresolved;
	}

	public void setUnresolved(boolean undefined) {
		this.unresolved = undefined;
	}

}
