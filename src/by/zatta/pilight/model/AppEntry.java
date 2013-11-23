package by.zatta.pilight.model;

import java.io.File;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class AppEntry implements Comparable<AppEntry>, Parcelable{
	
    private ApplicationInfo mInfo;
    private File mApkFile;
    private String mLabel;
    private String mPackName;
    private boolean mMounted;
    private String mCacheBool;
    private String mDataBool;
    private long mCacheSize;
    private long mTotalSize;
    private String mType;
    
    public AppEntry(ApplicationInfo info, String cache_to_data, String all_to_data) {
    	mCacheBool = cache_to_data;
    	mDataBool = all_to_data;
        mInfo = info;
        mApkFile = new File(info.sourceDir);
    }
    
    public void setCacheSize(String cache_size){
    	mCacheSize = Long.valueOf(cache_size)*1024;
    }
    
    public void setTotalSize(String total_size){
    	mTotalSize = Long.valueOf(total_size)*1024;
    }
    
    
    
    public long getCacheSize(){
    	return mCacheSize;
    }
    public long getTotalSize(){
    	return mTotalSize;
    }

    public void setCacheBool(String cache_to_data){
    	mCacheBool = cache_to_data;
    }
    
    public void setDataBool(String all_to_data){
    	mDataBool = all_to_data;
    }

	public ApplicationInfo getApplicationInfo() {
        return mInfo;
    }
	
	public String getCacheBool(){
		return mCacheBool;
	}
	
	public String getDataBool(){
		return mDataBool;
	}

    public String getLabel() {
        return mLabel;
    }
    
    public String getPackName(){
    	return mPackName;
    }
    
    public String getType(){
    	return mType;
    }

    
    
    
    @Override public String toString() {
        return mLabel;
    }

    void loadLabel(Context context) {
        if (mLabel == null || !mMounted) {
            if (!mApkFile.exists()) {
                mMounted = false;
                mLabel = mInfo.packageName;
                mPackName = "NOT INSTALLED";
                mType = "NOT INSTALLED";
                
                
            } else {
                mMounted = true;
                CharSequence label = mInfo.loadLabel(context.getPackageManager());
                mLabel = label != null ? label.toString() : mInfo.packageName;
                mPackName = mInfo.packageName;
                if (mInfo.sourceDir.contains("/system/app"))
                	mType = "system";
                else mType = "user";
     
                
            }
        }
    }
    
    
    
  
    

    public AppEntry(Parcel in) {
		readFromParcel(in);
	}
  
	@Override
	public int describeContents() {
		return 0;
	}
 
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mCacheBool);
		dest.writeString(mDataBool);
		dest.writeString(mLabel);
		dest.writeString(mPackName);
	}

	private void readFromParcel(Parcel in) {
		mCacheBool = in.readString();
		mDataBool = in.readString();
		mLabel = in.readString();
 		mPackName = in.readString();
	}
     
    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            @Override
			public AppEntry createFromParcel(Parcel in) {
                return new AppEntry(in);
            }
 
            @Override
			public AppEntry[] newArray(int size) {
                return new AppEntry[size];
            }
        };	

        @Override
    	public int compareTo(AppEntry o) {
    		
    			if(this.mType != null && Long.toString(this.mTotalSize) != null){
    				if ( this.mType.toLowerCase().compareTo(o.getType().toLowerCase()) == 0   ){
    					int num = 0;
    					if (this.mTotalSize < o.mTotalSize ) num = 1; 
    					if (this.mTotalSize == o.mTotalSize ) num =  0;
    					if (this.mTotalSize > o.mTotalSize ) num =  -1;
    					return num;
    					
    				}else{
    					
    					return o.getType().compareTo(this.mType);
    				}
    			}
    			else 
    				throw new IllegalArgumentException();
    	}
}
