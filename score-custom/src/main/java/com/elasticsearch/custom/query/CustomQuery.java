package com.elasticsearch.custom.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SingleTermsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queries.function.valuesource.LongFieldSource;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.AttributeSource;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.elasticsearch.custom.cache.DocFreqCache;
import com.elasticsearch.custom.query.keyword.KeywordTermQuery;
import com.elasticsearch.custom.query.keyword.KeywordQueryDetail;
import com.elasticsearch.custom.query.topic.TopicQueryDetail;
import com.elasticsearch.custom.query.topic.TopicTermQuery;
import com.elasticsearch.custom.util.DoublePriorityQueue;

/**
 * score: 
 *   log(1+t.length) * dictidf(t) * (tfq(t) + fieldboost(fq)) * idf * (tfd(t) + fieldboost(fd)) * idf * timescore
 * first part goes to query: 
 *   log(1+t.length) * dictidf(t) * (tfq(t) + fieldboost(fq)) * idf
 * second part goes to each doc: 
 *   (tfd(t) + fieldboost(fd)) * idf
 * @author Brad
 *
 */
public class CustomQuery extends Query {
	private static ESLogger logger = Loggers.getLogger(CustomQuery.class);
	
	protected DocFreqCache cache;
	protected KeywordQueryDetail[] termDetails  = null;
	protected TopicQueryDetail[] topicDetails = null;
	protected String signature = null;
	protected LongFieldSource timeSrc = new LongFieldSource("lastModified");
	protected int maxTerms;
	protected long refTime;
	
  /**
   * Constructs a query matching terms that cannot be represented with a single
   * Term.
   */
  public CustomQuery(DocFreqCache cache, KeywordQueryDetail[] termDetails, TopicQueryDetail[] topicDetails, String signature, long refTime, int maxTerms) {
	this.cache = cache;
    this.termDetails = termDetails;
    this.topicDetails = topicDetails;
    this.signature = signature;
    this.refTime = refTime;
    this.maxTerms = maxTerms;
    assert termDetails != null;
  }

  /**
   * To rewrite to a simpler form, instead return a simpler
   * enum from {@link #getTermsEnum(Terms, AttributeSource)}.  For example,
   * to rewrite to a single term, return a {@link SingleTermsEnum}
   */
  @Override
	public final Query rewrite(IndexReader reader) throws IOException {
		List<KeywordQueryDetail> queryTerms = new ArrayList<KeywordQueryDetail>();
	
		if(termDetails != null){
			DoublePriorityQueue<KeywordQueryDetail> topTermQueue = new DoublePriorityQueue<KeywordQueryDetail>(maxTerms);
			DoublePriorityQueue<KeywordQueryDetail> termQueue = new DoublePriorityQueue<KeywordQueryDetail>(maxTerms);
			int numDocs = reader.numDocs();
			for (KeywordQueryDetail termDetail : termDetails) {
				int docFreq = cache.get(termDetail.term.text());
				if (docFreq == -1) docFreq = reader.docFreq(termDetail.term);
				if (docFreq >= 100) {
					float idf = (float) Math.log(numDocs / (docFreq + 1.0));
					double score = termDetail.queryScore(idf) * termDetail.docFreq(termDetail.fromfreq, termDetail.fromfield);
					termQueue.add(score, termDetail);
					if (termDetail.fromfreq >= 3 || termDetail.fromfield >= 1) topTermQueue.add(score, termDetail);
					cache.put(termDetail.term.text(), docFreq);
				} else {
					if (termDetail.fromfreq >= 3 || termDetail.fromfield >= 1) queryTerms.add(termDetail);
				}
			}
			List<KeywordQueryDetail> topTerms = topTermQueue.values();
			if (topTerms.size() * 2 < maxTerms) topTerms = termQueue.values();
			queryTerms.addAll(topTerms);
			if (logger.isDebugEnabled()) logger.debug(queryTerms.toString());
		}
		
		

		BooleanQuery q = new BooleanQuery();
		for (KeywordQueryDetail term : queryTerms) {
			q.add(new KeywordTermQuery(term), Occur.SHOULD);
		}
		
		//TODO: add boost for topic
		if(topicDetails != null){
			for(TopicQueryDetail topicDetail : topicDetails){
				Query query = new TopicTermQuery(topicDetail);
//				query.setBoost();
				q.add(query, Occur.SHOULD);
			}
		}
		
		if (signature != null) q.add(new TermQuery(new Term("signature", signature)), Occur.MUST_NOT);
		return new RecencyBoostQuery(q, refTime, new LongFieldSource("lastModified"));
	}
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Float.floatToIntBits(getBoost());
    for (KeywordQueryDetail termDetail: termDetails) result = prime * result + termDetail.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CustomQuery other = (CustomQuery) obj;
    if (Float.floatToIntBits(getBoost()) != Float.floatToIntBits(other.getBoost()))
      return false;
    return Arrays.equals(termDetails, other.termDetails);
  }

  @Override
  public String toString(String field) {
	return "LezhiQuery[keywords" + Arrays.toString(termDetails) + ", topic:" + Arrays.toString(topicDetails) + "]";
  }
}