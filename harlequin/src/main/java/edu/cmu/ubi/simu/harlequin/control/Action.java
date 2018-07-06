package edu.cmu.ubi.simu.harlequin.control;

import org.apache.commons.lang.StringUtils;

/**
 * Created by oscarr on 7/3/18.
 */
public class Action {
    private String user;
    private String message;
    private String notificationMessage;
    private ActionCallback callback;

    public Action(String user, String message) {
        this.user = StringUtils.capitalize(user);
        this.message = message;
    }

    public Action(String user, String message, String notificationMessage) {
        this(user, message);
        this.notificationMessage = notificationMessage;
    }

    public Action(String user, String message, ActionCallback callback) {
        this(user, message);
        this.callback = callback;
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

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public ActionCallback getCallback() {
        return callback;
    }

    public void setCallback(ActionCallback callback) {
        this.callback = callback;
    }
}
