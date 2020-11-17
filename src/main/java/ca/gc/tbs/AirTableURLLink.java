package ca.gc.tbs;

import com.google.gson.annotations.SerializedName;

public class AirTableURLLink {
	private String id;

	@SerializedName("Name")
	private String URLlink;


	public AirTableURLLink(String urlLink) {
		this.URLlink = urlLink;
	}
	
	public AirTableURLLink() {
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	public String getURLlink() {
		return URLlink;
	}

	public void setURLlink(String uRLlink) {
		URLlink = uRLlink;
	}


}
