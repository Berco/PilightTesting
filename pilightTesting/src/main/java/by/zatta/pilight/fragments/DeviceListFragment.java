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

package by.zatta.pilight.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import by.zatta.pilight.R;
import by.zatta.pilight.cards.ContactCard;
import by.zatta.pilight.cards.DeviceCardAbstract;
import by.zatta.pilight.cards.DimmerCard;
import by.zatta.pilight.cards.PendingSwitchCard;
import by.zatta.pilight.cards.RelayCard;
import by.zatta.pilight.cards.ScreenCard;
import by.zatta.pilight.cards.SwitchCard;
import by.zatta.pilight.cards.WeatherCard;
import by.zatta.pilight.model.DeviceEntry;
import it.gmariotti.cardslib.library.extra.staggeredgrid.internal.CardGridStaggeredArrayAdapter;
import it.gmariotti.cardslib.library.extra.staggeredgrid.view.CardGridStaggeredView;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;

public class DeviceListFragment extends BaseFragment {

	private static final String TAG = "Zatta::DevicelistFragement";
	public static DeviceListListener deviceListListener;
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
		for (DeviceEntry entry : mDevices) {
			if (entry.hasGroup(mFilter) || mFilter == null) {
				Card card = cards.get(i);
				if (card instanceof DeviceCardAbstract) {
					((DeviceCardAbstract) card).update(entry);
				}
				i++;
			}
		}
		if (forceList) mCardArrayAdapter.notifyDataSetChanged();
		else mCardStaggeredGridArrayAdapter.notifyDataSetChanged();
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

	@Override
	public String getName() {
		return "DeviceList";
	}

	@Override
	public int getTitleResourceId() {
		return R.string.title_list_base;
	}

	private void initCards() {
		Log.v(TAG, "initCards");
		cards = new ArrayList<Card>();
		for (DeviceEntry device : mDevices) {
			Card card = null;
			if (device.hasGroup(mFilter) || mFilter == null) {
				switch (device.getType()) {
					case DeviceEntry.DeviceType.SWITCH:
						card = new SwitchCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.DIMMER:
						card = new DimmerCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.WEATHER:
						card = new WeatherCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.RELAY:
						card = new RelayCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.SCREEN:
						card = new ScreenCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.CONTACT:
						card = new ContactCard(getActivity().getApplicationContext(), device);
						break;
					case DeviceEntry.DeviceType.PENDINGSW:
						card = new PendingSwitchCard(getActivity().getApplicationContext(), device);
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

}
