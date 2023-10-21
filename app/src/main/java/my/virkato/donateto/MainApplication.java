package my.virkato.donateto;

import android.app.Application;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Ads.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

}
