package edu.cmu.inmind.messenger.main;


import android.app.Application;

import com.sendbird.android.SendBird;
import edu.cmu.inmind.messenger.utils.PreferenceUtils;

public class BaseApplication extends Application {

    private static final String APP_ID = "96F1AF2B-967F-46A1-B752-19FA6F7ABDA3"; //"9DA1B1F4-0BE6-4DA8-82C5-2E81DAB56F23"; // US-1 Demo
    public static final String VERSION = "3.0.40";

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceUtils.init(getApplicationContext());

        SendBird.init(APP_ID, getApplicationContext());
    }
}
