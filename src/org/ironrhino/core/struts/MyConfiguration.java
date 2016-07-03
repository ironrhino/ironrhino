package org.ironrhino.core.struts;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;

public class MyConfiguration extends Configuration {

	private Collection<OverridableTemplateProvider> overridableTemplateProviders;

	private Collection<FallbackTemplateProvider> fallbackTemplateProviders;

	@Deprecated
	public MyConfiguration() {
		super();
	}

	public MyConfiguration(Version version) {
		super(version);
	}

	public void setOverridableTemplateProviders(Collection<OverridableTemplateProvider> overridableTemplateProviders) {
		if (overridableTemplateProviders != null) {
			this.overridableTemplateProviders = overridableTemplateProviders;
			for (OverridableTemplateProvider overridableTemplateProvider : overridableTemplateProviders)
				overridableTemplateProvider.setConfiguration(this);
		}
	}

	public void setFallbackTemplateProviders(Collection<FallbackTemplateProvider> fallbackTemplateProviders) {
		if (fallbackTemplateProviders != null) {
			this.fallbackTemplateProviders = fallbackTemplateProviders;
			for (FallbackTemplateProvider fallbackTemplateProvider : fallbackTemplateProviders)
				fallbackTemplateProvider.setConfiguration(this);
		}
	}

	@Override
	public Template getTemplate(String name, Locale locale, Object customLookupCondition, String encoding,
			boolean parseAsFTL, boolean ignoreMissing)
			throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
		Template result;
		if (overridableTemplateProviders != null) {
			for (OverridableTemplateProvider overridableTemplateProvider : overridableTemplateProviders) {
				result = overridableTemplateProvider.getTemplate(name, locale, encoding, parseAsFTL);
				if (result != null)
					return result;
			}
		}
		try {
			result = super.getTemplate(name, locale, customLookupCondition, encoding, parseAsFTL, ignoreMissing);
			if (result != null)
				return result;
			if (fallbackTemplateProviders != null) {
				for (FallbackTemplateProvider fallbackTemplateProvider : fallbackTemplateProviders) {
					result = fallbackTemplateProvider.getTemplate(name, locale, encoding, parseAsFTL);
					if (result != null)
						return result;
				}
			}
		} catch (TemplateNotFoundException e) {
			if (fallbackTemplateProviders != null) {
				for (FallbackTemplateProvider fallbackTemplateProvider : fallbackTemplateProviders) {
					result = fallbackTemplateProvider.getTemplate(name, locale, encoding, parseAsFTL);
					if (result != null)
						return result;
				}
			}
			throw e;
		}
		return null;
	}

}
