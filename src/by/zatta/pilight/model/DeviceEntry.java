package by.zatta.pilight.model;

import java.util.ArrayList;
import java.util.List;
import android.os.Parcel;
import android.os.Parcelable;

public class DeviceEntry implements Comparable<DeviceEntry>, Parcelable{
    private String location_id;
	private String name_id;
	private int type;
	public List<SettingEntry> settings = new ArrayList<SettingEntry>();
	
	public DeviceEntry(){}
    public DeviceEntry(String name_id, String location_id, List<SettingEntry> settings) {
    	this.name_id = name_id;
    	this.location_id = location_id;
    	this.settings = settings;
    }
	
	public String getNameID() 				{ return this.name_id; 		}
	public void setNameID(String name_id) 	{ this.name_id = name_id;	}
	
	public String getLocationID() 				{ return this.location_id;	}
	public void setLocationID(String location_id) { this.location_id = location_id; }
	
	public int getType()					{ return this.type; }
	public void setType(int type)			{ this.type = type; }

	public List<SettingEntry> getSettings() { return this.settings; }
	public void setSettings(List<SettingEntry> settings) { this.settings = settings; }
	
    @Override public String toString() { return this.name_id; }

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
		dest.writeString(this.location_id);
		dest.writeList(settings);
	}

	@SuppressWarnings("unchecked")
	private void readFromParcel(Parcel in) {
		this.name_id = in.readString();
		this.location_id = in.readString();
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
    			if(this.location_id != null && this.name_id != null){
    				if ( this.location_id.compareTo(o.getLocationID()) == 0   ){
    					return o.getNameID().compareTo(this.name_id);
    					
    				}else{
    					return o.getLocationID().compareTo(this.location_id);
    				}
    			}
    			else 
    				throw new IllegalArgumentException();
    	}
}
