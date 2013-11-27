package by.zatta.pilight.model;

import android.os.Parcel;
import android.os.Parcelable;

public class SettingEntry implements Parcelable{
    private String key;
	private String value;
	
	public SettingEntry(){}
    public SettingEntry(String key, String value) {
    	this.key = key;
    	this.value = value;
    }

	public String getKey() 				{ return this.key; }
	public void setKey(String key)		{ this.key = key; }
	
	public String getValue()	 		{ return this.value; }
	public void setValue(String value)	{ this.value = value; }
    
    @Override public String toString()	{ return this.key + " = " + this.value; }

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
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
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
