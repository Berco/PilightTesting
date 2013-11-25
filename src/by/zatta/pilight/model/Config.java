package by.zatta.pilight.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Config {
	
	private static final String TAG = "Config";
	
	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	
	public static List<DeviceEntry> getDevices() {
		return mDevices;
	}
	
	public static List<DeviceEntry> getDevices(JSONObject jloc) {
		parse(jloc);
		return mDevices;
	}
	
	public static void parse(JSONObject jloc) {

		Iterator<?> lit = jloc.keys();
		/* Iterate through all locations */
		while(lit.hasNext()) {

			String locationID = (String)lit.next();
			String locationName = "";

			try {				
				JSONObject jdev = jloc.getJSONObject(locationID);
				if (jdev.has("name")) locationName = jdev.getString("name");
				Log.e(TAG, locationID + " = " +locationName);
				Iterator<?> dit = jdev.keys();

				/* Iterate through all devices of this location */
				while(dit.hasNext()) {
					String dkey = (String)dit.next();
					Log.d("dkey", dkey);
					if(!dkey.equals("name")) {

						try {

							/* Create new device object for this location */
							DeviceEntry device = new DeviceEntry();
							device.setNameID(dkey);
							device.setLocationID(locationID);
							
							List<SettingEntry> settings = new ArrayList<SettingEntry>();
							SettingEntry sentry = new SettingEntry();
							sentry.setKey("locationName");
							sentry.setValue(locationName);
							settings.add(sentry);
														
							JSONObject jset = jdev.getJSONObject(dkey);
							Iterator<?> sit = jset.keys();
							
							/* Iterate through all settings of this device */							
							while(sit.hasNext()) {
								String skey = (String)sit.next();
								
								if(skey.equals("type")) {
									device.setType(Integer.valueOf(jset.getString(skey)));
								} else {

									try {
										sentry = new SettingEntry();
										sentry.setKey(skey);
										/* Create new settings array for this device */
										
										JSONArray jvalarr = jset.optJSONArray(skey);
										String jvalstr = jset.optString(skey);
										Double jvaldbl = jset.optDouble(skey);
										Long jvallng = jset.optLong(skey);
										
										if(jvalarr != null) {
											/* Iterate through all values for this setting */
											for(Short i=0; i<jvalarr.length(); i++) {
												sentry.setKey(skey);
												sentry.setValue(jvalarr.get(i).toString());
											}
										} else if(jvalstr != null) {
											//Log.e(TAG, skey + "  string : " + jvalstr);
											//sentry.setKey(skey);
											sentry.setValue(jvalstr.toString());
										} else if(jvaldbl != null) {
											Log.e(TAG, skey + "double : " + jvaldbl.toString());
											//sentry.setKey(skey);
											sentry.setValue(jvaldbl.toString());
										} else if(jvallng != null) {
											Log.e(TAG, skey + "long : " + jvallng.toString());
											//sentry.setKey(skey);
											sentry.setValue(jvallng.toString());
										}							

										if (sentry != null) settings.add(sentry);
									} catch (JSONException e) {
							            Log.w(TAG, "The received SETTING is of an incorrent format");
									}
								}
							}
					
							device.setSettings(settings);
							mDevices.add(device);
							
						} catch (JSONException e) {
				            Log.w(TAG, "The received DEVICE is of an incorrent format");
				        }

					}
				}

			} catch (JSONException e) {
	            Log.w(TAG, "The received LOCATION is of an incorrent format");
	        }
		}
		try {
			print();
		} catch (Exception e) {
			Log.w(TAG, "4) couldnt print");
		}
	}
	
	public static void print() {
		System.out.println("________________");
		for (DeviceEntry device : mDevices){
			 System.out.println("-"+device.getNameID());
			 System.out.println("-"+device.getLocationID());
			 System.out.println("-"+device.getType());
				for(SettingEntry sentry : device.getSettings()) {
					System.out.println("*"+sentry.getKey()+" = " + sentry.getValue());
				}
			System.out.println("________________");
		 }
	}
}
