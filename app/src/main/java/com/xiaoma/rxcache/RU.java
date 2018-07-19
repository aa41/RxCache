package com.xiaoma.rxcache;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.*;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * author: mxc
 * date: 2018/6/22.
 */

public class RU {
    public static Api getService() {
        return new Retrofit.Builder().baseUrl("http://service.picasso.adesk.com")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(Api.class);
    }
}
