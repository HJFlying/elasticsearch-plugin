package com.elasticsearch.custom.query.topic;

import org.apache.lucene.index.Term;

public class TopicQueryDetail{
	public Term term;
	public float fromProbability;
	
	public TopicQueryDetail(Term term, float fromProbability){
		this.term = term;
		this.fromProbability = fromProbability;
	}
	
	public int freq(float probability){
		return (int) Math.round(probability * 100);
	}
	
	public float queryScore() {
		return (float) Math.log(1 + fromProbability); 
	}
	
	@Override
	public String toString() {
		return term + "(fromProbability=" + fromProbability + ")";
	}
	
	public static TopicQueryDetail[] parse(TopicInfo[] topics, String indexField){
		TopicQueryDetail[] topicDetails = null;
	       if(topics != null){
	           topicDetails = new TopicQueryDetail[topics.length];
	           for(int i = 0; i < topics.length; i++){
	        		Term ft = new Term(indexField, topics[i].getTerm());
	        		topicDetails[i] = new TopicQueryDetail(ft, topics[i].getProbability());
	        	}
	    }
	    return topicDetails;
	}
}
