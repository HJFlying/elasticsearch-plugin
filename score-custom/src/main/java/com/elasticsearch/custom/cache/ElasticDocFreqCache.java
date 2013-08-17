package com.elasticsearch.custom.cache;

import org.elasticsearch.common.cache.Cache;

public class ElasticDocFreqCache implements DocFreqCache {
	private Cache<String, Integer> realCache;
	private String indexName;
	
	public ElasticDocFreqCache(Cache<String, Integer> realCache, String indexName) {
		this.realCache = realCache;
		this.indexName = indexName;
	}

	@Override
	public int get(String term) {
		String key = indexName + "#" + term;
		Integer docFreq = realCache.getIfPresent(key);
		if (docFreq == null) return -1;
		else return docFreq;
	}

	@Override
	public void put(String term, int docFreq) {
		String key = indexName + "#" + term;
		realCache.put(key, docFreq);
	}
}