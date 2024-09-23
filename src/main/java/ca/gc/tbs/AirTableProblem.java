package ca.gc.tbs;
import com.google.gson.annotations.SerializedName;

public class AirTableProblem {
	private String id;
	
	@SerializedName("Unique ID")
	private String uniqueID;
	 
	@SerializedName("Date")
	private String date;
	@SerializedName("Time received")
	private String timeStamp;
	@SerializedName("URL")
	private String URL;
	@SerializedName("Name")
	private String URL_link;
	@SerializedName("Page title")
	private String pageTitle;
	@SerializedName("Lang")
	private String lang;
	@SerializedName("What's wrong")
	private String whatswrong;
	@SerializedName("Details")
	private String details;
	@SerializedName("Tags")
	private String tags;
	@SerializedName("Info exists")
	private String infoExists;
	@SerializedName("PII")
	private String PII;
	
	@SerializedName("PII Type")
	private String PIIType;
	
	
	@SerializedName("Topic - HC")
	private String topic;
	@SerializedName("Actionable")
	private Boolean actionable;
	
	
	
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getURL() {
		return URL;
	}
	public void setURL(String uRL) {
		URL = uRL;
	}
	public String getPageTitle() {
		return pageTitle;
	}
	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
	public String getWhatswrong() {
		return whatswrong;
	}
	public void setWhatswrong(String whatswrong) {
		this.whatswrong = whatswrong;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}
	public String getInfoExists() {
		return infoExists;
	}
	public void setInfoExists(String infoExists) {
		this.infoExists = infoExists;
	}
	public String getPII() {
		return PII;
	}
	public void setPII(String pII) {
		PII = pII;
	}
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getURL_link() {
		return URL_link;
	}
	public void setURL_link(String uRL_link) {
		URL_link = uRL_link;
	}
	public String getUniqueID() {
		return uniqueID;
	}
	public void setUniqueID(String uniqueID) {
		this.uniqueID = uniqueID;
	}
	public String getPIIType() {
		return PIIType;
	}
	public void setPIIType(String pIIType) {
		PIIType = pIIType;
	}
	
	
}
