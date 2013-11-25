package by.zatta.pilight.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class DeviceEntry implements Comparable<DeviceEntry>, Parcelable{
	
    private String location;
	private String name;
	public List<SettingEntry> settings = new ArrayList<SettingEntry>();
	
	
		public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.name = location;
	}

	public List<SettingEntry> getSettings() {
		return this.settings;
	}

	public void setSettings(List<SettingEntry> settings) {
		this.settings = settings;
	}
	
    public DeviceEntry(){}
    
    public DeviceEntry(String name, String location, List<SettingEntry> settings) {
    	this.name = name;
    	this.location = location;
    	this.settings = settings;
    }
    
    @Override public String toString() {
        return this.name;
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
		dest.writeString(this.name);
		dest.writeString(this.location);
		dest.writeList(settings);
		//writeMap(settings, dest);
	}

	@SuppressWarnings("unchecked")
	private void readFromParcel(Parcel in) {
		this.name = in.readString();
		this.location = in.readString();
		in.readTypedList(settings, SettingEntry.CREATOR);
	}
	
	
     
    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
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
    		
    			if(this.location != null && this.name != null){
    				if ( this.location.compareTo(o.getLocation()) == 0   ){
    					return o.getName().compareTo(this.name);
    					
    				}else{
    					return o.getLocation().compareTo(this.location);
    				}
    			}
    			else 
    				throw new IllegalArgumentException();
    	}
}
