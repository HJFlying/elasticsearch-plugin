package com.elasticsearch.custom.query.keyword;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.Similarity.ExactSimScorer;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

/** A Query that matches documents containing a term.
  This may be combined with other terms with a {@link BooleanQuery}.
  */
public class KeywordTermQuery extends Query {
	private final KeywordQueryDetail termDetail;
	private final Term term;
  
  private final int docFreq;
  private final TermContext perReaderTermState;
  
  final class TermWeight extends Weight {
    private final Similarity similarity;
    private final Similarity.SimWeight stats;
    private final TermContext termStates;
    
    public TermWeight(IndexSearcher searcher, TermContext termStates) throws IOException {
      assert termStates != null : "TermContext must not be null";
      this.termStates = termStates;
      this.similarity = searcher.getSimilarity();
      this.stats = similarity.computeWeight(
          getBoost(), 
          searcher.collectionStatistics(term.field()), 
          searcher.termStatistics(term, termStates));
    }

    @Override
    public String toString() { return "weight(" + KeywordTermQuery.this + ")"; }

    @Override
    public Query getQuery() { return KeywordTermQuery.this; }

    @Override
    public float getValueForNormalization() {
      return stats.getValueForNormalization() * termDetail.queryScore(1);
    }

    @Override
    public void normalize(float queryNorm, float topLevelBoost) {
      stats.normalize(queryNorm, topLevelBoost);
    }

    @Override
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
        boolean topScorer, Bits acceptDocs) throws IOException {
      assert termStates.topReaderContext == ReaderUtil.getTopLevelContext(context) : "The top-reader used to create Weight (" + termStates.topReaderContext + ") is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);
      
      final TermState state = termStates.get(context.ord);
      if (state == null) return null;
      
      final TermsEnum termsEnum = context.reader().terms(term.field()).iterator(null);
      termsEnum.seekExact(term.bytes(), state);
      final DocsAndPositionsEnum postings = termsEnum.docsAndPositions(acceptDocs, null, DocsAndPositionsEnum.FLAG_PAYLOADS);
      
      assert postings != null;
      return new KeywordTermScorer(this, postings, similarity.exactSimScorer(stats, context), termsEnum.docFreq(), termDetail);
    }
    
    @Override
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
      Scorer scorer = scorer(context, true, false, context.reader().getLiveDocs());
      if (scorer != null) {
        int newDoc = scorer.advance(doc);
        if (newDoc == doc) {
          float freq = scorer.freq();
          ExactSimScorer docScorer = similarity.exactSimScorer(stats, context);
          ComplexExplanation result = new ComplexExplanation();
          result.setDescription("weight("+getQuery()+" in "+doc+") [" + similarity.getClass().getSimpleName() + "], result of:");
          Explanation scoreExplanation = docScorer.explain(doc, new Explanation(freq, "termFreq=" + freq));
          result.addDetail(scoreExplanation);
          result.setValue(scoreExplanation.getValue());
          result.setMatch(true);
          return result;
        }
      }
      return new ComplexExplanation(false, 0.0f, "no matching term");      
    }
  }

  /** Constructs a query for the term <code>t</code>. */
  public KeywordTermQuery(KeywordQueryDetail termDetail) {
    this(termDetail, -1);
  }

  /** Expert: constructs a TermQuery that will use the
   *  provided docFreq instead of looking up the docFreq
   *  against the searcher. */
  public KeywordTermQuery(KeywordQueryDetail termDetail, int docFreq) {
	this.termDetail = termDetail;
	this.term = termDetail.term;
    this.docFreq = docFreq;
    perReaderTermState = null;
  }
  
  /** Expert: constructs a TermQuery that will use the
   *  provided docFreq instead of looking up the docFreq
   *  against the searcher. */
  public KeywordTermQuery(KeywordQueryDetail termDetail, TermContext states) {
    assert states != null;
    this.termDetail = termDetail;
    this.term = termDetail.term;
    
    docFreq = states.docFreq();
    perReaderTermState = states;
  }

  /** Returns the term of this query. */
  public Term getTerm() { return term; }

  @Override
  public Weight createWeight(IndexSearcher searcher) throws IOException {
    final IndexReaderContext context = searcher.getTopReaderContext();
    final TermContext termState;
    if (perReaderTermState == null || perReaderTermState.topReaderContext != context) {
      // make TermQuery single-pass if we don't have a PRTS or if the context differs!
      termState = TermContext.build(context, term, true); // cache term lookups!
    } else {
     // PRTS was pre-build for this IS
     termState = this.perReaderTermState;
    }

    // we must not ignore the given docFreq - if set use the given value (lie)
    if (docFreq != -1)
      termState.setDocFreq(docFreq);
    
    return new TermWeight(searcher, termState);
  }

  @Override
  public void extractTerms(Set<Term> terms) {
    terms.add(getTerm());
  }

  /** Prints a user-readable version of this query. */
  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    if (!term.field().equals(field)) {
      buffer.append(term.field());
      buffer.append(":");
    }
    buffer.append(term.text());
    buffer.append(ToStringUtils.boost(getBoost()));
    return buffer.toString();
  }

  /** Returns true iff <code>o</code> is equal to this. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof KeywordTermQuery))
      return false;
    KeywordTermQuery other = (KeywordTermQuery)o;
    return (this.getBoost() == other.getBoost())
      && this.term.equals(other.term);
  }

  /** Returns a hash code value for this object.*/
  @Override
  public int hashCode() {
    return Float.floatToIntBits(getBoost()) ^ term.hashCode();
  }

}
