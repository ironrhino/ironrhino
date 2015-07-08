package org.ironrhino.core.struts;

import org.ironrhino.core.util.ApplicationContextUtils;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.StringModel;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class PropertiesTemplateHashModel implements TemplateHashModel {

	@Override
	public TemplateModel get(String name) throws TemplateModelException {
		String str = ApplicationContextUtils.getApplicationContext().getEnvironment().getProperty(name);
		if (str == null)
			return null;
		BeansWrapper wrapper = new BeansWrapper(Configuration.VERSION_2_3_23);
		return new StringModel(str, wrapper);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException {
		return false;
	}

}
