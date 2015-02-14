/******************************************************************************************
 * 
 * Copyright (C) 2013 Zatta
 * 
 * This file is part of pilight for android.
 * 
 * pilight for android is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by the 
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * pilight for android is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with pilightfor android.
 * If not, see <http://www.gnu.org/licenses/>
 * 
 * Copyright (c) 2013 pilight project
 ********************************************************************************************/

package by.zatta.pilight.model;

import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Config {

	private static final String TAG = "Config";
	private static String lastUpdateString;

	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();

	public static List<DeviceEntry> getDevices(JSONObject jloc) {
		mDevices.clear();
		parse(jloc);
		contactWorkaround();
		return mDevices;
	}
	
	private static void contactWorkaround(){
		for (DeviceEntry device : mDevices) {
				for (SettingEntry sentry : device.getSettings()) {
					if (sentry.getValue().equals("opened") ||
							sentry.getValue().equals("closed")	) {
						device.setType(6);
					}
				}
		}
	}

	public static void parse(JSONObject jloc) {
		Iterator<?> lit = jloc.keys();
		/* Iterate through all locations */
		while (lit.hasNext()) {

			String locationID = (String) lit.next();
			String locationName = "";

			try {
				JSONObject jdev = jloc.getJSONObject(locationID);
				if (jdev.has("name")) locationName = jdev.getString("name");
				// Log.v(TAG, locationID + " = " +locationName);
				Iterator<?> dit = jdev.keys();

				/* Iterate through all devices of this location */
				while (dit.hasNext()) {
					String dkey = (String) dit.next();
					// Log.v("dkey", dkey);
					if (!dkey.equals("name")) {

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
							while (sit.hasNext()) {
								String skey = (String) sit.next();

								if (skey.equals("type")) {
									device.setType(Integer.valueOf(jset.getString(skey)));
								}
								/* OBSOLETE LOOP SINCE PILIGHT 4.0 */
//								} else if (skey.equals("settings")) {
//									// iterate over all specific settings
//									JSONObject jthi = jset.getJSONObject(skey);
//									Iterator<?> thit = jthi.keys();
//									while (thit.hasNext()) {
//										String thkey = (String) thit.next();
//										sentry = new SettingEntry();
//										sentry.setKey("sett_" + thkey);
//
//										JSONArray jvalarr = jthi.optJSONArray(thkey);
//										String jvalstr = jthi.optString(thkey);
//										Double jvaldbl = jthi.optDouble(thkey);
//										Long jvallng = jthi.optLong(thkey);
//
//										if (jvalarr != null) {
//											for (Short i = 0; i < jvalarr.length(); i++) {
//												sentry.setKey(thkey);
//												sentry.setValue(jvalarr.get(i).toString());
//											}
//										} else if (jvalstr != null) {
//											sentry.setValue(jvalstr.toString());
//										} else if (jvaldbl != null) {
//											Log.e(TAG, skey + "double : " + jvaldbl.toString());
//											sentry.setValue(jvaldbl.toString());
//										} else if (jvallng != null) {
//											Log.e(TAG, skey + "long : " + jvallng.toString());
//											sentry.setValue(jvallng.toString());
//										}
//										if (sentry != null) settings.add(sentry);
//									}
//								}
								else if (skey.equals("id") || skey.equals("protocol") || skey.equals("order")) {
								} else {
									try {
										sentry = new SettingEntry();
										sentry.setKey(skey);
										JSONArray jvalarr = jset.optJSONArray(skey);
										String jvalstr = jset.optString(skey);
										Double jvaldbl = jset.optDouble(skey);
										Long jvallng = jset.optLong(skey);

										if (jvalarr != null) {
											for (Short i = 0; i < jvalarr.length(); i++) {
												sentry.setKey(skey);
												sentry.setValue(jvalarr.get(i).toString());
											}
										} else if (jvalstr != null) {
											sentry.setValue(jvalstr.toString());
										} else if (jvaldbl != null) {
											Log.e(TAG, skey + "double : " + jvaldbl.toString());
											sentry.setValue(jvaldbl.toString());
										} else if (jvallng != null) {
											Log.e(TAG, skey + "long : " + jvallng.toString());
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
		for (DeviceEntry device : mDevices) {
			System.out.println("-" + device.getNameID());
			System.out.println("-" + device.getLocationID());
			System.out.println("-" + device.getType());
			for (SettingEntry sentry : device.getSettings()) {
				System.out.println("*" + sentry.getKey() + " = " + sentry.getValue());
			}
			System.out.println("________________");
		}
	}

	@SuppressLint("SimpleDateFormat")
	public static List<DeviceEntry> update(OriginEntry originEntry) {
		lastUpdateString = "";
		int decimals = -1;
		int gui_decimals = -1;
		String name = "";
		String value = "";
		DecimalFormat digits = new DecimalFormat("#,##0.0");// format to 1 decimal place
		for (DeviceEntry device : mDevices) {
			if (device.getNameID().equals(originEntry.getNameID())) {
				// Log.v(TAG, "updating: " + device.getNameID());
				for (SettingEntry sentry : device.getSettings()) {
					// Log.v(TAG, sentry.getKey());
					if (sentry.getKey().equals("name")) {
						originEntry.setPopularName(sentry.getValue());
						name = sentry.getValue();
					}
					if (sentry.getKey().equals("device-decimals")) {
						decimals = Integer.valueOf(sentry.getValue());
						// Log.v(TAG, sentry.getValue());
					}
					if (sentry.getKey().equals("gui-decimals")) {
						gui_decimals = Integer.valueOf(sentry.getValue());
						switch (gui_decimals){
						case 1:
							digits = new DecimalFormat("#,##0.0");// format to 1 decimal place
							break;
						case 2:
							digits = new DecimalFormat("#,##0.00");// format to 1 decimal place
							break;
						case 3:
							digits = new DecimalFormat("#,##0.000");// format to 1 decimal place
							break;
						case 4:
							digits = new DecimalFormat("#,##0.0000");// format to 1 decimal place
							break;
						default:
							digits = new DecimalFormat("#,##0.0");// format to 1 decimal place
							break;
						}
					}
				}
				for (SettingEntry sentry : device.getSettings()) {
					for (SettingEntry orSentry : originEntry.getSettings()) {
						if (sentry.getKey().equals(orSentry.getKey())) {
							sentry.setValue(orSentry.getValue());

							if (sentry.getKey().equals("temperature") && (decimals != -1)) {
								String temp = digits.format(Integer.valueOf(sentry.getValue()) / (Math.pow(10, decimals)))
										+ " \u2103";
                                if (!value.contains("Temp:"))
								    value = value + "Temp: " + temp + "\n";
							} else if (sentry.getKey().equals("humidity") && (decimals != -1)) {
								String hum = digits.format(Integer.valueOf(sentry.getValue()) / (Math.pow(10, decimals)))
										+ " %";
                                if (!value.contains("Humidity:"))
								    value = value + "Humidity: " + hum + "\n";
							} else if (sentry.getKey().equals("timestamp")){
                                if (!value.contains("Stamp: "))
								value = value + "Stamp: " + new SimpleDateFormat("HH:mm:ss").format(Long.valueOf(sentry.getValue())*1000) + "\n";
							}

							else {
								char firstChar = orSentry.getKey().charAt(0);
								char replaceBy = Character.toUpperCase(firstChar);
								String what = orSentry.getKey().replaceFirst(Character.toString(firstChar), Character.toString(replaceBy));
                                if (!value.contains(what))
								    value = value + what + ": " + orSentry.getValue() + "\n";
							}
						}
					}
				}
			}
		}
		lastUpdateString = name + "\n" + value;
		return mDevices;
	}

	public static String getLastUpdateString() {
		// Log.v(TAG, lastUpdateString);
		return lastUpdateString;
	}
}
