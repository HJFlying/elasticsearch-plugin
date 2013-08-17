package com.elasticsearch.custom.query.keyword;

import java.io.IOException;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

final class KeywordTermScorer extends Scorer {
	  private final DocsAndPositionsEnum postings;
	  private final Similarity.ExactSimScorer docScorer;
	  private final int docFreq;
	  
	  private final KeywordQueryDetail termDetail;
	  
	  private int docId = -1;
	  private int freq;
	  private byte field;
	  
	  /**
	   * Construct a <code>TermScorer</code>.
	   * 
	   * @param weight
	   *          The weight of the <code>Term</code> in the query.
	   * @param docScorer
	   *          The </code>Similarity.ExactSimScorer</code> implementation 
	   *          to be used for score computations.
	   * @param docFreq
	   *          per-segment docFreq of this term
	   */
	  KeywordTermScorer(Weight weight, DocsAndPositionsEnum postings, Similarity.ExactSimScorer docScorer, int docFreq, KeywordQueryDetail termDetail) {
	    super(weight);
	    this.docScorer = docScorer;
	    this.postings = postings;
	    this.docFreq = docFreq;
	    
	    this.termDetail = termDetail;
	  }

	  @Override
	  public int docID() {
		  return docId;
	  }

	  @Override
	  public int freq() throws IOException {
		  return termDetail.docFreq(freq, field);
	  }
	  
	  /**
	   * Advances to the next document matching the query. <br>
	   * 
	   * @return the document matching the query or NO_MORE_DOCS if there are no more documents.
	   */
	  @Override
	  public int nextDoc() throws IOException {
	    postings.nextDoc();
	    return nextPositionPayload();
	  }
	  
	  @Override
	  public float score() throws IOException {
	    assert docID() != NO_MORE_DOCS;
	    return docScorer.score(docID(), termDetail.docFreq(freq, field));  
	  }

	  /**
	   * Advances to the first match beyond the current whose document number is
	   * greater than or equal to a given target. <br>
	   * The implementation uses {@link DocsEnum#advance(int)}.
	   * 
	   * @param target
	   *          The target document number.
	   * @return the matching document or NO_MORE_DOCS if none exist.
	   */
	  @Override
	  public int advance(int target) throws IOException {
		  postings.advance(target);
		  return nextPositionPayload();
	  }
	  
	  private int nextPositionPayload() throws IOException {
		  docId = postings.docID();
		if (docId != NO_MORE_DOCS) {
			postings.nextPosition();

			BytesRef payload = postings.getPayload();

			if (payload != null) {
				freq = PayloadHelper.decodeInt(payload.bytes, payload.offset);
				field = payload.bytes[payload.offset + 4];
			} else {
				freq = postings.freq();
				field = 0;
			}
		}
		return docId;
	  }

	  /** Returns a string representation of this <code>TermScorer</code>. */
	  @Override
	  public String toString() { return "LezhiTermScorer(" + weight + ")"; }
	  
	  // TODO: generalize something like this for scorers?
	  // even this is just an estimation...

    @Override
    public long cost() { return 1; }
}