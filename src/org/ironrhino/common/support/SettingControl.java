package org.ironrhino.common.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Order;
import org.ironrhino.common.model.Setting;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.spring.configuration.PropertySourceMode;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SettingControl {

	public static final String SETTING_KEY_SETUP_DATETIME = "setup.datetime";
	public static final String SETTING_KEY_SETUP_TIMESTAMP = "setup.timestamp";

	@Autowired
	private ConfigurableEnvironment environment;

	@Value("${settingControl.propertySourceMode:}")
	private PropertySourceMode propertySourceMode;

	@Autowired
	private EntityManager<Setting> entityManager;

	private volatile Map<String, Setting> settings;

	private Map<String, Setting> getRequiredSettings() {
		Map<String, Setting> temp = settings;
		if (temp == null)
			settings = temp = reload();
		return temp;
	}

	private Map<String, Setting> reload() {
		entityManager.setEntityClass(Setting.class);
		List<Setting> list = entityManager.findAll(Order.asc("key"));
		Map<String, Setting> temp = new HashMap<>();
		for (Setting s : list)
			temp.put(s.getKey(), s);
		return temp;
	}

	@Scheduled(fixedDelayString = "${settingControl.refresh.fixedDelay:5}", initialDelayString = "${settingControl.refresh.initialDelay:5}", timeUnit = TimeUnit.MINUTES)
	public void refresh() {
		if (settings != null)
			settings = reload();
	}

	@PostConstruct
	public void afterPropertiesSet() {
		if (propertySourceMode != null) {
			MutablePropertySources mps = environment.getPropertySources();
			PropertySource<?> ps = new SettingsPropertySource("settings", this::getRequiredSettings);
			propertySourceMode.add(mps, ps);
			log.info("Add settings PropertySource as {}", propertySourceMode);
		}
	}

	public void setValue(String key, String value) {
		entityManager.setEntityClass(Setting.class);
		Setting s = entityManager.findByNaturalId(key);
		if (s != null) {
			if (value == null) {
				entityManager.delete(s);
				return;
			}
			s.setValue(value);
		} else {
			s = new Setting(key, value);
		}
		entityManager.save(s);
	}

	public void setValue(String key, String value, boolean readonly) {
		entityManager.setEntityClass(Setting.class);
		Setting s = entityManager.findByNaturalId(key);
		if (s != null) {
			if (value == null) {
				entityManager.delete(s);
				return;
			}
			s.setValue(value);
		} else {
			s = new Setting(key, value);
		}
		s.setReadonly(readonly);
		entityManager.save(s);
	}

	public void setValue(String key, String value, boolean readonly, boolean hidden) {
		entityManager.setEntityClass(Setting.class);
		Setting s = entityManager.findByNaturalId(key);
		if (s != null) {
			if (value == null) {
				entityManager.delete(s);
				return;
			}
			s.setValue(value);
		} else {
			s = new Setting(key, value);
		}
		s.setReadonly(readonly);
		s.setHidden(hidden);
		entityManager.save(s);
	}

	public int getIntValue(String key) {
		return getIntValue(key, 0);
	}

	public int getIntValue(String key, int defaultValue) {
		Setting s = getRequiredSettings().get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue()))
			return Integer.parseInt(s.getValue().trim());
		return defaultValue;
	}

	public boolean getBooleanValue(String key) {
		return getBooleanValue(key, false);
	}

	public boolean getBooleanValue(String key, boolean defaultValue) {
		Setting s = getRequiredSettings().get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue())) {
			String value = s.getValue().trim();
			return value.equals("true");
		}
		return defaultValue;
	}

	public List<Setting> getAllBooleanSettings() {
		return Collections
				.unmodifiableList(getRequiredSettings().values().stream().filter(s -> !(s.isReadonly() || s.isHidden()))
						.filter(s -> "true".equals(s.getValue()) || "false".equals(s.getValue()))
						.sorted(Comparator.comparing(Setting::getKey)).collect(Collectors.toList()));
	}

	public String getStringValue(String key) {
		return getStringValue(key, null);
	}

	public String getStringValue(String key, String defaultValue) {
		Setting s = getRequiredSettings().get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue()))
			return s.getValue().trim();
		return defaultValue;
	}

	public String[] getStringArray(String key) {
		Setting s = getRequiredSettings().get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue()))
			return s.getValue().trim().split("\\s*,\\s*");
		return new String[0];
	}

	@EventListener
	public void onApplicationEvent(EntityOperationEvent<Setting> event) {
		settings = null;
	}

	@Setup
	@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
	@Transactional
	public void setup() {
		entityManager.setEntityClass(Setting.class);
		Date now = new Date();
		Setting sd = entityManager.findOne(SETTING_KEY_SETUP_DATETIME);
		if (sd == null) {
			sd = new Setting(SETTING_KEY_SETUP_DATETIME, DateUtils.formatDatetime(now));
			sd.setReadonly(true);
			entityManager.save(sd);
		}
		sd = entityManager.findOne(SETTING_KEY_SETUP_TIMESTAMP);
		if (sd == null) {
			sd = new Setting(SETTING_KEY_SETUP_TIMESTAMP, String.valueOf(now.getTime()));
			sd.setReadonly(true);
			sd.setHidden(true);
			entityManager.save(sd);
		}
		try (InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("resources/data/setting.txt")) {
			if (is == null)
				return;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				br.lines().filter(s -> StringUtils.isNotBlank(s) && !s.trim().startsWith("#")).forEach(s -> {
					String arr[] = s.trim().split("#", 2);
					String description = null;
					if (arr.length == 2)
						description = arr[1].trim();
					arr = arr[0].trim().split("\\s*=\\s*", 2);
					if (arr.length > 1) {
						Setting temp;
						if ((temp = entityManager.findOne(arr[0])) != null) {
							if (AppInfo.getStage() == Stage.DEVELOPMENT) {
								entityManager.delete(temp);
								temp = null;
							}
						}
						if (temp == null) {
							Setting setting = new Setting(arr[0], arr[1]);
							setting.setDescription(description);
							entityManager.save(setting);
						}
					}
				});
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	static class SettingsPropertySource extends EnumerablePropertySource<Supplier<Map<String, Setting>>> {

		public SettingsPropertySource(String name, Supplier<Map<String, Setting>> source) {
			super(name, source);
		}

		@Override
		@Nullable
		public Object getProperty(String name) {
			Setting setting = this.source.get().get(name);
			return setting != null ? setting.getValue() : null;
		}

		@Override
		public boolean containsProperty(String name) {
			return this.source.get().containsKey(name);
		}

		@Override
		public String[] getPropertyNames() {
			return org.springframework.util.StringUtils.toStringArray(this.source.get().keySet());
		}

	}
}
