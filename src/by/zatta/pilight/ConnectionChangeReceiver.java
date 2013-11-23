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

public class ConnectionChangeReceiver extends BroadcastReceiver {

    private final static String TAG = "Zatta::ConnectionChangeReceiver";
    boolean isConnectedTo(String t, Context ctx) {
        try {
            WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            
            Log.d(TAG, wifiInfo.getSSID());
            
            if (wifiInfo.getSSID().contains("307"))
                return true;
            if (wifiInfo.getSSID().contains("micheltest"))
                return true;
            if (wifiInfo.getSSID().contains("pride of rotterdam fwd"))
                return true;
        } catch (Exception a) { }
        return false;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received connectivity change");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //hier een string array van maken
        
        String favWifi = prefs.getString("my_wifi", "307");
        if (isConnectedTo(favWifi, context)){
        	Log.d(TAG, "we are connected to home!");
        		context.startService(new Intent(context, ConnectionService.class));
        } else {
        	Log.d(TAG, "not at home anymore :(");
        		context.stopService(new Intent(context, ConnectionService.class));
        }
    }
}
