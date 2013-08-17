package com.elasticsearch.custom.api;

public class PrefixFilter {
	public String siteprefix;
//	public boolean _cache = false;
	
	public PrefixFilter() {}
	
	public PrefixFilter(String siteprefix) {
		this.siteprefix = siteprefix;
	}
}