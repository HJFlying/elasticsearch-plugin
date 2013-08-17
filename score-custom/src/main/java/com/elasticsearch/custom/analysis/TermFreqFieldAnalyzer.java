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

public class TermFreqFieldAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		final Tokenizer source = new WhitespaceTokenizer(Version.LUCENE_43, reader);
		return new TokenStreamComponents(source, new FreqFieldPayloadTokenFilter(source, FreqFieldPayloadTokenFilter.DEFAULT_DELIMITER));
	}

	public static final class FreqFieldPayloadTokenFilter extends TokenFilter {
		public static final char DEFAULT_DELIMITER = '|';
		private final char delimiter;
		private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
		private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);

		public FreqFieldPayloadTokenFilter(TokenStream input, char delimiter) {
			super(input);
			this.delimiter = delimiter;
		}

		@Override
		public boolean incrementToken() throws IOException {
			if (input.incrementToken()) {
				final char[] buffer = termAtt.buffer();
				final int length = termAtt.length();
				for (int i = 0; i < length; i++) {
					if (buffer[i] == delimiter) {
						int j;
						for (j = i + 1; j < length; j++) {
							if (buffer[j] == ',') break;
						}
						int freq = Integer.parseInt(new String(buffer, i + 1, j - (i + 1)));
						byte field = Byte.parseByte(new String(buffer, j + 1, length - (j + 1)));
						byte[] data = new byte[5];
						PayloadHelper.encodeInt(freq, data, 0);
						data[4] = field;
						payAtt.setPayload(new BytesRef(data));

						termAtt.setLength(i); // simply set a new length
						return true;
					}
				}
				// we have not seen the delimiter
				payAtt.setPayload(null);
				return true;
			} else return false;
		}
	}
}