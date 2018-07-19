/*
 * Copyright (C) 2016 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiaoma.rxcache;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.xiaoma.rxcache.diskcache.DiskLruCache;
import com.xiaoma.rxcache.diskcache.DiskLruCacheHelper;
import com.xiaoma.rxcache.params.MethodFactory;
import com.xiaoma.rxcache.params.MethodFactoryAdapter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.Call;
import retrofit2.Response;

final class CallExecuteObservable<T> extends Observable<Response<T>> {
    private final Call<T> originalCall;
    private final Annotation[] annotations;
    private long time;
    private String bindParams[];
    private DiskLruCache cache;
    private static final String URL_CACHE = "URL_CACHE";
    private String fileName;

    CallExecuteObservable(Call<T> originalCall, Annotation[] annotations) {
        this.originalCall = originalCall;
        this.annotations = annotations;
    }

    @Override
    protected synchronized void subscribeActual(Observer<? super Response<T>> observer) {
        // Since Call is a one-shot type, clone it for each new observer.
        Call<T> call = originalCall.clone();

        try {
            for (Annotation annotation : annotations) {
                if (annotation instanceof Cache) {
                    time = ((Cache) annotation).time();
                    bindParams = ((Cache) annotation).bindParams();
                    break;
                }
            }
            MethodFactoryAdapter adapter = new MethodFactoryAdapter(call.request(), bindParams);
            MethodFactory factory = adapter.get();
            HashMap<String, String> parseParameters = factory.parseParameters();
            String url = call.request().url().toString();
            boolean isFirst = true;
            StringBuffer buffer = new StringBuffer();
            for (HashMap.Entry<String, String> entry : parseParameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isFirst) {
                    buffer.append("?").append(key).append("=").append(value);
                    isFirst = false;
                } else {
                    buffer.append("&").append(key).append("=").append(value);
                }
            }
            String cacheUrl = url + buffer.toString();
            long nowTime = System.currentTimeMillis();
            fileName = cacheUrl + "&" + nowTime;

            cache = DiskLruCacheHelper.createCache(APP.app, URL_CACHE);
            String responseCacheData = DiskLruCacheHelper.readCacheToString(cache, cacheUrl);
            int indexOfTime = responseCacheData.indexOf("\n");
            String[] times = responseCacheData.substring(0, indexOfTime).split(":");
            responseCacheData = responseCacheData.substring(indexOfTime + 1);
            long diskCacheTime = Long.parseLong(times[1]);
            if (!TextUtils.isEmpty(responseCacheData) && diskCacheTime != -1 && nowTime - time <= diskCacheTime) {
                E e = new Gson().fromJson(responseCacheData, E.class);
                Response<T> cacheResponse = (Response<T>) Response.success(e);
                observer.onNext(cacheResponse);
            } else {
                cache.delete();
                cache = null;

            }
        } catch (Exception e) {
            if (cache != null && !cache.isClosed()) {
                try {
                    cache.delete();
                } catch (IOException e1) {

                }
                call = null;
            }
        }


        CallDisposable disposable = new CallDisposable(call);
        observer.onSubscribe(disposable);

        boolean terminated = false;
        try {
            Response<T> response = call.execute();
            E e = (E) response.body();
            String responseData = new Gson().toJson(e);
            if (cache == null) {
                cache = DiskLruCacheHelper.createCache(APP.app, URL_CACHE);
            }
            DiskLruCacheHelper.writeStringToCache(cache, responseData, fileName);
            if (!disposable.isDisposed()) {
                observer.onNext(response);
            }
            if (!disposable.isDisposed()) {
                terminated = true;
                observer.onComplete();
            }
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            if (terminated) {
                RxJavaPlugins.onError(t);
            } else if (!disposable.isDisposed()) {
                try {
                    observer.onError(t);
                } catch (Throwable inner) {
                    Exceptions.throwIfFatal(inner);
                    RxJavaPlugins.onError(new CompositeException(t, inner));
                }
            }
        }
    }

    private static final class CallDisposable implements Disposable {
        private final Call<?> call;
        private volatile boolean disposed;

        CallDisposable(Call<?> call) {
            this.call = call;
        }

        @Override
        public void dispose() {
            disposed = true;
            call.cancel();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
