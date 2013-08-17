package com.elasticsearch.custom.query.topic;

import java.io.IOException;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

final class TopicTermScorer extends Scorer {
	  private final DocsAndPositionsEnum postings;
	  private final Similarity.ExactSimScorer docScorer;
	  private final TopicQueryDetail termDetail;
	  private int docId = -1;
	  private float probability;
	  
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
	  TopicTermScorer(Weight weight, DocsAndPositionsEnum postings, Similarity.ExactSimScorer docScorer, TopicQueryDetail termDetail) {
	    super(weight);
	    this.docScorer = docScorer;
	    this.postings = postings;
	    this.termDetail = termDetail;
	  }

	  @Override
	  public int docID() {
		  return docId;
	  }

	  @Override
	  public int freq() throws IOException {
		  return termDetail.freq(probability);
	  }
	  
	  /**
	   * Advances to the next document matching the query. <br>
	   * After search , when you iterate the search result, you will call this method.
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
	    return docScorer.score(docID(), termDetail.freq(probability));  
	  }

	  /**
	   * Advances to the first match beyond the current whose document number is
	   * greater than or equal to a given target. <br>
	   * The implementation uses {@link DocsEnum#advance(int)}.
	   * It will be called when lucene computes score
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
				probability = PayloadHelper.decodeFloat(payload.bytes, payload.offset);
			} else {
				probability = postings.freq() / 100f;
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