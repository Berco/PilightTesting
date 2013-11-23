/*  Semaphore Manager
 *  
 *   Copyright (c) 2012 Stratos Karafotis (stratosk@semaphore.gr)
 *   
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 */
package by.zatta.pilight;

import by.zatta.pilight.connection.ConnectionService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

    private final static String TAG = "Zatta::StartUpReceiver";
    boolean isConnectedTo(String t, Context ctx) {
        try {
            WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            
            Log.d(TAG, wifiInfo.getSSID());
            
            if (wifiInfo.getSSID().contains(t))
                return true;
        } catch (Exception a) { }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive, boot complete");
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean start_on_boot = prefs.getBoolean("start_on_boot", true);
        String favWifi = prefs.getString("my_wifi", "307");
        if (start_on_boot && isConnectedTo(favWifi, context)) {
        	context.startService(new Intent(context, ConnectionService.class));
        }
    }
}
