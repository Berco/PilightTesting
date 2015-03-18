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

public class DeviceEntry implements Comparable<DeviceEntry>, Parcelable {

	public final class DeviceType {
		public static final int INTERNAL = -2;
		public static final int PROC = -1;
		public static final int RAW = -0;
		public static final int SWITCH = 1;
		public static final int DIMMER = 2;
		public static final int WEATHER = 3;
		public static final int RELAY = 4;
		public static final int SCREEN = 5;
		public static final int CONTACT = 6;
		public static final int PENDINGSW = 7;
		public static final int DATETIME = 8;
		public static final int XBMC = 9;
		public static final int LIRC = 10;
		public static final int WEBCAM = 11;
		public static final int MOTION = 12;
		public static final int DUSK = 13;
		public static final int PING = 14;
	}

	private String name_id;
	private int type;
	private int order;
	public List<SettingEntry> settings = new ArrayList<SettingEntry>();

	public DeviceEntry() {
	}

	public String getNameID() {
		return this.name_id;
	}

	public void setNameID(String name_id) {
		this.name_id = name_id;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public List<SettingEntry> getSettings() {
		return this.settings;
	}

	public void setSettings(List<SettingEntry> settings) {
		this.settings = settings;
	}

	@Override
	public String toString() {
		return this.name_id;
	}

	public boolean hasGroup(String filter) {
		for (SettingEntry sentry : settings) {
			if (sentry.getKey().equals("group") && sentry.getValue().equals(filter))
				return true;
		}
		return false;
	}

	public DeviceEntry(Parcel in) {
		this();
		readFromParcel(in);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.name_id);
		dest.writeInt(this.type);
		dest.writeInt(this.order);
		dest.writeList(this.settings);

	}

	@SuppressWarnings("unchecked")
	private void readFromParcel(Parcel in) {
		this.name_id = in.readString();
		this.type = in.readInt();
		this.order = in.readInt();
		this.settings = in.readArrayList(SettingEntry.class.getClassLoader());
	}

	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		@Override
		public DeviceEntry createFromParcel(Parcel in) {
			return new DeviceEntry(in);
		}

		@Override
		public DeviceEntry[] newArray(int size) {
			return new DeviceEntry[size];
		}
	};

	@Override
	public int compareTo(DeviceEntry o) {
		// Set weather cards on top, respect order for the rest.

		if ((this.getType() != o.getType()) && ((this.getType() == DeviceType.WEATHER) || (o.getType() == DeviceType.WEATHER))) {
			if (this.getType() == DeviceType.WEATHER && o.getType() != DeviceType.WEATHER) return -1;
			if (this.getType() != DeviceType.WEATHER && o.getType() == DeviceType.WEATHER) return 1;
		} else {
			if (this.getOrder() < o.getOrder()) return -1;
			if (this.getOrder() > o.getOrder()) return 1;
		}
		return 0;
	}
}
