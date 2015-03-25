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
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import by.zatta.pilight.R;
import by.zatta.pilight.dialogs.AboutDialog;

public class PrefFragment extends BasePreferenceFragment {
	static final String TAG = "PrefFragment";
	OnLanguageListener languageListener;
	OnViewChangeListener viewChangeListener;
	PreferenceManager manager;

	// OnResetListener resetListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			languageListener = (OnLanguageListener) activity;
			viewChangeListener = (OnViewChangeListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement correct Listener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Context context = this.getActivity().getLayoutInflater().getContext();
		manager = getPreferenceManager();
		manager.setSharedPreferencesName("ZattaPrefs");
		manager.setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
		setPreferenceScreen(createPreferenceHierarchy(context));

	}

	private PreferenceScreen createPreferenceHierarchy(Context mContext) {
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(mContext);
		root.setKey("pilight_preferences");

		PreferenceCategory launchPrefCat = new PreferenceCategory(mContext);
		launchPrefCat.setTitle(R.string.prefTit_info);
		root.addPreference(launchPrefCat);

		Preference infoScreenPref = getPreferenceManager().createPreferenceScreen(mContext);
		infoScreenPref.setTitle(R.string.title_about);
		infoScreenPref.setSummary(R.string.prefSum_about);
		infoScreenPref.setKey("about_app_key");
		launchPrefCat.addPreference(infoScreenPref);

		Preference homePref = getPreferenceManager().createPreferenceScreen(mContext);
		homePref.setIntent(new Intent().setAction(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.setData(Uri.parse("http://www.pilight.org")));
		homePref.setTitle(R.string.prefTit_web);
		homePref.setSummary(R.string.prefSum_web);
		homePref.setKey("visit_pilight");
		launchPrefCat.addPreference(homePref);

		Preference tweakersPref = getPreferenceManager().createPreferenceScreen(mContext);
		tweakersPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.setData(Uri.parse("http://gathering.tweakers.net/forum/list_messages/1556119/last")));
		tweakersPref.setTitle(R.string.prefTit_tweakers);
		tweakersPref.setSummary(R.string.prefSum_tweakers);
		tweakersPref.setKey("visit_tweakers");
		launchPrefCat.addPreference(tweakersPref);

		PreferenceCategory settingsPrefCat = new PreferenceCategory(mContext);
		settingsPrefCat.setTitle(R.string.title_settings);
		root.addPreference(settingsPrefCat);

		Preference removeNetworksPref = getPreferenceManager().createPreferenceScreen(mContext);
		removeNetworksPref.setTitle(R.string.prefTit_connection);
		removeNetworksPref.setSummary(R.string.prefSum_connection);
		removeNetworksPref.setKey("remove_all_nets");
		settingsPrefCat.addPreference(removeNetworksPref);

		CheckBoxPreference serviceCheckBoxPref = new CheckBoxPreference(mContext);
		serviceCheckBoxPref.setTitle(R.string.prefTit_useService);
		serviceCheckBoxPref.setSummary(R.string.prefSum_useService);
		serviceCheckBoxPref.setKey("useService");
		serviceCheckBoxPref.setDefaultValue(true);
		settingsPrefCat.addPreference(serviceCheckBoxPref);

		CheckBoxPreference ssdpCheckBoxPref = new CheckBoxPreference(mContext);
		ssdpCheckBoxPref.setTitle(R.string.prefTit_useSSDP);
		ssdpCheckBoxPref.setSummary(R.string.prefSum_useSSDP);
		ssdpCheckBoxPref.setKey("useSSDP");
		ssdpCheckBoxPref.setDefaultValue(true);
		settingsPrefCat.addPreference(ssdpCheckBoxPref);

		PreferenceCategory uInterfacePrefCat = new PreferenceCategory(mContext);
		uInterfacePrefCat.setTitle(R.string.prefTit_UI);
		root.addPreference(uInterfacePrefCat);

		CheckBoxPreference forceListCheckBoxPref = new CheckBoxPreference(mContext);
		forceListCheckBoxPref.setTitle(R.string.prefTit_listgrid);
		forceListCheckBoxPref.setSummary(R.string.prefSum_listgrid);
		forceListCheckBoxPref.setKey("forceList");
		forceListCheckBoxPref.setDefaultValue(false);
		forceListCheckBoxPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.e(TAG, "PrefChange: " + Boolean.toString((Boolean) newValue));
				//I need this editor to get the change written to the preferences before this method returns
				Editor edit = manager.getSharedPreferences().edit();
				edit.putBoolean("forceList", (Boolean) newValue).commit();
				viewChangeListener.onViewChangeListener((Boolean) newValue);
				return true;
			}
		});
		uInterfacePrefCat.addPreference(forceListCheckBoxPref);

		CheckBoxPreference destroyedCheckBoxPref = new CheckBoxPreference(mContext);
		destroyedCheckBoxPref.setTitle(R.string.prefTit_destroyNotification);
		destroyedCheckBoxPref.setSummary(R.string.prefSum_destroyNotification);
		destroyedCheckBoxPref.setKey("destroySilent");
		destroyedCheckBoxPref.setDefaultValue(false);
		uInterfacePrefCat.addPreference(destroyedCheckBoxPref);

		CheckBoxPreference showAllByDefaultPref = new CheckBoxPreference(mContext);
		showAllByDefaultPref.setTitle(R.string.prefTit_showAll);
		showAllByDefaultPref.setSummary(R.string.prefSum_showAll);
		showAllByDefaultPref.setKey("showAllByDefault");
		showAllByDefaultPref.setDefaultValue(false);
		uInterfacePrefCat.addPreference(showAllByDefaultPref);

		ListPreference listPref = new ListPreference(mContext);
		listPref.setEntries(R.array.languages);
		listPref.setEntryValues(R.array.languages_short);
		listPref.setDialogTitle(R.string.prefTit_language);
		listPref.setKey("languagePref");
		listPref.setTitle(R.string.prefTit_language);
		listPref.setSummary(R.string.prefSum_language);
		uInterfacePrefCat.addPreference(listPref);

		return root;
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference pref) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment about = getFragmentManager().findFragmentByTag("dialog");

		if (about != null) ft.remove(about);
		ft.addToBackStack(null);

		if (pref.getKey().contentEquals("about_app_key")) {
			DialogFragment aboutFragment = AboutDialog.newInstance();
			aboutFragment.show(ft, "dialog");
			return true;
		}

		if (pref.getKey().contentEquals("remove_all_nets")) {
			Toast.makeText(getActivity().getApplicationContext(), "forgot all known networks", Toast.LENGTH_LONG).show();
			pref.getEditor().putString("networks_known", "").commit();
			pref.getEditor().putString("known_host", "").commit();
			pref.getEditor().putString("known_port", "").commit();
			return true;
		}

		if (pref.getKey().contentEquals("languagePref")) {
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					languageListener.onLanguageListener(newValue.toString());
					return true;
				}
			});
		}
		return false;
	}

	@Override
	public int getTitleResourceId() {
		return R.string.title_settings;
	}

	public interface OnLanguageListener {
		public void onLanguageListener(String language);
	}

	public interface OnViewChangeListener {
		public void onViewChangeListener(Boolean forceList);
	}

}
