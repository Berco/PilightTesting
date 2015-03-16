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

package by.zatta.pilight.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import by.zatta.pilight.R;
import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;
import by.zatta.pilight.views.CircularSeekBar;
import by.zatta.pilight.views.CustomHeaderInnerCard;
import it.gmariotti.cardslib.library.extra.staggeredgrid.internal.CardGridStaggeredArrayAdapter;
import it.gmariotti.cardslib.library.extra.staggeredgrid.view.CardGridStaggeredView;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.view.CardListView;

public class DeviceListFragment extends BaseFragment {

	private static final String TAG = "Zatta::DevicelistFragement";
	static DeviceListListener deviceListListener;
	static ArrayList<Card> cards;
	static CardArrayAdapter mCardArrayAdapter;
	static CardGridStaggeredArrayAdapter mCardStaggeredGridArrayAdapter;
	static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	private static String mFilter;
	private static boolean forceList;

	public static DeviceListFragment newInstance(List<DeviceEntry> list, String filter) {
		DeviceListFragment f = new DeviceListFragment();
		Bundle args = new Bundle();
		args.putParcelableArrayList("config", (ArrayList<? extends Parcelable>) list);
		args.putString("filter", filter);
		f.setArguments(args);
		return f;
	}

	public static void updateUI(List<DeviceEntry> list) {
		mDevices = list;
		int i = 0;
		for (DeviceEntry device : mDevices) {
			if (device.getLocationID().equals(mFilter) || mFilter == null) {
				switch (device.getType()){
					case DeviceEntry.DeviceType.SWITCH:
						((ListSwitchCard) cards.get(i)).update(device);
						break;
					case DeviceEntry.DeviceType.DIMMER:
						((ListDimmerCard) cards.get(i)).update(device);
						break;
					case DeviceEntry.DeviceType.WEATHER:
						((ListWeatherCard) cards.get(i)).update(device);
						break;
					case DeviceEntry.DeviceType.RELAY:
						((ListRelayCard) cards.get(i)).update(device);
						break;
					case DeviceEntry.DeviceType.SCREEN:
						((ListScreenCard) cards.get(i)).update(device);
						break;
					case DeviceEntry.DeviceType.CONTACT:
						((ListContactCard) cards.get(i)).update(device);
						break;
					case DeviceEntry.DeviceType.PENDINGSW:
						break;
					case DeviceEntry.DeviceType.DATETIME:
						break;
					case DeviceEntry.DeviceType.XBMC:
						break;
					case DeviceEntry.DeviceType.LIRC:
						break;
					case DeviceEntry.DeviceType.WEBCAM:
						break;
				}
				i++;
			}
		}

		if (forceList) mCardArrayAdapter.notifyDataSetChanged();
		else mCardStaggeredGridArrayAdapter.notifyDataSetChanged();
	}

	@Override
	public int getTitleResourceId() {
		return R.string.title_list_base;
	}

	@Override
	public String getName() {
		return "DeviceList";
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			deviceListListener = (DeviceListListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement DeviceListListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("ZattaPrefs", Context.MODE_MULTI_PROCESS);
		forceList = prefs.getBoolean("forceList", false);
		if (forceList) return inflater.inflate(R.layout.devicelist_layout, container, false);
		else return inflater.inflate(R.layout.devicegrid_layout, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mDevices = getArguments().getParcelableArrayList("config");
		mFilter = getArguments().getString("filter", null);
		initCards();
	}



	private void initCards() {
		Log.v(TAG, "initCards");
		cards = new ArrayList<Card>();
		for (DeviceEntry device : mDevices) {
			Card card = null;
			if (device.hasGroup(mFilter) || mFilter == null) {
				switch (device.getType()){
					case DeviceEntry.DeviceType.SWITCH:
						new ListSwitchCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.DIMMER:
						new ListDimmerCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.WEATHER:
						new ListWeatherCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.RELAY:
						new ListRelayCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.SCREEN:
						new ListScreenCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.CONTACT:
						new ListContactCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.PENDINGSW:
						break;
					case DeviceEntry.DeviceType.DATETIME:
						break;
					case DeviceEntry.DeviceType.XBMC:
						break;
					case DeviceEntry.DeviceType.LIRC:
						break;
					case DeviceEntry.DeviceType.WEBCAM:
						break;
				}
			}
			if (!(card == null)) cards.add(card);
		}

		if (forceList) {
			mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards);
			mCardArrayAdapter.setInnerViewTypeCount(4);

			CardListView listView = (CardListView) getActivity().findViewById(R.id.carddemo_list_base1);
			if (listView != null) {
				listView.setAdapter(mCardArrayAdapter);
			}
		} else {

			mCardStaggeredGridArrayAdapter = new CardGridStaggeredArrayAdapter(getActivity(), cards);
			mCardStaggeredGridArrayAdapter.setInnerViewTypeCount(2);

			CardGridStaggeredView mGridView = (CardGridStaggeredView) getActivity().findViewById(R.id.carddemo_extras_grid_stag);
			if (mGridView != null) {
				mGridView.setAdapter(mCardStaggeredGridArrayAdapter);
			}

		}
	}

	public interface DeviceListListener {
		public void deviceListListener(int what, String action);
	}

	/*
	 * CARDS START FROM HERE: ListDimmerCard ************************************ ******************************************
	 */
	public class ListDimmerCard extends Card {
		protected JSONObject codeJSON = new JSONObject();
		protected boolean mState;
		protected boolean readwrite = true;
		protected int mSeekValue;
		protected int minSeekValue;
		protected int maxSeekValue;
		protected String mTitleDevice;
		protected String mTitleLocation;
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
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
			}
		};
		protected CircularSeekBar.OnCircularSeekBarChangeListener seekListener = new CircularSeekBar.OnCircularSeekBarChangeListener() {
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
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
			}

			@Override
			public void onProgressChanged(CircularSeekBar seekBar, int progress, boolean fromUser) {
				mSeekValue = progress + minSeekValue;
				mToggle.setText(Integer.toString(mSeekValue));
				mValueView.setText(Integer.toString(mSeekValue));
				mToggle.getBackground().setAlpha((int) ((float) (mSeekValue + 1) / (maxSeekValue + 1) * 80) + 70);
			}
		};

		public ListDimmerCard(Context context, DeviceEntry entry) {
			super(context, R.layout.dimmercard_inner);
			try {
				codeJSON.put("device", entry.getNameID());
			} catch (JSONException e) {
				Log.d(TAG, "could not create codeJSON");
			}
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleDevice = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleLocation = sentry.getValue();
				if (sentry.getKey().equals("dimlevel")) mSeekValue = Integer.valueOf(sentry.getValue());
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
				}
				if (sentry.getKey().equals("dimlevel-minimum")) minSeekValue = Integer.valueOf(sentry.getValue());
				if (sentry.getKey().equals("dimlevel-maximum")) maxSeekValue = Integer.valueOf(sentry.getValue());
				if (sentry.getKey().equals("readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			CardHeader header = new CustomHeaderInnerCard(getContext(), mTitleDevice, mTitleLocation);
			addCardHeader(header);
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
			}

			if (mSeekBar != null) {
				mSeekBar.setMax(maxSeekValue - minSeekValue);
				mSeekBar.setProgress(mSeekValue - minSeekValue);
				mSeekBar.setOnSeekBarChangeListener(seekListener);
			}
			mToggle.setClickable(readwrite);
			mSeekBar.setClickable(readwrite);
			if (!readwrite) mToggle.setAlpha((float) 0.5);
			if (!readwrite) mSeekBar.setAlpha((float) 0.5);
		}

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

	/*
	 * LISTSWITCHCARD ****************************************************************************************************
	 */
	public class ListSwitchCard extends Card {
		protected JSONObject codeJSON = new JSONObject();
		protected String mTitleDevice;
		protected String mTitleLocation;
		protected ToggleButton mToggle;
		protected boolean mState;
		protected boolean readwrite = true;
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
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
			}
		};

		public ListSwitchCard(Context context, DeviceEntry entry) {
			super(context, R.layout.switchcard_inner);
			try {
				codeJSON.put("device", entry.getNameID());
			} catch (JSONException e) {
				Log.d(TAG, "could not create codeJSON");
			}

			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleDevice = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleLocation = sentry.getValue();
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
				}
				if (sentry.getKey().equals("readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			CardHeader header = new CustomHeaderInnerCard(getContext(), mTitleDevice, mTitleLocation);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

			if (mToggle != null) {
				mToggle.setChecked(mState);
				mToggle.setOnCheckedChangeListener(toggleListener);
			}
			mToggle.setClickable(readwrite);
			if (!readwrite) mToggle.setAlpha((float) 0.5);

		}

		public void update(DeviceEntry entry) {
			mToggle.setOnCheckedChangeListener(null);
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
					mToggle.setChecked(mState);
				}
			}
			mToggle.setOnCheckedChangeListener(toggleListener);
		}
	}

	/*
	 * LISTWEATHERCARD ****************************************************************************************************
	 */
	public class ListWeatherCard extends Card {

		protected String mTemperature;
		protected String mHumidity;
		protected boolean showBattery = false;
		protected boolean showTemperature = false;
		protected boolean showHumidity = false;
		protected boolean mBattery = false;
		protected String mTitleDevice;
		protected String mTitleLocation;
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

		public ListWeatherCard(Context context, DeviceEntry entry) {
			super(context, R.layout.weathercard_inner);
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleDevice = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleLocation = sentry.getValue();
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
			init();
		}

		private String makeTimeString(String time) throws ParseException {
			DecimalFormat timeDigit = new DecimalFormat("#0000.###");
			timeDigit.setDecimalSeparatorAlwaysShown(false);
			time = time.replace(".", "");
			time = timeDigit.format(Integer.valueOf(time));
			Date date = new SimpleDateFormat("hhmm").parse(time);
			SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getTimeFormat(mContext);
			time = sdf.format(date);
			return time;
		}

		private void init() {
			CardHeader header = new CustomHeaderInnerCard(getContext(), mTitleDevice, mTitleLocation);
			addCardHeader(header);
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

	/*
	 * LISTRELAYCARD *********************************************************** *****************************************
	 */
	public class ListRelayCard extends Card {
		protected JSONObject codeJSON = new JSONObject();
		protected String mTitleDevice;
		protected String mTitleLocation;
		protected ToggleButton mToggle;
		protected boolean mState;
		protected boolean readwrite = true;

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
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
			}
		};

		public ListRelayCard(Context context, DeviceEntry entry) {
			super(context, R.layout.relaycard_inner);
			try {
				codeJSON.put("device", entry.getNameID());
			} catch (JSONException e) {
				Log.d(TAG, "could not create codeJSON");
			}
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleDevice = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleLocation = sentry.getValue();
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
				}
				if (sentry.getKey().equals("readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			CardHeader header = new CustomHeaderInnerCard(getContext(), mTitleDevice, mTitleLocation);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

			if (mToggle != null) {
				mToggle.setChecked(mState);
				mToggle.setOnCheckedChangeListener(toggleListener);
			}
			mToggle.setClickable(readwrite);
			if (!readwrite) mToggle.setAlpha((float) 0.5);
		}

		public void update(DeviceEntry entry) {
			mToggle.setOnCheckedChangeListener(null);
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
					mToggle.setChecked(mState);
				}
			}
			mToggle.setOnCheckedChangeListener(toggleListener);
		}
	}

	/*
	 * LISTSCREENCARD *********************************************************** *****************************************
	 */
	public class ListScreenCard extends Card {
		protected JSONObject codeJSON = new JSONObject();
		protected String mTitleDevice;
		protected String mTitleLocation;
		protected Button mBtnUp;
		protected Button mBtnDown;
		protected boolean readwrite = true;

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
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
			}
		};

		public ListScreenCard(Context context, DeviceEntry entry) {
			super(context, R.layout.screencard_inner);
			try {
				codeJSON.put("device", entry.getNameID());
			} catch (JSONException e) {
				Log.d(TAG, "could not create codeJSON");
			}
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleDevice = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleLocation = sentry.getValue();
				if (sentry.getKey().equals("readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			CardHeader header = new CustomHeaderInnerCard(getContext(), mTitleDevice, mTitleDevice);
			addCardHeader(header);
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

		public void update(DeviceEntry entry) {
		}
	}

	/*
	 * LISTCONTACTCARD *********************************************************** *****************************************
	 */
	public class ListContactCard extends Card {
		protected JSONObject codeJSON = new JSONObject();
		protected String mTitleDevice;
		protected String mTitleLocation;
		protected ToggleButton mToggle;
		protected boolean mState;
		protected boolean readwrite = true;

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
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, actionJSON.toString());
			}
		};

		public ListContactCard(Context context, DeviceEntry entry) {
			super(context, R.layout.contactcard_inner);
			try {
				codeJSON.put("device", entry.getNameID());
			} catch (JSONException e) {
				Log.d(TAG, "could not create codeJSON");
			}
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleDevice = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleLocation = sentry.getValue();
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("opened")) mState = true;
					if (sentry.getValue().equals("closed")) mState = false;
				}
				if (sentry.getKey().equals("readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			CardHeader header = new CustomHeaderInnerCard(getContext(), mTitleDevice, mTitleLocation);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

			if (mToggle != null) {
				mToggle.setChecked(mState);
				mToggle.setOnCheckedChangeListener(toggleListener);
			}
			mToggle.setClickable(readwrite);
			// if (!readwrite) mToggle.setAlpha((float) 0.5); // I don't think we need this for the contacts
		}

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
}
