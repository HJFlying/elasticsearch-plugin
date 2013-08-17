package com.elasticsearch.custom.analysis;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.elasticsearch.index.settings.IndexSettings;

public class TopicAnalyzerProvider extends AbstractIndexAnalyzerProvider<TopicAnalyzer> {

	private final TopicAnalyzer analyzer;
	
	@Inject
	public TopicAnalyzerProvider(Index index, @IndexSettings Settings indexSettings, @Assisted String name, @Assisted Settings settings) {
		super(index, indexSettings, name, settings);
		analyzer = new TopicAnalyzer();
	}

	@Override
	public TopicAnalyzer get() {
		return this.analyzer;
	}

}
