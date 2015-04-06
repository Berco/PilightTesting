/******************************************************************************************
 *
 * Copyright (C) 2015 Zatta and Lukx
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

package by.zatta.pilight.cards;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;
import by.zatta.pilight.views.CustomHeaderInnerCard;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;

public abstract class DeviceCardAbstract extends Card {
	protected JSONObject codeJSON = new JSONObject();
	protected String mTitleDevice;
	protected boolean readwrite = true;
	protected int mColor;

	public DeviceCardAbstract(Context context, DeviceEntry entry, int innerLayout) {
		super(context, innerLayout);
		try {
			codeJSON.put("device", entry.getNameID());
		} catch (JSONException e) {
			Log.d(TAG, "could not create codeJSON");
		}
		for (SettingEntry sentry : entry.getSettings()) {
			if (sentry.getKey().equals("name")) {
				mTitleDevice = sentry.getValue();
				mColor = stringToColor(sentry.getValue());
			}
			if (sentry.getKey().equals("readonly") && sentry.getValue().equals("1")) readwrite = false;
		}

		CardHeader header = new CustomHeaderInnerCard(getContext(), mTitleDevice, null);
		addCardHeader(header);
	}

	/**
	 * Update the device card according to the values sent by the pilight server
	 *
	 * @param entry the updated device information entry
	 */
	abstract public void update(DeviceEntry entry);

	private static int stringToColor(String inString){
		String date = java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime());
		int i = (inString+inString).hashCode();
		String colorString = "#" +
				//Integer.toHexString(((i>>24)&0xFF))+ //Could use this string for some alpha
				Integer.toHexString(((i>>16)&0xFF))+
				Integer.toHexString(((i>>8)&0xFF))+
				Integer.toHexString((i&0xFF));
		try {
			i = Color.parseColor(colorString);
		} catch (Exception e) {
			i = Color.MAGENTA;
		}

		return i;
	}
}
