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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Origin {

	@SuppressWarnings("unused")
	private static final String TAG = "Zatta::Origin";

	private static OriginEntry originEntry = new OriginEntry();

	public static OriginEntry getOriginEntry() {
		return originEntry;
	}

	public static OriginEntry getOriginEntry(JSONObject jMessage) {
		parse(jMessage);
		return originEntry;
	}

	public static void parse(JSONObject jMessage) {
		//Log.d(TAG, jMessage.toString());
		String updateDeviceId = null;
		JSONObject devValuesJSON = null;
		//iterate over the VALUES
		try {
			updateDeviceId = jMessage.getJSONArray("devices").getString(0);
			devValuesJSON = jMessage.getJSONObject("values");
			originEntry.setNameID(updateDeviceId);
		} catch (JSONException e) {
			Log.w(TAG, "something wrong with jMessage");
		}

		Iterator<?> vit = devValuesJSON.keys();
		List<SettingEntry> settings = new ArrayList<SettingEntry>();
		while (vit.hasNext()) {
			String vkey = (String) vit.next();
			SettingEntry sentry = new SettingEntry();
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
		originEntry.setSettings(settings);
	}
}
