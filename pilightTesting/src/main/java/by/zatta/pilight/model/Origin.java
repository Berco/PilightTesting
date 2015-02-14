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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Origin {

	@SuppressWarnings("unused")
	private static final String TAG = "Origin";

	private static OriginEntry originEntry = new OriginEntry();

	public static OriginEntry getOriginEntry() {
		return originEntry;
	}

	public static OriginEntry getOriginEntry(JSONObject jMessage) {
		parse(jMessage);
		return originEntry;
	}

	public static void parse(JSONObject jMessage) {
		// Log.v(TAG, jMessage.toString());

		Iterator<?> fit = jMessage.keys();
		/* Iterate through all entries */
		while (fit.hasNext()) {
			String firstKey = (String) fit.next();
			try {
				JSONObject jSecond = jMessage.getJSONObject(firstKey);
				// Log.v(TAG, "firstKey: " + firstKey); //devices, values
				// Log.v(TAG, "jSecond: "+ jSecond.toString()); // {"living":["KamerTemp"]} & {"temperature":"20062"}

				if (firstKey.equals("devices")) {
					Iterator<?> sit = jSecond.keys();
					while (sit.hasNext()) {
						String secondKey = (String) sit.next();
						JSONArray jSecArr = jSecond.optJSONArray(secondKey);
						String jSecStr = jSecond.optString(secondKey);

						if (jSecArr != null) {
							/* Iterate through all values for this setting */
							for (Short i = 0; i < jSecArr.length(); i++) {
								// Log.v(TAG, "Uit array:" + jSecArr.get(i).toString());
								originEntry.setNameID(jSecArr.get(i).toString());
							}
						} else if (jSecStr != null) {
							// Log.v(TAG, "Uit string: " + jSecStr);
							originEntry.setNameID(jSecStr);
						}
					}
				}
				if (firstKey.equals("values")) {
					Iterator<?> sit = jSecond.keys();
					List<SettingEntry> settings = new ArrayList<SettingEntry>();

					while (sit.hasNext()) {
						String secondKey = (String) sit.next();
						JSONArray jSecArr = jSecond.optJSONArray(secondKey);
						String jSecStr = jSecond.optString(secondKey);
						Double jSecDbl = jSecond.optDouble(secondKey);
						Long jSecLng = jSecond.optLong(secondKey);
						
						
						SettingEntry sentry = new SettingEntry();
						if (jSecArr != null) {
							/* Iterate through all values for this setting */
							for (Short i = 0; i < jSecArr.length(); i++) {
								// Log.v(TAG, "Uit array:" + jSecArr.get(i).toString());
								sentry.setKey(secondKey);
								sentry.setValue(jSecArr.get(i).toString());
							}
						} else if (jSecStr != null) {
							// Log.v(TAG, "Uit string: " + jSecStr);
							sentry.setKey(secondKey);
							sentry.setValue(jSecStr);
						}else if (jSecDbl != null) {
							// Log.v(TAG, "Uit string: " + jSecStr);
							sentry.setKey(secondKey);
							sentry.setValue(Double.toString(jSecDbl));
						}else if (jSecLng != null) {
							// Log.v(TAG, "Uit string: " + jSecStr);
							sentry.setKey(secondKey);
							sentry.setValue(Long.toString(jSecLng));
						}
						settings.add(sentry);
					}
					originEntry.setSettings(settings);
				}
			} catch (JSONException e) {
			}
		}
	}
}
