package edu.cmu.ubi.simu.copernic.orchestrator;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.ubi.simu.copernic.control.CopernicController;
import edu.cmu.ubi.simu.copernic.util.MsgConstants;

import static edu.cmu.ubi.simu.copernic.util.MsgConstants.ACK;

/**
 * Created by oscarr on 5/7/18.
 */
@BlackboardSubscription(messages = {MsgConstants.MSG_SEND_RESPONSE})
public class SimuOrchestrator extends ProcessOrchestratorImpl {

    private CopernicController copernicController;

    @Override
    public void postCreate() {
        super.postCreate();
        try {
            copernicController = CopernicController.getInstance();
            copernicController.addOrchestrator(getSessionId(), this);
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    @Override
    public void process(String message) throws Throwable {
        super.process(message);
        SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
        // if an user says "hi" in order to start the interaction, we just send back a confirmation
        if( sessionMessage.getPayload().equalsIgnoreCase("Hi") ) {
            sendInMindResponse(ACK);
        }
        // if we receive messages from the other phone, then just print them on the current phone
        else if( sessionMessage.getRequestType().equals(Constants.CROSS_SESSION_MESSAGE) ){
            sendInMindResponse(sessionMessage.getPayload(), sessionMessage.getMessageId());
        }
        // otherwise, processes the message
        else {
            blackboard.post(this, MsgConstants.MSG_PROCESS_COMMAND, sessionMessage.getPayload());
        }
    }

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent blackboardEvent) throws Throwable {
        sendInMindResponse((String) blackboardEvent.getElement());
    }

    public void sendInMindResponse(String message){
        sendInMindResponse(message, "");
    }

    public void sendInMindResponse(String message, String messageId){
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setPayload(message);
        sessionMessage.setMessageId(messageId);
        sessionMessage.setPayload("[INMIND] " + sessionMessage.getPayload());
        sendResponse( sessionMessage );
    }
}
