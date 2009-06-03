/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.bpel.iapi;

import java.io.Serializable;

import javax.xml.namespace.QName;

/**
 * Event generated by the process store.
 * @author mszefler
 */
public class ProcessStoreEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        /** A process was deployed to the store. */
        DEPLOYED,
        
        /** A process was undeployed to from the store. */
        UNDEPLOYED,
        
        /** A process has been retired (i.e. it should no longer create new instances. */
        RETIRED,
        
        /** 
         * A process that was previously disabled or retired has become activated. This
         * event is also sent whenver an active process is "discovered" 
         */
        ACTIVATED,
        
        /** A process has been disabled: it should no longer execute for new or old instances. */
        DISABLED,
        
        /** A process property was changed. */
        PROPERTY_CHANGED,

        /** Cron schedule settings have been changed for the process */
        SCHEDULE_SETTINGS_CHANGED
    }

    /**
     * Event type. 
     * @see Type
     */
    public final Type type;
    
    /**
     * Process identifier.
     */
    public final QName pid; 
    
    public final String deploymentUnit;
    
    public ProcessStoreEvent(Type type, QName pid, String deploymentUnit) {
        this.type = type;
        this.pid = pid;
        this.deploymentUnit = deploymentUnit;
    }
    
    @Override
    public String toString() {
        return "{ProcessStoreEvent#" + type + ":" + pid +"}";
    }
}
