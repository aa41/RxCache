package com.xiaoma.rxcache.params;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

/**
 * author: mxc
 * date: 2018/7/18.
 */

public class MethodFactoryAdapter {
    private final List<MethodFactory> factories = new ArrayList<>();
    protected final Request request;
    protected final String[] bindParams;

    public MethodFactoryAdapter(Request request, String[] bindParams) {
        this.request = request;
        this.bindParams = bindParams;
        factories.add(new GetMethodFactory(request, bindParams));
        factories.add(new PostMethodFactory(request, bindParams));
    }

    public MethodFactory get() {
        for (MethodFactory factory : factories) {
            if (request.method().toUpperCase().equals(factory.getMethodName())) {
                return factory;
            }
        }
        return null;
    }

}
