package org.ironrhino.core.freemarker;

import java.util.Map;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperConfiguration;
import freemarker.ext.util.ModelFactory;
import freemarker.template.Version;

@SuppressWarnings("rawtypes")
public class MyBeansWrapper extends BeansWrapper {

	public MyBeansWrapper(Version incompatibleImprovements) {
		super(new BeansWrapperConfiguration(incompatibleImprovements) {
		}, false);
	}

	protected MyBeansWrapper(BeansWrapperConfiguration bwConf, boolean writeProtected) {
		super(bwConf, writeProtected, true);
	}

	protected ModelFactory getModelFactory(Class clazz) {
		if (Map.class.isAssignableFrom(clazz)) {
			return MyMapModel.FACTORY;
		}
		return super.getModelFactory(clazz);
	}
}
