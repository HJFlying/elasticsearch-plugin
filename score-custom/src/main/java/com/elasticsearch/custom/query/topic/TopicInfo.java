package com.elasticsearch.custom.query.topic;

import com.elasticsearch.custom.util.StringUtils;

public class TopicInfo {
	private String term;
	private float probability;
	
	public TopicInfo(String term, float probability){
		this.term = term;
		this.probability = probability;
	}

	public String getTerm() {
		return term;
	}

	public float getProbability() {
		return probability;
	}
	
	public static TopicInfo[] parse(String text) {
		String[] parts = StringUtils.split(text, ' ');
		TopicInfo[] topics = new TopicInfo[parts.length];
		for (int i = 0; i < topics.length; i++) {
			String[] attr = StringUtils.split(parts[i], '|');
			topics[i] = new TopicInfo(attr[0],  Float.parseFloat(attr[1]));
		}
		return topics;
	}

	@Override
	public String toString() {
		return term + "|" + probability;
	}
	
	public static void main(String[] args) {
		TopicInfo[] tis = TopicInfo.parse("1|0.1 2|0.3 123|0.01 234|0.8");
		for (TopicInfo ti: tis) System.out.println(ti);
	}
	
}
