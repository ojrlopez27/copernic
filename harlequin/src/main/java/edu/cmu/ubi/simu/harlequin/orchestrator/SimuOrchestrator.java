package edu.cmu.ubi.simu.harlequin.orchestrator;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.ubi.simu.harlequin.control.HarlequinController;
import edu.cmu.ubi.simu.harlequin.util.MsgConstants;

/**
 * Created by oscarr on 5/7/18.
 */
@BlackboardSubscription(messages = {MsgConstants.MSG_SEND_RESPONSE})
public class SimuOrchestrator extends ProcessOrchestratorImpl {

    private HarlequinController harlequinController;

    @Override
    public void postCreate() {
        super.postCreate();
        try {
            harlequinController = HarlequinController.getInstance();
            harlequinController.addOrchestrator(getSessionId(), this);
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    @Override
    public void process(String message) throws Throwable {
        super.process(message);
        SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
        // if we receive messages from the other phone, then just print them on the current phone
        if( sessionMessage.getRequestType().equals(Constants.CROSS_SESSION_MESSAGE) ){
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
