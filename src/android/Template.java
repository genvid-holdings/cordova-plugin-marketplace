/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.genvidtech.cordova.eos;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.Runnable;

public class Template extends CordovaPlugin {

    private static final String LogTag = "Template";

    /**
     * Constructor.
     */
    public Template() {}

    public boolean Initialize() throws JSONException {

        Activity activity = this.cordova.getActivity();

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
    }

    // All calls to EOS need to be made in the UI thread.
    // 
    private void runOnUiThread(Runnable r) {
        cordova.getActivity().runOnUiThread(r);
    }

    private boolean handleGetVersion(CallbackContext callbackContext) {        
        try {
            JSONObject resp = new JSONObject();
            resp.put("version", "0.0.1");
            callbackContext.success(resp);
        } catch(JSONException err) {
            callbackContext.error("Exception writing response: " + err.getMessage());
        }
        return true;
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            // sdk interface
            case "getVersion":
                return handleGetVersion(callbackContext);
            default:
                return false;
        }
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * This will maintain calling itself every 10th of a second after the initial trigger
     */

}
