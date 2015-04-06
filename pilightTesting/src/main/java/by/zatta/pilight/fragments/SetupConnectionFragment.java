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
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import by.zatta.pilight.R;
import by.zatta.pilight.model.ConnectionEntry;
import by.zatta.pilight.views.CustomHeaderInnerCard;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;

public class SetupConnectionFragment extends BaseFragment implements View.OnClickListener {

	public final static int DISMISS = 1187639657;
	public final static int FINISH = 1432084755;
	public final static int RECONNECT = 1873475293;
	private static final String TAG = "SetupConnectionFragment";
	static OnChangedStatusListener changedStatusListener;
	static ArrayList<Card> cards;
	private static FloatingActionButton mBtnFAB;
	private static Context aCtx;
	private static boolean prefUseSSDP;
	private static ProgressBar pbConnecting;
	private static String mStatus;
	private static View mEv;
	private static CardArrayAdapter mCardArrayAdapter;
	CardListView listView;

	public static SetupConnectionFragment newInstance(String status) {
		SetupConnectionFragment f = new SetupConnectionFragment();
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
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.setup_connection_layout, container, false);
		mEv = (View) v.findViewById(R.id.emptyView);
		pbConnecting = (ProgressBar) v.findViewById(R.id.pbConnecting);
		mBtnFAB = (FloatingActionButton) v.findViewById(R.id.btnFAB);
		mBtnFAB.setOnClickListener(this);
		listView = (CardListView) v.findViewById(R.id.clCardList);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		aCtx = getActivity().getApplicationContext();
		if (mStatus == null)
			mStatus = getArguments().getString("status");

		SharedPreferences prefs = aCtx.getSharedPreferences("ZattaPrefs", Context.MODE_MULTI_PROCESS);
		prefUseSSDP = prefs.getBoolean("useSSDP", true);

		cards = new ArrayList<Card>();

		if (prefUseSSDP) {
			HostHolderCard ssdpCard = new HostHolderCard(aCtx, null);
			cards.add(ssdpCard);
		}

		getConnectionsFromPrefs();

		mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards);
		if (listView != null) {
			listView.setAdapter(mCardArrayAdapter);
		}
		setChangedStatus(mStatus);
	}

	private void getConnectionsFromPrefs() {
		SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("ZattaPrefs", Context.MODE_MULTI_PROCESS);
		Set<String> connections = prefs.getStringSet("know_connections", new HashSet<String>());
		Iterator<?> connIt = connections.iterator();
		while (connIt.hasNext()) {
			String aConnection = (String) connIt.next();
			ConnectionEntry entry = new ConnectionEntry(aConnection);
			if (!entry.isSSDP()) {
				cards.add(new HostHolderCard(aCtx, entry));
			}
		}
	}

	private static int stringToColor(String inString){
		String date = java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime());
		int i = (inString + date).hashCode();
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

	public static void setChangedStatus(String status) {
		mStatus = status;
		Log.d(TAG, status);
		mBtnFAB.setBackgroundColor(stringToColor(mStatus));
		// TODO check for correctness

		if (status.equals("CONNECTED")) {
			mStatus = null;
			changedStatusListener.onChangedStatusListener(DISMISS, null);
		} else if (status.equals("CONNECTING")) {
			pbConnecting.setVisibility(View.VISIBLE);
			mBtnFAB.hide(true);
		} else {
			//if "LOST_CONNECTION", "FAILED", "DESTROYED", "NO_SERVER"
			int max;
			//TODO we need to have a list of possible servers, now we actually get a list if multiple times a connection has been
			//TODO made but it sucks big time. The last succesfull connection will be the one tried when pressing the button.
			if (prefUseSSDP) max = 2;
			else max = 1;
			if (cards.size() < max) {
				cards.add(new HostHolderCard(aCtx, new ConnectionEntry(null, null, null, false)));
				mCardArrayAdapter.notifyDataSetChanged();
			}
			pbConnecting.setVisibility(View.GONE);
			mBtnFAB.show(true);
		}
	}

	@Override
	public String getName() {
		return "SetupConnectionFragment";
	}

	@Override
	public int getTitleResourceId() {
		return R.string.title_status_fragment;
	}

	@Override
	public void onClick(View v) {
		mEv.requestFocus();

		switch (v.getId()) {
			case R.id.btnCancelStart:
				changedStatusListener.onChangedStatusListener(FINISH, null);
				break;
			case R.id.btnFAB:
				HostHolderCard card = (HostHolderCard) cards.get(cards.size() - 1);
				if (!(card.getConnEntry().isSSDP())) {
					changedStatusListener.onChangedStatusListener(RECONNECT, card.getConnEntry());
				} else {
					changedStatusListener.onChangedStatusListener(RECONNECT, card.getConnEntry());
				}
				break;
		}
	}

	public interface OnChangedStatusListener {
		public void onChangedStatusListener(int what, ConnectionEntry connectionEntry);
	}

	/*
	 * HostHolderCard ****************************************************************************************************
	 */
	private static class HostHolderCard extends Card {

		private ConnectionEntry mConEntry;
		private CustomHeaderInnerCard header;
		private EditText mEtHost;
		private EditText mEtPort;

		public HostHolderCard(Context context, ConnectionEntry conEntry) {
			//TODO make the card look pretty
			super(context, R.layout.hostholdercard_inner);
			if (conEntry != null) {
				mConEntry = conEntry;
				header = new CustomHeaderInnerCard(context, "Server", null);
				addCardHeader(header);
			} else {
				header = new CustomHeaderInnerCard(context, "Searching", null);
				addCardHeader(header);
				mConEntry = new ConnectionEntry("ssdp;404;home;true");
			}
		}

		@Override
		public void setupInnerViewElements(ViewGroup parent, View view) {
			header.showAlwaysCB(true);
			mEtHost = (EditText) view.findViewById(R.id.etHost);
			mEtPort = (EditText) view.findViewById(R.id.etPort);
			TextView mTvColon = (TextView) view.findViewById(R.id.colon);
			ImageView mImageView = (ImageView) view.findViewById(R.id.colorBorder);

			if (!mConEntry.isSSDP()) {
				mEtHost.setVisibility(View.VISIBLE);
				mEtPort.setVisibility(View.VISIBLE);
				mEtHost.setText(mConEntry.getHost());
				mEtPort.setText(mConEntry.getPort());
				mTvColon.setText(aCtx.getResources().getString(R.string.colon));
				mEtHost.setOnFocusChangeListener(new OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						mConEntry.setHost(((EditText) v).getText().toString());
						Log.d(SetupConnectionFragment.TAG, "HOST FOCUS CHANGED to: " + hasFocus);
					}
				});
				mEtPort.setOnFocusChangeListener(new OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						mConEntry.setPort(((EditText) v).getText().toString());
						Log.d(SetupConnectionFragment.TAG, "PORT FOCUS CHANGED to: " + hasFocus);
					}
				});
				mImageView.setBackgroundColor(SetupConnectionFragment.stringToColor("blahblah"));
			} else {
				mEtHost.setVisibility(View.GONE);
				mEtPort.setVisibility(View.GONE);
				mTvColon.setText("Searching your server via SSDP");
				mImageView.setBackgroundColor(SetupConnectionFragment.stringToColor("joetoetet"));
			}
		}

		public ConnectionEntry getConnEntry() {
			return mConEntry;
		}

		public boolean doAlways() {
			return header.doAlways();
		}

	}

}
