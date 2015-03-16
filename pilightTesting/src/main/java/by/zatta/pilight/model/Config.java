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

	private static final String TAG = "Zatta::Config";
	private static String lastUpdateString;

	private static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();

	public static List<DeviceEntry> getDevices(JSONObject jloc) {
		mDevices.clear();
		parse(jloc);
		contactWorkaround();
		return mDevices;
	}

	private static void contactWorkaround() {
		for (DeviceEntry device : mDevices) {
			for (SettingEntry sentry : device.getSettings()) {
				if (sentry.getValue().equals("opened") ||
						sentry.getValue().equals("closed")) {
					device.setType(6);
				}
			}
		}
	}

	public static void parse(JSONObject collectJSON) {
		JSONObject guiJSON = null;
		JSONArray valuesJSON = null;

		try {
			guiJSON = collectJSON.getJSONObject("gui");
			valuesJSON = collectJSON.getJSONArray("values");
			/* Iterate through the GUI  */
			Iterator<?> dit = guiJSON.keys();
			while (dit.hasNext()) {
				String dkey = (String) dit.next();
				try {
					DeviceEntry device = new DeviceEntry();
					device.setNameID(dkey);
					List<SettingEntry> settings = new ArrayList<SettingEntry>();
					SettingEntry sentry = new SettingEntry();
					JSONObject jset = guiJSON.getJSONObject(dkey);
					Iterator<?> sit = jset.keys();
					/* Iterate through all settings of this device */
					while (sit.hasNext()) {
						String skey = (String) sit.next();

						if (skey.equals("type")) {
							device.setType(Integer.valueOf(jset.getString(skey)));
						}else if (skey.equals("order")) {
								device.setOrder(Integer.valueOf(jset.getString(skey)));
						} else {
							try {

								JSONArray jvalarr = jset.optJSONArray(skey);
								String jvalstr = jset.optString(skey);
								Double jvaldbl = jset.optDouble(skey);
								Long jvallng = jset.optLong(skey);

								if (jvalarr != null) {
									for (Short i = 0; i < jvalarr.length(); i++) {
										sentry = new SettingEntry();
										sentry.setKey(skey);
										sentry.setValue(jvalarr.get(i).toString());
										if (sentry != null) settings.add(sentry);
									}
								} else if (jvalstr != null) {
									sentry = new SettingEntry();
									sentry.setKey(skey);
									sentry.setValue(jvalstr.toString());
									if (sentry != null) settings.add(sentry);
								} else if (jvaldbl != null) {
									sentry = new SettingEntry();
									sentry.setKey(skey);
									sentry.setValue(jvaldbl.toString());
									if (sentry != null) settings.add(sentry);
								} else if (jvallng != null) {
									sentry = new SettingEntry();
									sentry.setKey(skey);
									sentry.setValue(jvallng.toString());
									if (sentry != null) settings.add(sentry);
								}
							} catch (JSONException e) {
								Log.w(TAG, "The received SETTING is of an incorrent format");
							}

						}
					}
					//iterate over the VALUES
					for (int i = 0; i < valuesJSON.length(); i++) {
						JSONObject row = valuesJSON.getJSONObject(i);
						String isThisIt = row.getJSONArray("devices").toString();
						if (isThisIt.contains(device.getNameID())) {

							JSONObject devValuesJSON = row.getJSONObject("values");
							Iterator<?> vit = devValuesJSON.keys();

							while (vit.hasNext()) {
								String vkey = (String) vit.next();
								try {

									JSONArray jvalarr = devValuesJSON.optJSONArray(vkey);
									String jvalstr = devValuesJSON.optString(vkey);
									Double jvaldbl = devValuesJSON.optDouble(vkey);
									Long jvallng = devValuesJSON.optLong(vkey);

									if (jvalarr != null) {
										for (Short l = 0; l < jvalarr.length(); l++) {
											sentry = new SettingEntry(); // WHY DO I NEED THIS ONE??
											sentry.setKey(vkey);
											sentry.setValue(jvalarr.get(l).toString());
											if (sentry != null) settings.add(sentry);
										}
									} else if (jvalstr != null) {
										sentry = new SettingEntry();
										sentry.setKey(vkey);
										sentry.setValue(jvalstr.toString());
										if (sentry != null) settings.add(sentry);
									} else if (jvaldbl != null) {
										sentry = new SettingEntry();
										sentry.setKey(vkey);
										sentry.setValue(jvaldbl.toString());
										if (sentry != null) settings.add(sentry);
									} else if (jvallng != null) {
										sentry = new SettingEntry();
										sentry.setKey(vkey);
										sentry.setValue(jvallng.toString());
										if (sentry != null) settings.add(sentry);
									}
								} catch (JSONException e) {
									Log.w(TAG, "The received VALUE is of an incorrent format");
								}
							}
						}
					}
					device.setSettings(settings);
					mDevices.add(device);
				} catch (JSONException e) {
					Log.w(TAG, "The received DEVICE is of an incorrent format");
				}
			}

			try {
				print();
			} catch (Exception e) {
				Log.w(TAG, "couldnt print devices");
			}
		} catch (JSONException e) {
			Log.w(TAG, "The received collectJSON is of an incorrent format");
		}
	}

	public static void print() {
		Log.d(TAG, "________________");
		for (DeviceEntry device : mDevices) {
			Log.d(TAG, "-" + device.getNameID());
			Log.d(TAG, "-" + device.getType());
			Log.d(TAG, "-" + device.getOrder());
			for (SettingEntry sentry : device.getSettings()) {
				Log.d(TAG, "*" + sentry.getKey() + " = " + sentry.getValue());
			}
			Log.d(TAG, "________________");
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
					if (sentry.getKey().equals("decimals")) {
						gui_decimals = Integer.valueOf(sentry.getValue());
						switch (gui_decimals) {
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
							if (sentry.getKey().equals("temperature") && (gui_decimals != -1)) {
								String temp = digits.format(Float.valueOf(sentry.getValue()))+ " \u2103";
								if (!value.contains("Temp:"))
									value = value + "Temp: " + temp + "\n";
							} else if (sentry.getKey().equals("humidity")) {
								String hum = sentry.getValue() + " %";
								if (!value.contains("Humidity:"))
									value = value + "Humidity: " + hum + "\n";
							} else if (sentry.getKey().equals("timestamp")) {
								if (!value.contains("Stamp: "))
									value = value + "Stamp: " + new SimpleDateFormat("HH:mm:ss").format(Long.valueOf(sentry.getValue()) * 1000) + "\n";
							} else {
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
