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
 * with pilight for android.
 * If not, see <http://www.gnu.org/licenses/>
 *
 * Copyright (c) 2013 pilight project
 ********************************************************************************************/

package by.zatta.pilight.cards;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import by.zatta.pilight.R;
import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.fragments.DeviceListFragment;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;

public class ContactCard extends DeviceCardAbstract {

	protected ToggleButton mToggle;
	protected boolean mState;

	protected CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			JSONObject actionJSON = null;
			try {
				if (isChecked) codeJSON.put("state", "opened");
				else codeJSON.put("state", "closed");
				actionJSON = new JSONObject();
				actionJSON.put("action", "control");
				actionJSON.put("code", codeJSON);
			} catch (JSONException e) {
				Log.d(TAG, "could not create actionJSON");
			}
			mState = isChecked;
			DeviceListFragment.deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
		}
	};

	public ContactCard(Context context, DeviceEntry entry) {
		super(context, entry, R.layout.contactcard_inner);

		for (SettingEntry sentry : entry.getSettings()) {
			if (sentry.getKey().equals("state")) {
				if (sentry.getValue().equals("opened")) mState = true;
				if (sentry.getValue().equals("closed")) mState = false;
			}
		}
	}

	@Override
	public void setupInnerViewElements(ViewGroup parent, View view) {
		// Retrieve elements
		mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

		if (mToggle != null) {
			mToggle.setChecked(mState);
			mToggle.setOnCheckedChangeListener(toggleListener);
			mToggle.setClickable(readwrite);
			// if (!readwrite) mToggle.setAlpha((float) 0.5); // I don't think we need this for the contacts
		}
	}

	@Override
	public void update(DeviceEntry entry) {
		mToggle.setOnCheckedChangeListener(null);
		for (SettingEntry sentry : entry.getSettings()) {
			if (sentry.getKey().equals("state")) {
				if (sentry.getValue().equals("opened")) mState = true;
				if (sentry.getValue().equals("closed")) mState = false;
				mToggle.setChecked(mState);
			}
		}
		mToggle.setOnCheckedChangeListener(toggleListener);
	}
}