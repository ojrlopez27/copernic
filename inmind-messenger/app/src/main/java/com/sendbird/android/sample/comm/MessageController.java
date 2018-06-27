package com.sendbird.android.sample.comm;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.sendbird.android.BaseMessage;
import com.sendbird.android.sample.R;
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
    private Context ctx;

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
        this.ctx = activity.getApplicationContext();
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

        if(activity != null) {
            showNotification( sessionMessage.getMessageId() );
        }
    }


    public void showNotification(String notificationType){
        int mNotificationID = -1; //Make sure this number is unique, we use this to update or cancel notification.
        int smallIcon = -1, bigIcon = -1;
        String title = "";
        String description = "";
        String summary = "";

        switch (notificationType){
            case "PHARMACY":
                mNotificationID = 1;
                title = "CVS Pharmacy";
                description = "CVS Discount Coupons";
                summary = "You have some discount coupons waiting for you";
                smallIcon = R.drawable.cvs_small;
                bigIcon = R.drawable.cvs_big;
                break;

            case "ORGANIC":
                mNotificationID = 2;
                title = "Whole Foods Market";
                description = "Match with your preferences!";
                summary = "Whole Foods provides Organic and Non-GMO foods!";
                smallIcon = R.drawable.whole_foods_small;
                bigIcon = R.drawable.whole_foods_big;
                break;

        }

        if(mNotificationID != -1) {
            //Sound & icon related to notification
            Uri mNotificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Bitmap bitmap = BitmapFactory.decodeResource(ctx.getResources(), bigIcon);
            //Build the object and set the title, text, sound etc.. properties
            NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, "channelId")
                    .setLargeIcon(bitmap)
                    .setContentTitle(title)
                    .setContentText(description)
                    .setSound(mNotificationSoundUri)
                    .setSmallIcon(smallIcon) //to small icon on the right hand side
                    .setWhen(System.currentTimeMillis()); // Displays the date on right side bottom

            NotificationCompat.BigPictureStyle s =
                    new NotificationCompat.BigPictureStyle().bigPicture(bitmap);
            s.setSummaryText(summary);
            nb.setStyle(s);

            Notification mNotificationObject = nb.build();
            //This is to keep the default settings of notification,
            mNotificationObject.defaults |= Notification.DEFAULT_VIBRATE;
            mNotificationObject.flags |= Notification.FLAG_AUTO_CANCEL;
            //This is to show the ticker text which appear at top.
            mNotificationObject.tickerText = title + "n" + description;
            //Trigger the notification
            NotificationManager manager = (NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(mNotificationID, mNotificationObject);
        }
    }
}
