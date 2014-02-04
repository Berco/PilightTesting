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

import by.zatta.pilight.R;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import by.zatta.pilight.dialogs.AboutDialog;

public class PrefFragment extends BasePreferenceFragment {

	private static final String TAG = "PrefFragment";

	// OnLanguageListener languageListener;
	// OnResetListener resetListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// try {
		// languageListener = (OnLanguageListener) activity;
		// resetListener = (OnResetListener) activity;
		// } catch (ClassCastException e) {
		// throw new ClassCastException(activity.toString() + " must implement correct Listener");
		// }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Context context = this.getActivity().getLayoutInflater().getContext();
		setPreferenceScreen(createPreferenceHierarchy(context));
	}

	private PreferenceScreen createPreferenceHierarchy(Context mContext) {
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(mContext);
		root.setKey("pilight_preferences");

		PreferenceCategory launchPrefCat = new PreferenceCategory(mContext);
		launchPrefCat.setTitle("Information");
		root.addPreference(launchPrefCat);

		Preference infoScreenPref = getPreferenceManager().createPreferenceScreen(mContext);
		infoScreenPref.setTitle("About");
		infoScreenPref.setSummary("All about the app");
		infoScreenPref.setKey("about_app_key");
		launchPrefCat.addPreference(infoScreenPref);

		Preference homePref = getPreferenceManager().createPreferenceScreen(mContext);
		homePref.setIntent(new Intent().setAction(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.setData(Uri.parse("http://www.pilight.org")));
		homePref.setTitle("pilight.org");
		homePref.setSummary("visit the forum");
		homePref.setKey("visit_xda");
		launchPrefCat.addPreference(homePref);

		Preference tweakersPref = getPreferenceManager().createPreferenceScreen(mContext);
		tweakersPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.setData(Uri.parse("http://gathering.tweakers.net/forum/list_messages/1556119/last")));
		tweakersPref.setTitle("Tweakers.net");
		tweakersPref.setSummary("Dutch forum thread");
		tweakersPref.setKey("vist_dg");
		launchPrefCat.addPreference(tweakersPref);

		PreferenceCategory settingsPrefCat = new PreferenceCategory(mContext);
		settingsPrefCat.setTitle("Settings");
		root.addPreference(settingsPrefCat);

		Preference removeNetworksPref = getPreferenceManager().createPreferenceScreen(mContext);
		removeNetworksPref.setTitle("Connection");
		removeNetworksPref.setSummary("remove all remembered networks");
		removeNetworksPref.setKey("remove_all_nets");
		settingsPrefCat.addPreference(removeNetworksPref);

		CheckBoxPreference serviceCheckBoxPref = new CheckBoxPreference(mContext);
		serviceCheckBoxPref.setTitle("Keep service running");
		serviceCheckBoxPref.setSummary("also when not in the app");
		serviceCheckBoxPref.setKey("useService");
		serviceCheckBoxPref.setDefaultValue(true);
		settingsPrefCat.addPreference(serviceCheckBoxPref);
		
		CheckBoxPreference forceListCheckBoxPref = new CheckBoxPreference(mContext);
		forceListCheckBoxPref.setTitle("Force list for devices");
		forceListCheckBoxPref.setSummary("instead of GridView");
		forceListCheckBoxPref.setKey("forceList");
		forceListCheckBoxPref.setDefaultValue(false);
		settingsPrefCat.addPreference(forceListCheckBoxPref);

		// Preference resetCustom = getPreferenceManager().createPreferenceScreen(mContext);
		// resetCustom.setTitle(R.string.ResetCustomTitle);
		// resetCustom.setSummary(R.string.ResetCustomSummary);
		// resetCustom.setKey("reset_custom_key");
		// settingsPrefCat.addPreference(resetCustom);

		// CheckBoxPreference welcomeCheckBoxPref = new CheckBoxPreference(mContext);
		// welcomeCheckBoxPref.setTitle(R.string.PrefWelcomeTitle);
		// welcomeCheckBoxPref.setSummary(R.string.PrefWelcomeSummary);
		// welcomeCheckBoxPref.setKey("showFirstUse");
		// settingsPrefCat.addPreference(welcomeCheckBoxPref);

		// ListPreference listPref = new ListPreference(mContext);
		// listPref.setEntries(R.array.languages);
		// listPref.setEntryValues(R.array.languages_short);
		// listPref.setDialogTitle(R.string.LanguagePrefTitle);
		// listPref.setKey("languagePref");
		// listPref.setTitle(R.string.LanguagePrefTitle);
		// listPref.setSummary(R.string.LanguagePrefSummary);
		// settingsPrefCat.addPreference(listPref);

		// CheckBoxPreference addonCheckBoxPref = new CheckBoxPreference(mContext);
		// addonCheckBoxPref.setTitle(R.string.AddonScriptPrefTitle);
		// addonCheckBoxPref.setSummary(R.string.AddonScriptPrefSummary);
		// addonCheckBoxPref.setKey("enableAddonScript");
		// addonCheckBoxPref.setChecked(true);
		// if (ShellProvider.INSTANCE.isAddonable())
		// settingsPrefCat.addPreference(addonCheckBoxPref);

		// CheckBoxPreference debugCheckBoxPref = new CheckBoxPreference(mContext);
		// debugCheckBoxPref.setTitle(R.string.DebugPrefTitle);
		// debugCheckBoxPref.setSummary(R.string.DebugPrefSummary);
		// debugCheckBoxPref.setKey("enableDebugging");
		// debugCheckBoxPref.setChecked(true);
		// settingsPrefCat.addPreference(debugCheckBoxPref);

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
			return true;
		}

		//
		// if (pref.getKey().contentEquals("reset_custom_key")){
		// Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toastReset), Toast.LENGTH_LONG).show();
		// resetListener.onResetListener();
		// return true;
		// }
		//
		// if (pref.getKey().contentEquals("languagePref")){
		// pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
		// @Override
		// public boolean onPreferenceChange(Preference preference, Object newValue) {
		// languageListener.onLanguageListener(newValue.toString());
		// return true;
		// }
		// });
		// }
		//
		// if (pref.getKey().contentEquals("enableAddonScript")){
		// ShellProvider.INSTANCE.mountRW(true);
		// if (pref.getSharedPreferences().getBoolean("enableAddonScript", true)){
		// Log.w(TAG, "enabled addon.d support");
		// ShellProvider.INSTANCE.copyAddon();
		// } else {
		// Log.w(TAG, "disabled addon.d support");
		// ShellProvider.INSTANCE.removeAddon();
		// }
		// ShellProvider.INSTANCE.mountRW(false);
		// return true;
		// }

		return false;
	}

	@Override
	public int getTitleResourceId() {
		return R.string.action_settings;
	}

	// public interface OnLanguageListener{
	// public void onLanguageListener(String language);
	// }
	//
	// public interface OnResetListener{
	// public void onResetListener();
	// }

}
