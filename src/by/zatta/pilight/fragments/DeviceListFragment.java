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
import android.os.Bundle;
import android.os.Parcelable;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.devadvance.circularseekbar.CircularSeekBar;
import by.zatta.pilight.R;
import by.zatta.pilight.connection.ConnectionService;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.view.CardListView;

public class DeviceListFragment extends BaseFragment {

	static DeviceListListener deviceListListener;
	protected ScrollView mScrollView;
	static ArrayList<Card> cards;
	private static final String TAG = "ListBase";
	static CardArrayAdapter mCardArrayAdapter;
	static CardGridArrayAdapter mCardGridArrayAdapter;
	static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();
	public final int GIMME_DEVICES = 1002;
	private static String mFilter;

	public static DeviceListFragment newInstance(List<DeviceEntry> list, String filter) {
		DeviceListFragment f = new DeviceListFragment();
		Bundle args = new Bundle();
		args.putParcelableArrayList("config", (ArrayList<? extends Parcelable>) list);
		args.putString("filter", filter);
		f.setArguments(args);
		return f;
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
		mDevices = getArguments().getParcelableArrayList("config");
		mFilter = getArguments().getString("filter", null);
		setRetainInstance(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.devicelist_layout, container, false);
		// return inflater.inflate(R.layout.demo_fragment_grid_base, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initCards();
	}

	private void initCards() {
		Log.v(TAG, "initCards");
		cards = new ArrayList<Card>();
		for (DeviceEntry device : mDevices) {
			Card card = null;
			if (device.getLocationID().equals(mFilter) || mFilter == null) {
				if (device.getType() == 1)
					card = new ListSwitchCard(getActivity().getApplicationContext(), device);
				else if (device.getType() == 2)
					card = new ListDimmerCard(getActivity().getApplicationContext(), device);
				else if (device.getType() == 3)
					card = new ListWeatherCard(getActivity().getApplicationContext(), device);
				else if (device.getType() == 4)
					card = new ListRelayCard(getActivity().getApplicationContext(), device);
				else if (device.getType() == 5) card = new ListScreenCard(getActivity().getApplicationContext(), device);
			}
			if (!(card == null)) cards.add(card);
		}

		mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards);
		mCardArrayAdapter.setInnerViewTypeCount(4);

		CardListView listView = (CardListView) getActivity().findViewById(R.id.carddemo_list_base1);
		if (listView != null) {
			listView.setAdapter(mCardArrayAdapter);
		}

		// mCardGridArrayAdapter = new CardGridArrayAdapter(getActivity(), cards);
		// mCardGridArrayAdapter.setInnerViewTypeCount(2);
		//
		// CardGridView gridView = (CardGridView) getActivity().findViewById(R.id.carddemo_grid_base);
		// if (gridView!=null){
		// gridView.setAdapter(mCardGridArrayAdapter);
		// }
	}

	public static void updateUI(List<DeviceEntry> list) {
		mDevices = list;
		int i = 0;
		for (DeviceEntry device : mDevices) {
			if (device.getLocationID().equals(mFilter) || mFilter == null) {
				if (device.getType() == 1) {
					((ListSwitchCard) cards.get(i)).update(device);
				} else if (device.getType() == 2) {
					((ListDimmerCard) cards.get(i)).update(device);
				} else if (device.getType() == 3) {
					((ListWeatherCard) cards.get(i)).update(device);
				} else if (device.getType() == 4) {
					((ListRelayCard) cards.get(i)).update(device);
				} else if (device.getType() == 5) {
					((ListScreenCard) cards.get(i)).update(device);
				}
				i++;
			}
		}
		mCardArrayAdapter.notifyDataSetChanged();
		// mCardGridArrayAdapter.notifyDataSetChanged();
	}

	public interface DeviceListListener {
		public void deviceListListener(int what, String action);
	}

	/*
	 * CARDS START FROM HERE: ListDimmerCard ************************************ ******************************************
	 */
	public class ListDimmerCard extends Card {
		protected String who;
		protected boolean mState;
		protected boolean readwrite = true;
		protected int mSeekValue;
		protected int minSeekValue;
		protected int maxSeekValue;
		protected String mTitleHeader;
		protected String mTitleMain;
		protected CircularSeekBar mSeekBar;
		protected ToggleButton mToggle;
		protected TextView mTitleMainView;
		protected CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				String action = "\"state\":\"off\"";
				if (isChecked) action = "\"state\":\"on\"";
				mState = isChecked;
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, who + action);
			}
		};
		protected CircularSeekBar.OnCircularSeekBarChangeListener seekListener = new CircularSeekBar.OnCircularSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(CircularSeekBar seekBar) {
				mTitleMainView.setText(mTitleMain);
				mToggle.setText(Integer.toString(mSeekValue));
				String action = "\"state\":\"on\",\"values\":{\"dimlevel\":" + String.valueOf(mSeekValue) + "}";
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, who + action);
			}

			@Override
			public void onProgressChanged(CircularSeekBar seekBar, int progress, boolean fromUser) {
				mSeekValue = progress + minSeekValue;
				mToggle.setText(Integer.toString(mSeekValue));
				mTitleMainView.setText("Dimmer setting: " + Integer.toString(mSeekValue));
				mToggle.getBackground().setAlpha((int) ((float) (mSeekValue + 1) / (maxSeekValue + 1) * 80) + 70);
			}
		};

		public ListDimmerCard(Context context, DeviceEntry entry) {
			super(context, R.layout.dimmercard_inner);
			who = "\"device\":\"" + entry.getNameID() + "\",\"location\":\"" + entry.getLocationID() + "\",";
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleHeader = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleMain = sentry.getValue();
				if (sentry.getKey().equals("dimlevel")) mSeekValue = Integer.valueOf(sentry.getValue());
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
				}
				if (sentry.getKey().equals("sett_min")) minSeekValue = Integer.valueOf(sentry.getValue());
				if (sentry.getKey().equals("sett_max")) maxSeekValue = Integer.valueOf(sentry.getValue());
				if (sentry.getKey().equals("sett_readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			setTitle(mTitleMain);
			// Create a CardHeader
			CardHeader header = new CardHeader(getContext());
			header.setTitle(mTitleHeader);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mTitleMainView = (TextView) parent.findViewById(R.id.card_main_inner_simple_title);
			mSeekBar = (CircularSeekBar) parent.findViewById(R.id.circularSeekBar1);
			mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

			if (mTitleMainView != null) mTitleMainView.setText(mTitleMain);

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
	 * LISTSWITCHCARD *********************************************************** *****************************************
	 */
	public class ListSwitchCard extends Card {
		protected String who;
		protected String mValue;
		protected String mTitleHeader;
		protected String mTitleMain;
		protected TextView mTitleMainView;
		protected ToggleButton mToggle;
		protected boolean mState;
		protected boolean readwrite = true;
		protected CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				String action = "\"state\":\"off\"";
				if (isChecked) action = "\"state\":\"on\"";
				mState = isChecked;
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, who + action);
			}
		};

		public ListSwitchCard(Context context, DeviceEntry entry) {
			super(context, R.layout.switchcard_inner);
			who = "\"device\":\"" + entry.getNameID() + "\",\"location\":\"" + entry.getLocationID() + "\",";
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleHeader = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleMain = sentry.getValue();
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
				}
				if (sentry.getKey().equals("sett_readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			setTitle(mTitleMain);
			// Create a CardHeader
			CardHeader header = new CardHeader(getContext());
			header.setTitle(mTitleHeader);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mTitleMainView = (TextView) parent.findViewById(R.id.card_main_inner_simple_title);
			mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

			if (mTitleMainView != null) mTitleMainView.setText(mTitleMain);
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
	 * LISTWEATHERCARD ********************************************************** ******************************************
	 */
	public class ListWeatherCard extends Card {

		protected String mTemperature;
		protected String mHumidity;
		protected boolean showBattery = false;
		protected boolean mBattery = false;
		protected String mTitleHeader;
		protected String mTitleMain;
		protected int decimals;
		protected TextView mTemperatureView;
		protected TextView mHumidityView;
		protected ImageView mBatteryView;
		protected TextView mTitleMainView;

		public ListWeatherCard(Context context, DeviceEntry entry) {
			super(context, R.layout.weathercard_inner);
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleHeader = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleMain = sentry.getValue();
				if (sentry.getKey().equals("temperature")) mTemperature = sentry.getValue();
				if (sentry.getKey().equals("humidity")) mHumidity = sentry.getValue();
				if (sentry.getKey().equals("battery") && (sentry.getValue().equals("1"))) mBattery = true;
				if (sentry.getKey().equals("sett_decimals")) decimals = Integer.valueOf(sentry.getValue());
				if (sentry.getKey().equals("sett_battery") && (sentry.getValue().equals("1"))) showBattery = true;
			}

			DecimalFormat oneDigit = new DecimalFormat("#,##0.0");// format to 1
																	// decimal
																	// place
			if (mTemperature != null)
				mTemperature = oneDigit.format(Integer.valueOf(mTemperature) / (Math.pow(10, decimals))) + " \u2103";
			if (mHumidity != null) mHumidity = oneDigit.format(Integer.valueOf(mHumidity) / (Math.pow(10, decimals))) + " %";

			init();
		}

		private void init() {
			setTitle(mTitleMain);
			// Create a CardHeader
			CardHeader header = new CardHeader(getContext());
			header.setTitle(mTitleHeader);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mTitleMainView = (TextView) parent.findViewById(R.id.card_main_inner_simple_title);
			mTemperatureView = (TextView) parent.findViewById(R.id.card_main_inner_temperature);
			mHumidityView = (TextView) parent.findViewById(R.id.card_main_inner_humidity);
			mBatteryView = (ImageView) parent.findViewById(R.id.card_main_inner_battery);

			if (mTitleMainView != null) mTitleMainView.setText(mTitleMain);
			if (mTemperatureView != null && mTemperature != null) mTemperatureView.setVisibility(View.VISIBLE);
			mTemperatureView.setText(mTemperature);
			if (mHumidityView != null && mHumidity != null) mHumidityView.setVisibility(View.VISIBLE);
			mHumidityView.setText(mHumidity);
			if (mBatteryView != null && showBattery) {
				mBatteryView.setVisibility(View.VISIBLE);
				if (mBattery)
					mBatteryView.setImageResource(R.drawable.batt_full);
				else mBatteryView.setImageResource(R.drawable.batt_empty);
			}
		}

		public void update(DeviceEntry entry) {
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("temperature")) {
					DecimalFormat oneDigit = new DecimalFormat("#,##0.0");// format to 1 decimal place
					mTemperature = oneDigit.format(Integer.valueOf(sentry.getValue()) / (Math.pow(10, decimals))) + " \u2103";
				}
				if (sentry.getKey().equals("humidity")) {
					DecimalFormat oneDigit = new DecimalFormat("#,##0.0");// format to 1 decimal place
					mHumidity = oneDigit.format(Integer.valueOf(sentry.getValue()) / (Math.pow(10, decimals))) + " %";
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
		protected String who;
		protected String mValue;
		protected String mTitleHeader;
		protected String mTitleMain;
		protected TextView mTitleMainView;
		protected ToggleButton mToggle;
		protected boolean mState;
		protected boolean readwrite = true;

		protected CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				String action = "\"state\":\"off\"";
				if (isChecked) action = "\"state\":\"on\"";
				mState = isChecked;
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, who + action);
			}
		};

		public ListRelayCard(Context context, DeviceEntry entry) {
			super(context, R.layout.relaycard_inner);
			who = "\"device\":\"" + entry.getNameID() + "\",\"location\":\"" + entry.getLocationID() + "\",";
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleHeader = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleMain = sentry.getValue();
				if (sentry.getKey().equals("state")) {
					if (sentry.getValue().equals("on")) mState = true;
					if (sentry.getValue().equals("off")) mState = false;
				}
				if (sentry.getKey().equals("sett_readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			setTitle(mTitleMain);
			// Create a CardHeader
			CardHeader header = new CardHeader(getContext());
			header.setTitle(mTitleHeader);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mTitleMainView = (TextView) parent.findViewById(R.id.card_main_inner_simple_title);
			mToggle = (ToggleButton) parent.findViewById(R.id.card_inner_tb);

			if (mTitleMainView != null) mTitleMainView.setText(mTitleMain);
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
		protected String who;
		protected String mTitleHeader;
		protected String mTitleMain;
		protected Button mBtnUp;
		protected Button mBtnDown;
		protected TextView mTitleMainView;
		protected boolean readwrite = true;

		protected Button.OnClickListener clickListener = new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				String action = "";
				switch (v.getId())
				{
				case R.id.card_inner_btnUp:
					action = "\"state\":\"up\"";
					break;
				case R.id.card_inner_btnDown:
					action = "\"state\":\"down\"";
					break;
				}
				deviceListListener.deviceListListener(ConnectionService.MSG_SWITCH_DEVICE, who + action);
			}
		};

		public ListScreenCard(Context context, DeviceEntry entry) {
			super(context, R.layout.screencard_inner);
			who = "\"device\":\"" + entry.getNameID() + "\",\"location\":\"" + entry.getLocationID() + "\",";
			for (SettingEntry sentry : entry.getSettings()) {
				if (sentry.getKey().equals("name")) mTitleHeader = sentry.getValue();
				if (sentry.getKey().equals("locationName")) mTitleMain = sentry.getValue();
				if (sentry.getKey().equals("sett_readonly") && sentry.getValue().equals("1")) readwrite = false;
			}
			init();
		}

		private void init() {
			setTitle(mTitleMain);
			// Create a CardHeader
			CardHeader header = new CardHeader(getContext());
			header.setTitle(mTitleHeader);
			addCardHeader(header);
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			// Retrieve elements
			mTitleMainView = (TextView) parent.findViewById(R.id.card_main_inner_simple_title);
			if (mTitleMainView != null) mTitleMainView.setText(mTitleMain);
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
}
