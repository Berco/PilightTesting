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

package by.zatta.pilight.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class OriginEntry implements Parcelable {

	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		@Override
		public OriginEntry createFromParcel(Parcel in) {
			return new OriginEntry(in);
		}

		@Override
		public OriginEntry[] newArray(int size) {
			return new OriginEntry[size];
		}
	};
	public List<SettingEntry> settings = new ArrayList<SettingEntry>();
	private String name_id;
	private String popular_name;

	public OriginEntry() {
	}

	public OriginEntry(Parcel in) {
		this();
		readFromParcel(in);
	}

	public String getNameID() {
		return this.name_id;
	}

	public void setNameID(String name_id) {
		this.name_id = name_id;
	}

	public List<SettingEntry> getSettings() {
		return this.settings;
	}

	public void setSettings(List<SettingEntry> settings) {
		this.settings = settings;
	}

	public String getPopularName() {
		return this.popular_name;
	}

	public void setPopularName(String popular_name) {
		this.popular_name = popular_name;
	}

	@Override
	public String toString() {
		String toBeReturned = this.getNameID();
		for (SettingEntry sentry : settings) {
			toBeReturned = toBeReturned + "\n" + sentry.getValue();
		}
		return toBeReturned;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.name_id);
		dest.writeList(this.settings);
	}

	@SuppressWarnings("unchecked")
	private void readFromParcel(Parcel in) {
		this.name_id = in.readString();
		this.settings = in.readArrayList(SettingEntry.class.getClassLoader());
	}
}
