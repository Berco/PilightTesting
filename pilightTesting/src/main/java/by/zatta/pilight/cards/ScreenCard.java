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
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import by.zatta.pilight.R;
import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.fragments.DeviceListFragment;
import by.zatta.pilight.model.DeviceEntry;

public class ScreenCard extends DeviceCardAbstract {

	protected Button mBtnUp;
	protected Button mBtnDown;

	protected Button.OnClickListener clickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			JSONObject actionJSON = null;
			try {
				switch (v.getId()) {
					case R.id.card_inner_btnUp:
						codeJSON.put("state", "up");
						break;
					case R.id.card_inner_btnDown:
						codeJSON.put("state", "down");
						break;
				}
				actionJSON = new JSONObject();
				actionJSON.put("action", "control");
				actionJSON.put("code", codeJSON);
			} catch (JSONException e) {
				Log.d(TAG, "could not create actionJSON");
			}
			DeviceListFragment.deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
		}
	};

	public ScreenCard(Context context, DeviceEntry entry) {
		super(context, entry, R.layout.screencard_inner);
	}

	@Override
	public void setupInnerViewElements(ViewGroup parent, View view) {
		// Retrieve elements
		mBtnUp = (Button) parent.findViewById(R.id.card_inner_btnUp);
		mBtnDown = (Button) parent.findViewById(R.id.card_inner_btnDown);
		mBtnUp.setOnClickListener(clickListener);
		mBtnDown.setOnClickListener(clickListener);
		mBtnDown.setClickable(readwrite);
		mBtnUp.setClickable(readwrite);
	}

	@Override
	public void update(DeviceEntry entry) {
	}
}