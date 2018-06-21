package com.sendbird.android.sample.comm;

import android.app.Activity;

import com.sendbird.android.BaseMessage;
import com.sendbird.android.sample.groupchannel.GroupChatAdapter;
import com.sendbird.android.sample.utils.PreferenceUtils;

import java.util.Random;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.log.LogC;

/**
 * Created by oscarr on 6/8/18.
 */

public class MessageController implements ResponseListener{
    private static MessageController instance;
    private ClientCommController clientCommController;
    private String sessionId;
    private GroupChatAdapter chatAdapter;
    private Activity activity;
    private Random random;

    private MessageController() {
        this.sessionId = PreferenceUtils.getUserId();
        random = new Random();
    }

    public static MessageController getInstance(){
        if(instance == null){
            instance = new MessageController();
        }
        return instance;
    }

    public void setChatAdapter(GroupChatAdapter chatAdapter) {
        this.chatAdapter = chatAdapter;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void connect(){
        clientCommController = new ClientCommController.Builder(new LogC() )
                .setServerAddress( "tcp://inmind-harlequin.ddns.net:5555" )
//                .setServerAddress("tcp://128.237.189.221:5555")
                //.setServerAddress("tcp://100.6.9.197:5555")
                .setSessionId(sessionId)
                .setRequestType(Constants.REQUEST_CONNECT)
                .setResponseListener(this)
                .build();
    }

    public static void send(final String message){
        CommonUtils.execute(new Runnable() {
            @Override
            public void run() {
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setPayload(message);
                sessionMessage.setSessionId(instance.sessionId);
                instance.clientCommController.send(instance.sessionId, sessionMessage);
            }
        });
    }

    public void disconnect(){
        CommonUtils.execute(new Runnable() {
            @Override
            public void run() {
                clientCommController.disconnect(sessionId);
            }
        });
    }

    @Override
    public void process(final String message) {
        // message from InMind
        final SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
        if( activity != null && !message.contains(Constants.SESSION_INITIATED)
                && !message.contains(Constants.SESSION_RECONNECTED)){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatAdapter.addFirst( BaseMessage.buildFromSerializedData(
                            new InMindMessage( String.valueOf(random.nextLong()),
                                    sessionMessage.getPayload()).serialize() ));
                }
            });
        }
    }
}
