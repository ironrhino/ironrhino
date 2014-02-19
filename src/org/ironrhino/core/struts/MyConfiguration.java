package org.ironrhino.core.struts;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class MyConfiguration extends Configuration {

	private Collection<OverridableTemplateProvider> overridableTemplateProviders;

	private Collection<FallbackTemplateProvider> fallbackTemplateProviders;

	public void setOverridableTemplateProviders(
			Collection<OverridableTemplateProvider> overridableTemplateProviders) {
		if (overridableTemplateProviders != null) {
			this.overridableTemplateProviders = overridableTemplateProviders;
			for (OverridableTemplateProvider overridableTemplateProvider : overridableTemplateProviders)
				overridableTemplateProvider.setConfiguration(this);
		}
	}

	public void setFallbackTemplateProviders(
			Collection<FallbackTemplateProvider> fallbackTemplateProviders) {
		if (fallbackTemplateProviders != null) {
			this.fallbackTemplateProviders = fallbackTemplateProviders;
			for (FallbackTemplateProvider fallbackTemplateProvider : fallbackTemplateProviders)
				fallbackTemplateProvider.setConfiguration(this);
		}
	}

	@Override
	public Template getTemplate(String name, Locale locale, String encoding,
			boolean parse) throws IOException {
		Template result = null;
		if (overridableTemplateProviders != null) {
			for (OverridableTemplateProvider overridableTemplateProvider : overridableTemplateProviders) {
				result = overridableTemplateProvider.getTemplate(name, locale,
						encoding, parse);
				if (result != null)
					break;
			}
		}
		if (result == null) {
			try {
				result = super.getTemplate(name, locale, encoding, parse);
			} catch (IOException e) {
				if (fallbackTemplateProviders != null) {
					for (FallbackTemplateProvider fallbackTemplateProvider : fallbackTemplateProviders) {
						result = fallbackTemplateProvider.getTemplate(name,
								locale, encoding, parse);
						if (result != null)
							break;
					}
					if (result == null)
						throw e;
				}
			}
		}
		return result;
	}

}
