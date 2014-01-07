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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import by.zatta.pilight.R;
import by.zatta.pilight.model.DeviceEntry;
import by.zatta.pilight.model.SettingEntry;

/**
 * List base example
 * 
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class OverviewFragment extends BaseFragment implements OnClickListener {

	OverViewListener overViewListener;
	private Button btnStart, btnStop, btnBind, btnUnbind, btnUpby1, btnUpby10;
	private static TextView textStatus;
	private TextView textIntValue;
	private static TextView textStrValue;

	static List<DeviceEntry> mDevices = new ArrayList<DeviceEntry>();

	@Override
	public int getTitleResourceId() {
		return R.string.overview_title;
	}

	@Override
	public String getName() {
		return "OverView";
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			overViewListener = (OverViewListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OverViewListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.debugging_layout, container, false);

		btnStart = (Button) v.findViewById(R.id.btnStart);
		btnStop = (Button) v.findViewById(R.id.btnStop);
		btnBind = (Button) v.findViewById(R.id.btnBind);
		btnUnbind = (Button) v.findViewById(R.id.btnUnbind);
		textStatus = (TextView) v.findViewById(R.id.textStatus);
		textIntValue = (TextView) v.findViewById(R.id.textIntValue);
		textStrValue = (TextView) v.findViewById(R.id.textStrValue);
		btnUpby1 = (Button) v.findViewById(R.id.btnUpby1);
		btnUpby10 = (Button) v.findViewById(R.id.btnUpby10);

		btnStart.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnBind.setOnClickListener(this);
		btnUnbind.setOnClickListener(this);
		btnUpby1.setOnClickListener(this);
		btnUpby10.setOnClickListener(this);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	public interface OverViewListener {
		public void overViewListener(int buttonPressed);
	}

	public static void updateUI(Bundle bundle) {
		if (bundle.getBoolean("bound"))
			textStatus.setText("bound");
		else
			textStatus.setText("not bound");
		String str1 = bundle.getString("str1");
		textStrValue.setText("Str Message: " + str1);
		mDevices = bundle.getParcelableArrayList("config");
		// print();
	}

	@Override
	public void onClick(View v) {
		overViewListener.overViewListener(v.getId());
	}

	public static void print() {
		System.out.println("________________");
		for (DeviceEntry device : mDevices) {
			System.out.println("-" + device.getNameID());
			System.out.println("-" + device.getLocationID());
			System.out.println("-" + device.getType());
			for (SettingEntry sentry : device.getSettings()) {
				System.out.println("*" + sentry.getKey() + " = " + sentry.getValue());
			}
			System.out.println("________________");
		}
	}

}
