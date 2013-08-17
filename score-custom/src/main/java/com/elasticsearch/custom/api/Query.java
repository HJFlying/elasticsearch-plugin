package com.elasticsearch.custom.api;

public class Query {
	public String signature;
	public String keyword;
	public String topic;
	public long reftime;
	
	public Query() {}
	
	public Query(String signature, String keyword, String topic, long reftime) {
		this.signature = signature;
		this.keyword = keyword;
		this.topic = topic;
		this.reftime = reftime;
	}
}