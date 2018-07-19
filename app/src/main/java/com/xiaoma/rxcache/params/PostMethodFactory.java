package com.xiaoma.rxcache.params;

import android.util.Log;

import java.util.HashMap;

import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * author: mxc
 * date: 2018/7/18.
 */

public class PostMethodFactory extends MethodFactory {

    public PostMethodFactory(Request request, String[] bindParams) {
        super(request, bindParams);
    }

    @Override
    protected String getMethodName() {
        return "POST";
    }

    @Override
    public HashMap<String, String> parseParameters() {
        RequestBody body = request.body();
        if (body instanceof FormBody) {
            FormBody formBody = (FormBody) body;
            BIND:
            for (String parameter : bindParams)
                BODY:for (int i = 0; i < formBody.size(); i++) {
                    String name = formBody.encodedName(i);
                    if (parameter.equals(name)) {
                        saveParametersMap.put(parameter, formBody.encodedValue(i));
                        break BODY;
                    }

                }
        }else if(body instanceof MultipartBody){

        }


        return saveParametersMap;
    }
}
