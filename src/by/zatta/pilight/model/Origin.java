package by.zatta.pilight.model;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Origin {
	
	private static final String TAG = "Origin";
	
	private static OriginEntry originEntry = new OriginEntry();
	
	public static OriginEntry getOriginEntry() {
		return originEntry;
	}
	
	public static OriginEntry getOriginEntry(JSONObject jMessage) {
		parse(jMessage);
		return originEntry;
	}
	
	public static void parse(JSONObject jMessage) {
		//Log.d(TAG, jMessage.toString());
		
		Iterator<?> fit = jMessage.keys();
		/* Iterate through all entries */
		while(fit.hasNext()) {
			String firstKey = (String)fit.next();
			try {				
				JSONObject jSecond = jMessage.getJSONObject(firstKey);
				//Log.d(TAG, "firstKey: " + firstKey); //devices, values
				//Log.d(TAG, "jSecond: "+ jSecond.toString()); // {"living":["KamerTemp"]}  &  {"temperature":"20062"}

				if(firstKey.equals("devices")) {
					Iterator<?> sit = jSecond.keys();
					while(sit.hasNext()){
						String secondKey = (String)sit.next();
						JSONArray jSecArr = jSecond.optJSONArray(secondKey);
						String jSecStr = jSecond.optString(secondKey);
					
						if(jSecArr != null) {
							/* Iterate through all values for this setting */
							for(Short i=0; i<jSecArr.length(); i++) {
								//Log.d(TAG, "Uit array:" + jSecArr.get(i).toString());
								originEntry.setNameID(jSecArr.get(i).toString());			
							}
						} else if (jSecStr != null) {
							//Log.e(TAG, "Uit string: " + jSecStr);
							originEntry.setNameID(jSecStr);
						}
					}
				}
				if (firstKey.equals("values")){
					Iterator<?> sit = jSecond.keys();
					while(sit.hasNext()){
						String secondKey = (String)sit.next();
						JSONArray jSecArr = jSecond.optJSONArray(secondKey);
						String jSecStr = jSecond.optString(secondKey);
					
						if(jSecArr != null) {
							/* Iterate through all values for this setting */
							for(Short i=0; i<jSecArr.length(); i++) {
								//Log.d(TAG, "Uit array:" + jSecArr.get(i).toString());
								originEntry.setValue(jSecArr.get(i).toString());
								originEntry.setChange(secondKey);
							}
						} else if (jSecStr != null) {
							//Log.d(TAG, "Uit string: " + jSecStr);
							originEntry.setValue(jSecStr);
							originEntry.setChange(secondKey);
						}
					}
				}
			} catch (JSONException e) {}
		}
	}
}
