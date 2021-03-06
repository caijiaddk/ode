package org.apache.ode.bpel.engine;

import java.util.List;

import org.apache.ode.bpel.iapi.BpelEngineException;
import org.apache.ode.bpel.iapi.EndpointReference;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.MyRoleMessageExchange;

/**
 * A transacted MEP that delegates messages to a list of subscribers  
 *
 * @author $author$
 * @version $Revision$
  */
public class BrokeredTransactedMyRoleMessageExchangeImpl
    extends TransactedMyRoleMessageExchangeImpl {
    private List<MyRoleMessageExchange> subscribers;
    private MyRoleMessageExchange template;

    /**
     * Creates a new BrokeredTransactedMyRoleMessageExchangeImpl object.
     *
     * @param process 
     * @param subscribers 
     * @param mexId 
     * @param oplink 
     * @param template 
     */
    public BrokeredTransactedMyRoleMessageExchangeImpl(ODEProcess process,
        List<MyRoleMessageExchange> subscribers, String mexId, MyRoleMessageExchange template) {
        super(process, mexId, null, template.getOperation(),
            template.getServiceName());
        this.subscribers = subscribers;
        this.template = template;
    }

    /**
     * Propagate the invoke transacted call to each subscriber
     *
     * @return type
     */
    public Status invokeTransacted() {
        for (MyRoleMessageExchange subscriber : subscribers) {
            subscriber.invokeTransacted();
        }

        return Status.COMPLETED;
    }

    /**
     * Fool the engine into thinking I'm one-way, wherever possible
     *
     * @return type
     */
    @Override
    public AckType getAckType() {
        return template.getAckType();
    }
    
    /**
     * Return the status of one of the subscribers
     *
     * @return status
     */
    @Override
    public Status getStatus() {
        return template.getStatus();
    }

    /**
     * Use the EPR of one of the subscribers as my EPR
     *
     * @return type
     *
     * @throws BpelEngineException BpelEngineException 
     */
    @Override
    public EndpointReference getEndpointReference() throws BpelEngineException {
        return template.getEndpointReference();
    }

    /**
     * Use the response from one of the subscribers as my response 
     *
     * @return type
     */
    @Override
    public Message getResponse() {
        return template.getResponse();
    }

    /**
     * Propagate set request call to every subscriber
     *
     * @param request request 
     */
    @Override
    public void setRequest(Message request) {
        for (MyRoleMessageExchange subscriber : subscribers) {
            subscriber.setRequest(cloneMessage(request));
        }
    }

    /**
     * Propagate set timeout call to every subscriber
     *
     * @param timeout timeout 
     */
    @Override
    public void setTimeout(long timeout) {
        for (MyRoleMessageExchange subscriber : subscribers) {
            subscriber.setTimeout(timeout);
        }
    }
}
