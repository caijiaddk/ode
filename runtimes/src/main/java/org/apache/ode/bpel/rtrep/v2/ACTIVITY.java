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
package org.apache.ode.bpel.rtrep.v2;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.evar.ExternalVariableModuleException;
import org.apache.ode.bpel.evt.ActivityDisabledEvent;
import org.apache.ode.bpel.evt.ActivityEvent;
import org.apache.ode.bpel.evt.EventContext;
import org.apache.ode.bpel.evt.ScopeEvent;
import org.apache.ode.bpel.evt.VariableReadEvent;
import org.apache.ode.bpel.rapi.PartnerLink;
import org.apache.ode.bpel.rapi.PartnerLinkModel;
import org.apache.ode.bpel.rapi.PropagationRule;
import org.apache.ode.jacob.IndexedObject;
import org.w3c.dom.Node;

/**
 * Base template for activities.
 */
abstract class ACTIVITY extends BpelJacobRunnable implements IndexedObject {
	private static final Log __log = LogFactory.getLog(ACTIVITY.class);
    protected ActivityInfo _self;

    /**
     * Permeability flag, if <code>false</code> we defer outgoing links until successful completion.
     */
    protected boolean _permeable = true;

    protected LinkFrame _linkFrame;
    
    public ACTIVITY(ActivityInfo self, ScopeFrame scopeFrame, LinkFrame linkFrame) {
        assert self != null;
        assert scopeFrame != null;
        assert linkFrame != null;

        _self = self;
        _scopeFrame = scopeFrame;
        _linkFrame = linkFrame;
    }

    public Object getKey() {
        return new Key(_self.o,_self.aId);
    }

    
    protected void sendVariableReadEvent(VariableInstance var) {
    	VariableReadEvent vre = new VariableReadEvent();
    	vre.setVarName(var.declaration.name);
    	sendEvent(vre);
    }
    
    protected void sendEvent(ActivityEvent event) {
        event.setActivityName(_self.o.name);
        event.setActivityType(_self.o.getType());
        event.setActivityDeclarationId(_self.o.getId());
        event.setActivityId(_self.aId);
        if (event.getLineNo() == -1) {
            event.setLineNo(getLineNo());
        }
        sendEvent((ScopeEvent) event);
    }

    protected void sendEvent(ScopeEvent event) {
        if (event.getLineNo() == -1 && _self.o.debugInfo != null) {
            event.setLineNo(_self.o.debugInfo.startLine);
        }
        _scopeFrame.fillEventInfo(event);
        fillEventContext(event);
        getBpelRuntime().sendEvent(event);
    }

    /**
     * Populate BpelEventContext, to be used by Registered Event Listeners
     * @param event ScopeEvent
     */
    protected void fillEventContext(ScopeEvent event)
    {
        EventContext eventContext = new EventContextImpl(
                _scopeFrame.oscope, _scopeFrame.scopeInstanceId, getBpelRuntime());
        event.eventContext = eventContext;
    }

    protected void dpe(Collection<OLink> links) {
        // Dead path all of the outgoing links (nothing has been activated yet!)
         for (OLink link : links) {
             if (__log.isDebugEnabled()) __log.debug("DPE on link " + link.name);
             _linkFrame.resolve(link).channel.linkStatus(false);
         }
    }
    
    protected OConstants getConstants() {
    	return _self.o.getOwner().constants;
    }

    /**
     * Perform dead-path elimination on an activity that was
     * <em>not started</em>.
     * 
     * @param activity
     */
    protected void dpe(OActivity activity) {
        dpe(activity.sourceLinks);
        dpe(activity.outgoingLinks);
        sendEvent(new ActivityDisabledEvent());
        // TODO: register listeners for target / incoming links
    }

    protected EvaluationContext getEvaluationContext() {
        return new ExprEvaluationContextImpl(_scopeFrame, getBpelRuntime());
    }

    private int getLineNo() {
        if (_self.o.debugInfo != null && _self.o.debugInfo.startLine != -1) {
            return _self.o.debugInfo.startLine;
        }
        return -1;
    }

    Node fetchVariableData(VariableInstance variable, boolean forWriting) throws FaultException {
        return getBpelRuntime().fetchVariableData(variable, _scopeFrame, forWriting);
    }

    void commitChanges(VariableInstance var, Node val) throws ExternalVariableModuleException {
        getBpelRuntime().commitChanges(var, _scopeFrame, val);
    }
    
    Set<PropagationRule> computePropagationRules(OComm ocomm, Set<OContextPropagation> propagates) {
        Map<PartnerLink, Set<String>> contextsByPL = new LinkedHashMap<PartnerLink, Set<String>>(); 
        
        if (propagates != null) {
            for (OContextPropagation p : propagates) {
                PartnerLink pl = _scopeFrame.resolve(p.fromPartnerLink);
                Set<String> contexts = contextsByPL.get(pl);
                if (contexts == null) {
                    contexts = new HashSet<String>();
                    contextsByPL.put(pl, contexts);
                }
                if (contexts.contains("*")) {
                    continue;
                }
                for (String context : p.contexts) {
                    if (context.equals("*")) {
                        contexts.clear();
                        contexts.add("*");
                        break;
                    } else {
                        contexts.add(context);
                    }
                }
            }
        }
        
        for (org.apache.ode.bpel.iapi.ProcessConf.PropagationRule p : getBpelRuntime().getPropagationRules()) {
            PartnerLinkModel pl = _self.o.getOwner().getPartnerLink(p.getFromPL());
            if (pl == null) {
                __log.warn("Invoke " + ocomm + " is configured to propagate context information from partner link " + p.getFromPL() + " which could not be resolved. Therefore the propagation rule will be ignored." );
                continue;
            }
            if (p.getToPL().equals(ocomm.getPartnerLink().name)) {
                Set<String> contexts = contextsByPL.get(pl);
                if (contexts == null) {
                    contexts = new HashSet<String>();
                    contextsByPL.put(_scopeFrame.resolve(pl), contexts);
                }
                if (contexts.contains("*")) {
                    continue;
                }
                for (String context : p.getContexts()) {
                    if ("*".equals(context)) {
                        contexts.clear();
                        contexts.add("*");
                        break;
                    } else {
                        contexts.add(context);
                    }
                }
            }
        }
        
        Set<PropagationRule> ruleset = new LinkedHashSet<PropagationRule>();
        for (PartnerLink pl : contextsByPL.keySet()) {
            PropagationRule pr = new PropagationRule();
            pr.setFromPL(pl);
            pr.setContexts(contextsByPL.get(pl));
            ruleset.add(pr);
        }

        return ruleset;
    }
    
    public static final class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        final OActivity type;

        final long aid;

        public Key(OActivity type, long aid) {
            this.type = type;
            this.aid = aid;
        }

        @Override
        public String toString() {
            return type + "::" + aid;
        }
    }
}
