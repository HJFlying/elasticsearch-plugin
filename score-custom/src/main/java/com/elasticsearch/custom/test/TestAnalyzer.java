package com.elasticsearch.custom.test;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;

import com.elasticsearch.custom.analysis.TopicAnalyzer;

public class TestAnalyzer {
    public static void main(String[] args) throws IOException {
//        Analyzer analyzer = new SnippetAnalyzer();
//        TokenStream ts = analyzer.tokenStream("text", new StringReader("中新网 社区 | 中国 媒体 | 朝鲜 赌注 | 高 | 中国 | 适当 惩罚"));
//        ts.reset();
//        while (ts.incrementToken()) {
////            System.out.println(StringUtils.join(ts.getAttributeClassesIterator(), ""));
//            CharTermAttribute ct = ts.getAttribute(CharTermAttribute.class);
//            PositionIncrementAttribute pt = ts.getAttribute(PositionIncrementAttribute.class);
//            System.out.println(ct.toString() + " => " + pt.getPositionIncrement());
//        }
        
        Analyzer analyzer = new TopicAnalyzer();
        TokenStream ts = analyzer.tokenStream("text", new StringReader("12|0.11 452|0.22 22|0.898 1|0.21212"));
        ts.reset();
        while (ts.incrementToken()) {
            CharTermAttribute ct = ts.getAttribute(CharTermAttribute.class);
            PayloadAttribute payAtt = ts.getAttribute(PayloadAttribute.class);
            System.out.println(ct.toString() + " => " + PayloadHelper.decodeFloat(payAtt.getPayload().bytes, payAtt.getPayload().offset));
        }
    }
}
