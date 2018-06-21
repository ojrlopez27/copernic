package edu.cmu.ubi.simu.scenario.demo;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.log.LogC;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 5/7/18.
 */
public class ClientExample{

    private ClientCommController commController;
    private AtomicBoolean ready = new AtomicBoolean(false);
    private final static String SESSION_ID = "my-session-id";

    public ClientExample() {
        commController = new ClientCommController.Builder( new Log4J() )
                .setServerAddress( "tcp://127.0.0.1:5555" ) //replace with your ip
                .setSessionId( SESSION_ID )
                .setRequestType(edu.cmu.inmind.multiuser.controller.common.Constants.REQUEST_CONNECT)
                .setResponseListener(new MyResponseListener())
                .build();
    }

    public static void main(String args[]){
        ClientExample example = new ClientExample();
        // just for sync purposes
        CommonUtils.sleep(100);
        example.send("test");
        CommonUtils.sleep(3000);
        System.out.println("Done!");
    }

    public void send(Object message){
        try {
            while( !ready.get() ){
                CommonUtils.sleep(10);
            }
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setPayload(message.toString());
            sessionMessage.setSessionId(SESSION_ID);
            commController.send(SESSION_ID, CommonUtils.toJson(sessionMessage));
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    class MyResponseListener implements ResponseListener {
        @Override
        public void process(String message) {
            Log4J.debug(this, "Response from server: " + message);
            ready.set(true);
        }
    }
}
