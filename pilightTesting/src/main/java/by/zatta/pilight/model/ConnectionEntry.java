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
 * with pilight for android.
 * If not, see <http://www.gnu.org/licenses/>
 *
 * Copyright (c) 2015 pilight project
 ********************************************************************************************/

package by.zatta.pilight.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ConnectionEntry implements Parcelable {

	@SuppressWarnings("rawtypes")
	public static final Creator CREATOR = new Creator() {
		@Override
		public ConnectionEntry createFromParcel(Parcel in) {
			return new ConnectionEntry(in);
		}

		@Override
		public ConnectionEntry[] newArray(int size) {
			return new ConnectionEntry[size];
		}
	};

	private boolean isPassive = false;
	private boolean isSSDP;
	private boolean isAuto;
	private String host;
	private String port;

	public ConnectionEntry() {
	}

	public ConnectionEntry(String fromPrefs){
		String[] gotFromPrefs = fromPrefs.split(";");
		this.host = gotFromPrefs[0];
		this.port = gotFromPrefs[1];
		if (gotFromPrefs[2].equals("true")) this.isAuto = true;
		else this.isAuto = false;
		if (gotFromPrefs[3].equals("true")) this.isSSDP = true;
		else this.isSSDP = false;
	}

	public ConnectionEntry(String in_host, String in_port, boolean auto, boolean isSSDP){
		this.host = in_host;
		this.port = in_port;
		this.isAuto = auto;
		this.isSSDP = isSSDP;
	}

	public ConnectionEntry(Parcel in) {
		this();
		readFromParcel(in);
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String in_host) {
		this.host = in_host;
	}

	public String getPort() {
		return this.port;
	}

	public void setPort(String in_port) {
		this.port = in_port;
	}

	public boolean isSSDP() { return this.isSSDP; }

	public boolean isAuto() { return this.isAuto; }

	public void setIsAuto(boolean isAuto) { this.isAuto = isAuto; }

	public boolean isPassive() { return this.isPassive; }

	public void setPassive(boolean isPassive) {this.isPassive = isPassive; }

	@Override
	public String toString() {
		return host + ";" + port + ";" + isAuto + ";" + isSSDP;
	}

	public String toStringNegative() {
		return host + ";" + port + ";" + !isAuto + ";" + isSSDP;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.host);
		dest.writeString(this.port);
		dest.writeInt(this.isAuto ? 1 : 0);
		dest.writeInt(this.isSSDP ? 1 : 0);
		dest.writeInt(this.isPassive ? 1 : 0);
	}

	@SuppressWarnings("unchecked")
	private void readFromParcel(Parcel in) {
		this.host = in.readString();
		this.port = in.readString();

		int autoInt = in.readInt();
		if (autoInt == 1) this.isAuto = true; else this.isAuto = false;

		int ssdpInt = in.readInt();
		if (ssdpInt == 1) this.isSSDP = true; else this.isSSDP = false;

		int passiveInt = in.readInt();
		if (passiveInt == 1) this.isPassive = true; else this.isPassive = false;
	}
}
