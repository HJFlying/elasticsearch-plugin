package com.elasticsearch.custom.filter;

import org.apache.lucene.codecs.bloom.FuzzySet;
import org.apache.lucene.codecs.bloom.FuzzySet.ContainsResult;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;

import java.io.IOException;

/**
 * Filter to remove duplicate values from search results.
 * <p>
 * WARNING: for this to work correctly, you may have to wrap
 * your reader as it cannot current deduplicate across different
 * index segments.
 * 
 * @see SlowCompositeReaderWrapper
 */
public class FuzzyDuplicateFilter extends Filter {
  // TODO: make duplicate filter aware of ReaderContext such that we can
  // filter duplicates across segments
  private String fieldName;
  private FuzzySet fieldvalues = FuzzySet.createSetBasedOnQuality(100 * 10000, 0.10f);

  public FuzzyDuplicateFilter(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
    return correctBits(context.reader(), acceptDocs);
  }

  private FixedBitSet correctBits(AtomicReader reader, Bits acceptDocs) throws IOException {
    FixedBitSet bits = new FixedBitSet(reader.maxDoc()); //assume all are INvalid
    Terms terms = reader.fields().terms(fieldName);

    if (terms == null) {
      return bits;
    }

    TermsEnum termsEnum = terms.iterator(null);
    DocsEnum docs = null;
    while (true) {
      BytesRef currTerm = termsEnum.next();
      if (currTerm == null) {
        break;
      } else {
        docs = termsEnum.docs(acceptDocs, docs, 0);
        int doc = docs.nextDoc();
        if (doc != DocIdSetIterator.NO_MORE_DOCS) {
          if(fieldvalues.contains(currTerm) == ContainsResult.NO) {
        	  bits.set(doc);
        	  fieldvalues.addValue(currTerm);
          }
        }
      }
    }
    return bits;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (obj.getClass() != this.getClass())) {
      return false;
    }

    FuzzyDuplicateFilter other = (FuzzyDuplicateFilter) obj;
    return fieldName != null && fieldName.equals(other.fieldName);
  }

  @Override
  public int hashCode() {
	return fieldName.hashCode();
  }
}