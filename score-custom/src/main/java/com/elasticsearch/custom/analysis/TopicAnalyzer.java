package com.elasticsearch.custom.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class TopicAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		final Tokenizer source = new WhitespaceTokenizer(Version.LUCENE_43, reader);
		return new TokenStreamComponents(source, new TopicPayloadTokenFilter(source, TopicPayloadTokenFilter.DEFAULT_DELIMITER));
	}

	public static final class TopicPayloadTokenFilter extends TokenFilter {
		public static final char DEFAULT_DELIMITER = '|';
		private final char delimiter;
		private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
		private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
		
		protected TopicPayloadTokenFilter(TokenStream input, char delimiter) {
			super(input);
			this.delimiter = delimiter;
		}

		@Override
		public boolean incrementToken() throws IOException {
			if(input.incrementToken()){
				final char[] buffer = termAtt.buffer();
				final int length = termAtt.length();
				for(int i = 0; i < length; i++){
					if(buffer[i] == delimiter){
						float proability = Float.parseFloat(new String(buffer, i + 1, length - (i + 1)));
						byte[] data = PayloadHelper.encodeFloat(proability);
						payAtt.setPayload(new BytesRef(data));
						termAtt.setLength(i);
						return true;
					}
				}
				payAtt.setPayload(null);
				return true;
			}
			return false;
		}
	}
}
