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
package com.genvidtech.cordova.marketplace;

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
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.InstallSourceInfo;

import java.lang.Runnable;

public class Marketplace extends CordovaPlugin {

    private static final String LogTag = "Marketplace";

    /**
     * Constructor.
     */
    public Marketplace() {}

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

    private void runOnUiThread(Runnable r) {
        cordova.getActivity().runOnUiThread(r);
    }

    private boolean handleGetInfo(CallbackContext callbackContext) { 
        Context context = cordova.getContext();
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        try {
            InstallSourceInfo source = packageManager.getInstallSourceInfo(packageName);
            String initiatingPackageName = source.getInitiatingPackageName();
            // TODO: Add SigningInfo.
            String installingPackageName = source.getInstallingPackageName();
            String originatingPackageName = source.getOriginatingPackageName();
            String updateOwnerPackageName = source.getUpdateOwnerPackageName();
            JSONObject resp = new JSONObject();
            resp.put("name", initiatingPackageName); // Common to all platform
            // Android specifics
            resp.put("originatingPackage", originatingPackageName);
            resp.put("installingPackage", installingPackageName);
            resp.put("initiatingPackage", initiatingPackageName);
            resp.put("updateOwnerPackage", updateOwnerPackageName);
            callbackContext.success(resp);
        } catch(PackageManager.NameNotFoundException err) {
            callbackContext.error("Can't find package " + packageName + ": " + err.getMessage());
        } catch (JSONException err) {
            callbackContext.error("Error writing response: " + err.getMessage());
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
            case "getInfo":
                return handleGetInfo(callbackContext);
            default:
                return false;
        }
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

}
