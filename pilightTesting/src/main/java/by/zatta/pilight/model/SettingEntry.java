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

public class SettingEntry implements Parcelable {
	private String key;
	private String value;

	public SettingEntry() {
	}

	public SettingEntry(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.key + " = " + this.value;
	}

	public SettingEntry(Parcel in) {
		this();
		readFromParcel(in);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.key);
		dest.writeString(this.value);
	}

	private void readFromParcel(Parcel in) {
		this.key = in.readString();
		this.value = in.readString();
	}

	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		@Override
		public SettingEntry createFromParcel(Parcel in) {
			return new SettingEntry(in);
		}

		@Override
		public SettingEntry[] newArray(int size) {
			return new SettingEntry[size];
		}
	};
}
