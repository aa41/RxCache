package com.xiaoma.rxcache;


import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * author: mxc
 * date: 2018/6/22.
 */

public interface Api {


    @FormUrlEncoded
    @POST("v1/vertical/vertical")
    @Cache(time = 7 * 24 * 60 * 60, bindParams = {"first"})
    Observable<E> query(@Field("adult") boolean adult, @Field("first") int first);

}
