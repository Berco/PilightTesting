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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import by.zatta.pilight.R;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;

public class WeatherCard extends DeviceCardAbstract {

	private static final String TAG = "Zatta::WeatherCard";
	protected String mTemperature;
	protected String mHumidity;
	protected boolean showBattery = false;
	protected boolean showTemperature = false;
	protected boolean showHumidity = false;
	protected boolean mBattery = false;
	protected String mSunriseTime;
	protected String mSunsetTime;
	protected int decimals;
	protected int gui_decimals;
	protected TextView mTemperatureView;
	protected TextView mHumidityView;
	protected TextView mSunriseView;
	protected TextView mSunsetView;
	protected ImageView mBatteryView;
	protected DecimalFormat digits = new DecimalFormat("#,##0.0");// format to 1 decimal place

	public WeatherCard(Context context, DeviceEntry entry) {
		super(context, entry, R.layout.weathercard_inner);
		for (SettingEntry sentry : entry.getSettings()) {
			if (sentry.getKey().equals("temperature")) mTemperature = sentry.getValue();
			if (sentry.getKey().equals("humidity")) mHumidity = sentry.getValue() + " %";
			if (sentry.getKey().equals("sunrise")) mSunriseTime = sentry.getValue();
			if (sentry.getKey().equals("sunset")) mSunsetTime = sentry.getValue();
			if (sentry.getKey().equals("battery") && (sentry.getValue().equals("1"))) mBattery = true;
			if (sentry.getKey().equals("device-decimals")) decimals = Integer.valueOf(sentry.getValue());
			if (sentry.getKey().equals("decimals")) gui_decimals = Integer.valueOf(sentry.getValue());
			if (sentry.getKey().equals("show-battery") && (sentry.getValue().equals("1"))) showBattery = true;
			if (sentry.getKey().equals("show-temperature") && (sentry.getValue().equals("1"))) showTemperature = true;
			if (sentry.getKey().equals("show-humidity") && (sentry.getValue().equals("1"))) showHumidity = true;
		}

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

		if (mTemperature != null)
			mTemperature = digits.format(Float.valueOf(mTemperature)) + " \u2103";

		if (mSunriseTime != null) {
			try {
				mSunriseTime = makeTimeString(mSunriseTime);
			} catch (ParseException e) {
			}
		}
		if (mSunsetTime != null) {
			try {
				mSunsetTime = makeTimeString(mSunsetTime);
			} catch (ParseException e) {
			}
		}
	}

	private String makeTimeString(String time) throws ParseException {
		DecimalFormat timeDigit = new DecimalFormat("#0000.###");
		timeDigit.setDecimalSeparatorAlwaysShown(false);
		Log.e(TAG, time);
		String[] timeArr = time.split("[^0-9]");
		if (timeArr[0].length() < 2) timeArr[0] = "0" + timeArr[0];
		if (timeArr[1].length() < 2) timeArr[1] = timeArr[1] + "0";
		time = timeArr[0] + timeArr[1];
		//time = time.replace(".", "");
		time = timeDigit.format(Integer.valueOf(time));
		Date date = new SimpleDateFormat("hhmm").parse(time);
		SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getTimeFormat(mContext);
		time = sdf.format(date);
		return time;
	}

	@Override
	public void setupInnerViewElements(ViewGroup parent, View view) {
		// Retrieve elements
		mTemperatureView = (TextView) parent.findViewById(R.id.card_main_inner_temperature);
		mHumidityView = (TextView) parent.findViewById(R.id.card_main_inner_humidity);
		mSunriseView = (TextView) parent.findViewById(R.id.card_main_inner_sunrise);
		mSunsetView = (TextView) parent.findViewById(R.id.card_main_inner_sunset);
		mBatteryView = (ImageView) parent.findViewById(R.id.card_main_inner_battery);

		if (mTemperatureView != null && mTemperature != null && showTemperature) mTemperatureView.setVisibility(View.VISIBLE);
		mTemperatureView.setText(mTemperature);
		if (mHumidityView != null && mHumidity != null && showHumidity) mHumidityView.setVisibility(View.VISIBLE);
		mHumidityView.setText(mHumidity);
		if (mSunriseView != null && mSunriseTime != null) mSunriseView.setVisibility(View.VISIBLE);
		mSunriseView.setText(mSunriseTime);
		if (mSunsetView != null && mSunsetTime != null) mSunsetView.setVisibility(View.VISIBLE);
		mSunsetView.setText(mSunsetTime);

		if (mBatteryView != null && showBattery) {
			mBatteryView.setVisibility(View.VISIBLE);
			if (mBattery) mBatteryView.setImageResource(R.drawable.batt_full);
			else mBatteryView.setImageResource(R.drawable.batt_empty);
		}
	}

	@Override
	public void update(DeviceEntry entry) {
		for (SettingEntry sentry : entry.getSettings()) {
			if (sentry.getKey().equals("temperature")) {
				mTemperature = digits.format(Float.valueOf(sentry.getValue())) + " \u2103";
			}
			if (sentry.getKey().equals("humidity")) {
				mHumidity = sentry.getValue() + " %";
			}
			if (sentry.getKey().equals("battery")) {
				if (sentry.getValue().equals("1")) {
					mBattery = true;
					mBatteryView.setImageResource(R.drawable.batt_full);
				} else {
					mBattery = false;
					mBatteryView.setImageResource(R.drawable.batt_empty);
				}
			}
		}
	}
}