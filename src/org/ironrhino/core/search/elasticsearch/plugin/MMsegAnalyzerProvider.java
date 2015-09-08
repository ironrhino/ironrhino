package org.ironrhino.core.search.elasticsearch.plugin;

import java.io.File;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.elasticsearch.index.settings.IndexSettings;

import com.chenlb.mmseg4j.analysis.MMSegAnalyzer;
import com.chenlb.mmseg4j.analysis.MaxWordAnalyzer;

/**
 * 
 * @author /medcl
 * @url https://github.com/medcl/elasticsearch-analysis-mmseg
 * 
 */
public class MMsegAnalyzerProvider extends AbstractIndexAnalyzerProvider<MMSegAnalyzer> {

	private final MMSegAnalyzer analyzer;

	@Inject
	public MMsegAnalyzerProvider(Index index, @IndexSettings Settings indexSettings, Environment env,
			@Assisted String name, @Assisted Settings settings) {
		super(index, indexSettings, name, settings);
		String path = new File(env.configFile(), "mmseg").getPath();
		analyzer = new MaxWordAnalyzer(path);
	}

	@Override
	public MMSegAnalyzer get() {
		return this.analyzer;
	}
}
