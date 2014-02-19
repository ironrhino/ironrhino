package org.ironrhino.core.search.elasticsearch.plugin;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.plugins.AbstractPlugin;

/**
 * 
 * @author /medcl
 * @url https://github.com/medcl/elasticsearch-analysis-mmseg
 * 
 */
public class AnalysisMMsegPlugin extends AbstractPlugin {

	@Override
	public String name() {
		return "analysis-mmseg";
	}

	@Override
	public String description() {
		return "mmseg analysis";
	}

	@Override
	public void processModule(Module module) {
		if (module instanceof AnalysisModule) {
			AnalysisModule analysisModule = (AnalysisModule) module;
			analysisModule.addProcessor(new MMsegAnalysisBinderProcessor());
		}
	}
}
