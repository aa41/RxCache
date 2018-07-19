package com.xiaoma.rxcache.params;

import java.util.HashMap;
import java.util.Set;

import okhttp3.HttpUrl;
import okhttp3.Request;

/**
 * author: mxc
 * date: 2018/7/18.
 */

public class GetMethodFactory extends MethodFactory {

    public GetMethodFactory(Request request, String[] bindParams) {
        super(request, bindParams);
    }

    @Override
    protected String getMethodName() {
        return "GET";
    }

    @Override
    public HashMap<String, String> parseParameters() {
        HttpUrl url = request.url();
        Set<String> parameterNames = url.queryParameterNames();

        BIND:
        for (String bindParams : bindParams) {
            PARAMS:
            for (String parameter : parameterNames) {
                if (bindParams.equals(parameter)) {
                    saveParametersMap.put(parameter, url.queryParameter(parameter));
                    break PARAMS;
                }

            }
        }
        return saveParametersMap;
    }
}
