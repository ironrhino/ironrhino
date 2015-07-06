package org.ironrhino.core.struts;

import org.ironrhino.core.util.ApplicationContextUtils;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class BeansTemplateHashModel implements TemplateHashModel {

	@Override
	public TemplateModel get(String name) throws TemplateModelException {
		Object bean = ApplicationContextUtils.getBean(name);
		if (bean == null)
			return null;
		BeansWrapper wrapper = new BeansWrapper(Configuration.VERSION_2_3_23);
		return wrapper.wrap(bean);
	}

	@Override
	public boolean isEmpty() throws TemplateModelException {
		return false;
	}

}
