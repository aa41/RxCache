package com.xiaoma.rxcache.params;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import okhttp3.Request;

/**
 * author: mxc
 * date: 2018/7/18.
 */

public abstract class MethodFactory {

    protected final Request request;
    protected final String[] bindParams;
    protected final LinkedHashMap<String, String> saveParametersMap = new LinkedHashMap<>();

    public MethodFactory(Request request, String[] bindParams) {
        this.request = request;
        this.bindParams = bindParams;

    }

    protected abstract String getMethodName();


    public abstract HashMap<String, String> parseParameters();




}
