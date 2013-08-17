package com.elasticsearch.custom.query.keyword;

import org.apache.lucene.index.Term;

/**
 * lezhi recommend score: 
 *   log(1+t.length) * dictidf(t) * (tfq(t) + fieldboost(fq)) * idf * (tfd(t) + fieldboost(fd)) * idf
 * first part goes to query: 
 *   log(1+t.length) * dictidf(t) * (tfq(t) + fieldboost(fq)) * idf
 * second part goes to each doc: 
 *   (tfd(t) + fieldboost(fd)) * idf
 * @author Brad
 *
 */
public class KeywordQueryDetail {
	public static final int[] DEFAULT_FIELD_BOOST = new int[] {1, 2, 6, 5};
	public Term term;
	public int[] fieldboost;
	public int fromfreq;
	public byte fromfield;
	public float dictidf;

	public KeywordQueryDetail(Term term, int[] fieldboost, int fromfreq, byte fromfield, float dictidf) {
		this.term = term;
		this.fieldboost = fieldboost;
		this.fromfreq = fromfreq;
		this.fromfield = fromfield;
		this.dictidf = dictidf;
	}
	
	@Override
	public String toString() {
		return term + "(fromfreq=" + fromfreq + ", fromfield=" + fromfield + ", dictidf=" + dictidf + ")";
	}
	
	public float queryScore(float idf) {
		return (float)Math.log(1 + term.text().length()) * dictidf * dictidf * (fromfreq + boost(fromfield)) * idf; 
	}
	
	public int docFreq(int docFreq, byte field) {
		return docFreq + boost(field);
	}
	
	private int boost(byte field) {
		if (field < fieldboost.length) return fieldboost[field];
		else return 1;
	}
	
	public static KeywordQueryDetail[] parse(KeywordInfo[] tis, int[] fieldboost, String indexField){
		KeywordQueryDetail[] termDetails = null;
        if(tis != null){
        	termDetails = new KeywordQueryDetail[tis.length];
        	for(int i = 0; i < tis.length; i++){
        		Term ft = new Term(indexField, tis[i].getTerm());
        		termDetails[i] = new KeywordQueryDetail(ft, fieldboost, tis[i].getFreq(), tis[i].getField(), (float) tis[i].getBoost());
        	}
        }	
        return termDetails;
	}
}