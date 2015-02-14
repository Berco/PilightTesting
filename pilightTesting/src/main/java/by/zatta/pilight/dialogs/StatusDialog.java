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

package by.zatta.pilight.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import by.zatta.pilight.R;

public class StatusDialog extends DialogFragment implements OnClickListener {

	static OnChangedStatusListener changedStatusListener;
	private static Button mBtnCancel;
	private static Button mBtnSetup;
	private static ProgressBar pbConnecting;
	private String status;
	private static TextView tv;

	public final static int DISMISS = 1187639657;
	public final static int FINISH = 1432084755;
	public final static int RECONNECT = 1873475293;

	public static StatusDialog newInstance(String status) {
		StatusDialog f = new StatusDialog();
		Bundle args = new Bundle();
		args.putString("status", status);
		f.setArguments(args);
		return f;
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
		status = getArguments().getString("status");
		setStyle(DialogFragment.STYLE_NORMAL, 0);
		setCancelable(false);
		// setRetainInstance(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.title_status_dialog);
		View v = inflater.inflate(R.layout.status_dialog_layout, container, false);
		tv = (TextView) v.findViewById(R.id.tvStatusDisplay);
		pbConnecting = (ProgressBar) v.findViewById(R.id.pbConnecting);
		mBtnCancel = (Button) v.findViewById(R.id.btnCancelStart);
		mBtnSetup = (Button) v.findViewById(R.id.btnSetupConnection);
		mBtnCancel.setOnClickListener(this);
		mBtnSetup.setOnClickListener(this);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setChangedStatus(status);
	}

	public enum NotificationType {
		CONNECTED, CONNECTING, DESTROYED, FAILED, LOST_CONNECTION, UPDATE,
	}

	public static void setChangedStatus(String status) {
		// TODO check for correctness
		mBtnCancel.setVisibility(View.GONE);
		mBtnSetup.setVisibility(View.GONE);
		pbConnecting.setVisibility(View.GONE);
		mBtnCancel.setText(R.string.btn_close);
		mBtnSetup.setText(R.string.btn_retry);
		if (status.equals("CONNECTED")) {
			changedStatusListener.onChangedStatusListener(DISMISS);
		} else if (status.equals("CONNECTING")) {
			pbConnecting.setVisibility(View.VISIBLE);
			tv.setText(R.string.status_connecting);
			mBtnCancel.setVisibility(View.VISIBLE);
			mBtnCancel.setText(R.string.btn_abort);
		} else if (status.equals("DESTROYED")) {
			tv.setText(R.string.status_destroyed);
			mBtnCancel.setVisibility(View.VISIBLE);
		} else if (status.equals("FAILED")) {
			tv.setText(R.string.status_failed);
			mBtnCancel.setVisibility(View.VISIBLE);
			mBtnSetup.setVisibility(View.VISIBLE);
		} else if (status.equals("LOST_CONNECTION")) {
			tv.setText(R.string.status_lost);
			mBtnCancel.setVisibility(View.VISIBLE);
			mBtnSetup.setVisibility(View.VISIBLE);
		}
	}

	public interface OnChangedStatusListener {
		public void onChangedStatusListener(int what);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId())
		{
		case R.id.btnCancelStart:
			changedStatusListener.onChangedStatusListener(FINISH);
			break;
		case R.id.btnSetupConnection:
			changedStatusListener.onChangedStatusListener(RECONNECT);
			break;
		}
	}

}
