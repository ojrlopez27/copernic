package edu.cmu.ubi.simu.harlequin.control;

import org.apache.commons.lang.StringUtils;

/**
 * Created by oscarr on 7/3/18.
 */
public class Action {
    private String user;
    private String message;
    private String notificationMessage;
    private boolean shouldUseDelay;
    private boolean isMsgForSelf;

    public Action(String user, String message) {
        this.user = StringUtils.capitalize(user);
        this.message = message;
    }

    public Action(String user, String message, boolean shouldUseDelay) {
        this(user, message);
        this.shouldUseDelay = shouldUseDelay;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isShouldUseDelay() {
        return shouldUseDelay;
    }

    public void setShouldUseDelay(boolean shouldUseDelay) {
        this.shouldUseDelay = shouldUseDelay;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public boolean isMsgForSelf() {
        return isMsgForSelf;
    }

    public void setMsgForSelf(boolean msgForSelf) {
        isMsgForSelf = msgForSelf;
    }
}
