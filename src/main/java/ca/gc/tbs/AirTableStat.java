package ca.gc.tbs;

import com.google.gson.annotations.SerializedName;

public class AirTableStat {
	private String id;

	@SerializedName("Page title")
	private String pageTitle;

	public AirTableStat(String title) {
		this.pageTitle = title;
	}
	
	public AirTableStat() {
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}
}
