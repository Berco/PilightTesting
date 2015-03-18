/******************************************************************************************
 *
 * Copyright (C) 2015 Zatta
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
 * Copyright (c) 2015 pilight project
 ********************************************************************************************/

package by.zatta.pilight.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import by.zatta.pilight.R;
import by.zatta.pilight.views.FloatingActionButton;

public class SetupConnectionFragment extends BaseFragment implements View.OnClickListener {

	public final static int DISMISS = 1187639657;
	public final static int FINISH = 1432084755;
	public final static int RECONNECT = 1873475293;
	public final static int CUSTOM_SERVER = 98273234;
	private static FloatingActionButton mBtnFAB;
	private static Context aCtx;
	private static final String TAG = "SetupConnectionFragment";
	static OnChangedStatusListener changedStatusListener;
	private static ProgressBar pbConnecting;
	private static EditText mEtHost;
	private static EditText mEtPort;
	private static String mStatus;
	private static TextView tv;

	public static SetupConnectionFragment newInstance(String status) {
		SetupConnectionFragment f = new SetupConnectionFragment();
		Bundle args = new Bundle();
		args.putString("status", status);
		f.setArguments(args);
		return f;
	}

	public static void setChangedStatus(String status) {
		mStatus = status;
		Log.d(TAG, status);
		// TODO check for correctness
		pbConnecting.setVisibility(View.GONE);
		mBtnFAB.hide(false);
		mBtnFAB.setDrawable(aCtx.getResources().getDrawable(R.drawable.ic_av_play));
		if (status.equals("CONNECTED")) {
			changedStatusListener.onChangedStatusListener(DISMISS, null);
		} else if (status.equals("CONNECTING")) {
			pbConnecting.setVisibility(View.VISIBLE);
			tv.setText(R.string.status_connecting);
			mBtnFAB.hide(true);
		} else if (status.equals("DESTROYED")) {
			tv.setText(R.string.status_destroyed);
			mBtnFAB.setDrawable(aCtx.getResources().getDrawable(R.drawable.ic_av_play));
			mBtnFAB.hide(false);
		} else if (status.equals("FAILED")) {
			tv.setText(R.string.status_failed);
			mBtnFAB.setDrawable(aCtx.getResources().getDrawable(R.drawable.ic_av_play));
			mBtnFAB.hide(false);
		} else if (status.equals("NO_SERVER")) {
			tv.setText(status);
			mBtnFAB.setDrawable(aCtx.getResources().getDrawable(R.drawable.ic_av_play));
			mBtnFAB.hide(false);

			//TODO Make this behavior a preference
			String host = mEtHost.getText().toString();
			String port = mEtPort.getText().toString();
			if (!(host == null) && !(port == null)) {
				String adress = host + ":" + port;
				changedStatusListener.onChangedStatusListener(CUSTOM_SERVER, adress);
			}
		} else if (status.equals("LOST_CONNECTION")) {
			tv.setText(R.string.status_lost);
			mBtnFAB.setDrawable(aCtx.getResources().getDrawable(R.drawable.ic_av_play));
			mBtnFAB.hide(false);
		}
	}

	@Override
	public int getTitleResourceId() {
		return R.string.title_status_fragment;
	}

	@Override
	public String getName() {
		return "SetupConnectionFragment";
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			changedStatusListener = (OnChangedStatusListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnChangedStatusListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mStatus = getArguments().getString("status");
		setRetainInstance(true);
		aCtx = getActivity().getApplicationContext();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.setup_connection_layout, container, false);
		tv = (TextView) v.findViewById(R.id.tvStatusDisplay);
		pbConnecting = (ProgressBar) v.findViewById(R.id.pbConnecting);
		mEtHost = (EditText) v.findViewById(R.id.etHost);
		mEtPort = (EditText) v.findViewById(R.id.etPort);
		mBtnFAB = (FloatingActionButton) v.findViewById(R.id.btnFAB);

		mBtnFAB.setOnClickListener(this);
		SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("ZattaPrefs", Context.MODE_MULTI_PROCESS);
		String known_host = prefs.getString("known_host", null);
		String known_port = prefs.getString("known_port", null);
		if (!(known_host == null) && !(known_port == null)) {
			mEtHost.setText(known_host);
			mEtPort.setText(known_port);
		}
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setChangedStatus(mStatus);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnCancelStart:
				changedStatusListener.onChangedStatusListener(FINISH, null);
				break;
			case R.id.btnFAB:
				mBtnFAB.hide(true);
				String host = mEtHost.getText().toString();
				String port = mEtPort.getText().toString();
				if (!(host == null) && !(port == null)) {
					SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("ZattaPrefs", Context.MODE_MULTI_PROCESS);

					SharedPreferences.Editor edit = prefs.edit();
					edit.putString("known_host", host);
					edit.putString("known_port", port);
					edit.commit();
					String adress = host + ":" + port;
					changedStatusListener.onChangedStatusListener(CUSTOM_SERVER, adress);
				} else {
					changedStatusListener.onChangedStatusListener(RECONNECT, null);
				}
				break;
		}
	}

	public interface OnChangedStatusListener {
		public void onChangedStatusListener(int what, String adress);
	}
}
