package org.ironrhino.core.freemarker;

import java.util.Map;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperConfiguration;
import freemarker.ext.util.ModelFactory;
import freemarker.template.Version;

public class FriendlyBeansWrapper extends BeansWrapper {

	public FriendlyBeansWrapper(Version incompatibleImprovements) {
		super(new BeansWrapperConfiguration(incompatibleImprovements) {
		}, false);
	}

	protected FriendlyBeansWrapper(BeansWrapperConfiguration bwConf, boolean writeProtected) {
		super(bwConf, writeProtected, true);
	}

	@Override
	protected ModelFactory getModelFactory(Class<?> clazz) {
		if (Map.class.isAssignableFrom(clazz)) {
			return FriendlyMapModel.FACTORY;
		}
		return super.getModelFactory(clazz);
	}
}
