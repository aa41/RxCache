package com.xiaoma.rxcache;

import android.app.Application;

/**
 * author: mxc
 * date: 2018/7/18.
 */

public class APP extends Application {
    public static APP app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

    }
}
