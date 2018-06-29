package edu.cmu.ubi.simu.harlequin.util;

/**
 * Created by oscarr on 6/15/18.
 */
public class SimuUtils {
    private final static int MAX_MSG_LENGTH = 20;
    private final static String MSG_TOKEN = "###_MSG_TOKEN_###";

    public static String[] breakIntoMessages(String message){
        //we only allow 2 lines in the bubble
        if(message.length() > 2 * MAX_MSG_LENGTH) message = message.substring(0, 2 * MAX_MSG_LENGTH);
        String[] messages = message.split("\\s");
        StringBuffer buffer = new StringBuffer();
        int length = 0;
        for(String msg : messages){
            buffer.append(msg + " ");
            length += msg.length() + 1;
            if(length >= MAX_MSG_LENGTH){
                buffer.append(MSG_TOKEN);
                length = 0;
            }
        }
        return buffer.toString().split(MSG_TOKEN);
    }
}
