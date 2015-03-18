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
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import by.zatta.pilight.R;
import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.fragments.DeviceListFragment;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;
import by.zatta.pilight.views.CircularSeekBar;

public class DimmerCard extends DeviceCardAbstract {

	protected boolean mState;
	protected int mSeekValue;
	protected int minSeekValue;
	protected int maxSeekValue;
	protected CircularSeekBar mSeekBar;
	protected ToggleButton mToggle;
	protected TextView mValueView;

	protected CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			JSONObject actionJSON = null;
			try {
				if (isChecked) codeJSON.put("state", "on");
				else codeJSON.put("state", "off");
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
	protected CircularSeekBar.OnCircularSeekBarChangeListener seekListener = new CircularSeekBar.OnCircularSeekBarChangeListener() {
		@Override
		public void onProgressChanged(CircularSeekBar seekBar, int progress, boolean fromUser) {
			mSeekValue = progress + minSeekValue;
			mToggle.setText(Integer.toString(mSeekValue));
			mValueView.setText(Integer.toString(mSeekValue));
			mToggle.getBackground().setAlpha((int) ((float) (mSeekValue + 1) / (maxSeekValue + 1) * 80) + 70);
		}

		@Override
		public void onStopTrackingTouch(CircularSeekBar seekBar) {
			mValueView.setText("");
			mToggle.setText(Integer.toString(mSeekValue));
			JSONObject actionJSON = null;
			JSONObject valuesJSON = null;
			try {
				valuesJSON = new JSONObject();
				valuesJSON.put("dimlevel", mSeekValue);
				codeJSON.put("state", "on");
				codeJSON.put("values", valuesJSON);
				actionJSON = new JSONObject();
				actionJSON.put("action", "control");
				actionJSON.put("code", codeJSON);
			} catch (JSONException e) {
				Log.d(TAG, "could not create actionJSON");
			}
			mState = true;
			DeviceListFragment.deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
		}
	};

	public DimmerCard(Context context, DeviceEntry entry) {
		super(context, entry, R.layout.dimmercard_inner);
		for (SettingEntry sentry : entry.getSettings()) {
			if (sentry.getKey().equals("dimlevel")) mSeekValue = Integer.valueOf(sentry.getValue());
			if (sentry.getKey().equals("state")) {
				if (sentry.getValue().equals("on")) mState = true;
				if (sentry.getValue().equals("off")) mState = false;
			}
			if (sentry.getKey().equals("dimlevel-minimum")) minSeekValue = Integer.valueOf(sentry.getValue());
			if (sentry.getKey().equals("dimlevel-maximum")) maxSeekValue = Integer.valueOf(sentry.getValue());
		}
	}

	@Override
	public void setupInnerViewElements(ViewGroup parent, View view) {
		// Retrieve elements
		mValueView = (TextView) parent.findViewById(R.id.card_main_inner_simple_title);
		mSeekBar = (CircularSeekBar) parent.findViewById(R.id.circularSeekBar1);
		mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

		if (mValueView != null) mValueView.setText("");

		if (mToggle != null) {
			mToggle.setChecked(mState);
			mToggle.setOnCheckedChangeListener(toggleListener);
			mToggle.setText(Integer.toString(mSeekValue));
			mToggle.getBackground().setAlpha((int) ((float) (mSeekValue + 1) / (maxSeekValue + 1) * 80) + 70);
			mToggle.setClickable(readwrite);
			if (!readwrite) mToggle.setAlpha((float) 0.5);
		}

		if (mSeekBar != null) {
			mSeekBar.setMax(maxSeekValue - minSeekValue);
			mSeekBar.setProgress(mSeekValue - minSeekValue);
			mSeekBar.setOnSeekBarChangeListener(seekListener);
			mSeekBar.setClickable(readwrite);
			if (!readwrite) mSeekBar.setAlpha((float) 0.5);
		}
	}

	@Override
	public void update(DeviceEntry entry) {
		mToggle.setOnCheckedChangeListener(null);
		mSeekBar.setOnSeekBarChangeListener(null);
		for (SettingEntry sentry : entry.getSettings()) {
			if (sentry.getKey().equals("state")) {
				if (sentry.getValue().equals("on")) mState = true;
				if (sentry.getValue().equals("off")) mState = false;
				mToggle.setChecked(mState);
			}
			if (sentry.getKey().equals("dimlevel")) {
				mSeekValue = Integer.valueOf(sentry.getValue());
				this.mSeekBar.setProgress(mSeekValue - minSeekValue);
			}
		}
		mSeekBar.setOnSeekBarChangeListener(seekListener);
		mToggle.setText(Integer.toString(mSeekValue));
		mToggle.setOnCheckedChangeListener(toggleListener);
	}
}