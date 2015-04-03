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

	private boolean isSSDP;
	private String networkName;
	private String host;
	private String port;

	public ConnectionEntry() {
	}

	public ConnectionEntry(String fromPrefs){
		String[] gotFromPrefs = fromPrefs.split(";");
		this.host = gotFromPrefs[0];
		this.port = gotFromPrefs[1];
		this.networkName = gotFromPrefs[2];
		if (gotFromPrefs[3].equals("true")) this.isSSDP = true;
		else this.isSSDP = false;
	}

	public ConnectionEntry(String in_host, String in_port, String in_network, boolean isSSDP){
		this.host = in_host;
		this.port = in_port;
		this.networkName = in_network;
		this.isSSDP = isSSDP;
	}

	public ConnectionEntry(String in_host, String in_port){
		host = in_host;
		port = in_port;
		isSSDP = false;
	}

	public ConnectionEntry(boolean wasSSDP, String networkName){
		this.host = "ssdp";
		this.port = "at " + networkName;
		this.networkName = networkName;
		this.isSSDP = true;
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

	public boolean isSSDP() { return isSSDP; }

	public void wasSSDP(boolean wasSSDP) { this.isSSDP = wasSSDP; }

	public String getNetworkName() { return networkName; }

	public void setNetworkName(String in_networkName) { this.networkName = in_networkName; }

	@Override
	public String toString() {
		return host + ";" + port + ";" + networkName + ";" + isSSDP;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.host);
		dest.writeString(this.port);
		dest.writeString(this.networkName);
		dest.writeInt(isSSDP ? 1 : 0);
	}

	@SuppressWarnings("unchecked")
	private void readFromParcel(Parcel in) {
		this.host = in.readString();
		this.port = in.readString();
		this.networkName = in.readString();

		int ssdpInt = in.readInt();
		if (ssdpInt == 1) this.isSSDP = true; else this.isSSDP = false;
	}
}
