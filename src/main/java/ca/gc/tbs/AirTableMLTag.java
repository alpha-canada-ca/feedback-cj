package ca.gc.tbs;

import com.google.gson.annotations.SerializedName;

public class AirTableMLTag {
	private String id;

	@SerializedName("ML tags")
	private String tag;

	public AirTableMLTag(String tag) {
		this.tag = tag;
	}
	
	public AirTableMLTag() {
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}
