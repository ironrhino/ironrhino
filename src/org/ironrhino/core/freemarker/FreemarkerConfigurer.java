package org.ironrhino.core.freemarker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import freemarker.cache.StrongCacheStorage;
import freemarker.core.ParseException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import lombok.Getter;

@Component
public class FreemarkerConfigurer {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String DEFAULT_FTL_CLASSPATH = "/resources/view";

	public static final Version DEFAULT_VERSION = Configuration.VERSION_2_3_26;

	public static final BeansWrapper DEFAULT_BEANS_WRAPPER = new FriendlyBeansWrapperBuilder(DEFAULT_VERSION).build();

	public static final String KEY_BASE = "base";
	public static final String KEY_STATICS = "statics";
	public static final String KEY_ENUMS = "enums";
	public static final String KEY_CONSTANTS = "constants";
	public static final String KEY_BEANS = "beans";
	public static final String KEY_PROPERTIES = "properties";
	public static final String KEY_DEV_MODE = "devMode";
	public static final String KEY_FLUID_LAYOUT = "fluidLayout";
	public static final String KEY_SIDEBAR_LAYOUT = "sidebarLayout";
	public static final String KEY_FROZEN_LAYOUT = "frozenLayout";
	public static final String KEY_MODERN_BROWSER = "modernBrowser";

	@Getter
	@Value("${freemarker.ftl.classpath:" + DEFAULT_FTL_CLASSPATH + "}")
	private String ftlClasspath = DEFAULT_FTL_CLASSPATH;

	@Value("${freemarker.defaultEncoding:UTF-8}")
	private String defaultEncoding = "UTF-8";

	@Value("${freemarker.templateUpdateDelay:" + Integer.MAX_VALUE + "}")
	private int templateUpdateDelay = Integer.MAX_VALUE;

	@Value("${freemarker.mruMaxStrongSize:0}")
	private int mruMaxStrongSize = 0;

	@Getter
	@Value("${base:}")
	private String base;

	@Getter
	@Value("${assetsBase:}")
	private String assetsBase;

	@Getter
	@Value("${ssoServerBase:}")
	private String ssoServerBase;

	@Getter
	@Value("${layout.fluid:true}")
	private boolean fluidLayout = true;

	@Getter
	@Value("${layout.sidebar:false}")
	private boolean sidebarLayout = false;

	@Getter
	@Value("${layout.frozen:false}")
	private boolean frozenLayout = false;

	@Autowired(required = false)
	private List<OverridableTemplateProvider> overridableTemplateProviders;

	@Autowired(required = false)
	private List<FallbackTemplateProvider> fallbackTemplateProviders;

	private List<Configuration> configurations = new CopyOnWriteArrayList<>();

	@PostConstruct
	public void init() {
		ftlClasspath = org.ironrhino.core.util.StringUtils.trimTailSlash(ftlClasspath);
		if (StringUtils.isNotBlank(base))
			base = org.ironrhino.core.util.StringUtils.trimTailSlash(base);
		if (StringUtils.isNotBlank(assetsBase))
			assetsBase = org.ironrhino.core.util.StringUtils.trimTailSlash(assetsBase);
		if (StringUtils.isNotBlank(ssoServerBase))
			ssoServerBase = org.ironrhino.core.util.StringUtils.trimTailSlash(ssoServerBase);
	}

	public Configuration createConfiguration() throws TemplateException {
		Configuration configuration = new Configuration(DEFAULT_VERSION) {
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
					result = super.getTemplate(name, locale, customLookupCondition, encoding, parseAsFTL,
							ignoreMissing);
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
		};
		setFormats(configuration);
		setEncodings(configuration);
		setStorage(configuration);
		setAllSharedVariables(configuration);
		configuration.setObjectWrapper(DEFAULT_BEANS_WRAPPER);
		configuration.setTemplateExceptionHandler(
				AppInfo.getStage() == Stage.DEVELOPMENT ? TemplateExceptionHandler.HTML_DEBUG_HANDLER
						: (ex, env, writer) -> {
							logger.error(ex.getMessage());
						});
		configurations.add(configuration);
		return configuration;
	}

	@Trigger(scope = Scope.APPLICATION)
	public void clearTemplateCache() {
		for (Configuration configuration : configurations)
			configuration.clearTemplateCache();
	}

	protected void setFormats(Configuration configuration) throws TemplateModelException {
		configuration.setDateFormat("yyyy-MM-dd");
		configuration.setTimeFormat("HH:mm:ss");
		configuration.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
		configuration.setNumberFormat("0.##");
		configuration.setWhitespaceStripping(true);
	}

	protected void setEncodings(Configuration configuration) throws TemplateModelException {
		configuration.setDefaultEncoding(defaultEncoding);
		configuration.setURLEscapingCharset(defaultEncoding);
	}

	protected void setStorage(Configuration configuration) throws TemplateException {
		configuration.setCacheStorage(new StrongCacheStorage());
		if (mruMaxStrongSize > 0)
			configuration.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:" + mruMaxStrongSize);
		configuration.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY,
				String.valueOf(AppInfo.getStage() == Stage.DEVELOPMENT ? 5 : templateUpdateDelay));
	}

	protected void setAllSharedVariables(Configuration configuration) throws TemplateModelException {
		Map<String, Object> allSharedVariables = new HashMap<>(8);
		if (StringUtils.isNotBlank(base))
			allSharedVariables.put("base", base);
		if (StringUtils.isNotBlank(assetsBase))
			allSharedVariables.put("assetsBase", assetsBase);
		if (StringUtils.isNotBlank(ssoServerBase))
			allSharedVariables.put("ssoServerBase", ssoServerBase);
		allSharedVariables.put(KEY_FLUID_LAYOUT, fluidLayout);
		allSharedVariables.put(KEY_SIDEBAR_LAYOUT, sidebarLayout);
		allSharedVariables.put(KEY_FROZEN_LAYOUT, frozenLayout);
		allSharedVariables.put(KEY_MODERN_BROWSER, true); // drop legacy browser supports
		allSharedVariables.put(KEY_DEV_MODE, AppInfo.getStage() == Stage.DEVELOPMENT);
		allSharedVariables.put(KEY_STATICS, DEFAULT_BEANS_WRAPPER.getStaticModels());
		allSharedVariables.put(KEY_ENUMS, DEFAULT_BEANS_WRAPPER.getEnumModels());
		allSharedVariables.put(KEY_CONSTANTS, new ConstantsTemplateHashModel());
		allSharedVariables.put(KEY_BEANS, new BeansTemplateHashModel());
		allSharedVariables.put(KEY_PROPERTIES, new PropertiesTemplateHashModel());
		TemplateHashModelEx hash = new SimpleMapModel(allSharedVariables, DEFAULT_BEANS_WRAPPER);
		configuration.setAllSharedVariables(hash);
	}

}
