package by.zatta.pilight.model;

import android.os.Parcel;
import android.os.Parcelable;

public class OriginEntry implements Parcelable{
	
	private String name_id;
	private String change;
	private String value;
	
	public OriginEntry(){}
    	
	public String getNameID() 				{ return this.name_id; 		}
	public void setNameID(String name_id) 	{ this.name_id = name_id;	}
	
	public String getChange() 				{ return this.change;	}
	public void setChange(String change) { this.change = change; }
	
	public String getValue() 				{ return this.value;	}
	public void setValue(String value) { this.value = value; }

	
	
    @Override public String toString() { return this.name_id+":\n   " + change+": "+ value; }

    public OriginEntry(Parcel in) {
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
		dest.writeString(this.change);
		dest.writeString(this.value);
		
	}

	@SuppressWarnings("unchecked")
	private void readFromParcel(Parcel in) {
		this.name_id = in.readString();
		this.change = in.readString();
		this.value = in.readString();		
	}	
     
    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            @Override
			public OriginEntry createFromParcel(Parcel in) {
                return new OriginEntry(in);
            }
            @Override
			public OriginEntry[] newArray(int size) {
                return new OriginEntry[size];
            }
        };	
}
