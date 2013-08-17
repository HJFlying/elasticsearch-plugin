package com.elasticsearch.custom.api;

public class HitDoc {
	public String url;
	public String title;
	public String signature;
	public String thumbnail;
	public long lastModified;
	public float score;
	
	public HitDoc() {}
	
	public HitDoc(String url, String title, String signature, String thumbnail, long lastModified) {
		this.url = url;
		this.title = title;
		this.signature = signature;
		this.thumbnail = thumbnail;
		this.lastModified = lastModified;
	}

	@Override
	public String toString() {
		return url + " /" + score + " [title=" + title + ", thumbnail=" + thumbnail + ", lastModified=" + lastModified + "]";
	}
}