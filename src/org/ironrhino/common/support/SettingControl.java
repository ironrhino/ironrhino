package org.ironrhino.common.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Order;
import org.ironrhino.common.Constants;
import org.ironrhino.common.model.Setting;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class SettingControl {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private Map<String, Setting> settings;

	@Resource
	private EntityManager<Setting> entityManager;

	@PostConstruct
	public void afterPropertiesSet() {
		refresh();
	}

	public void refresh() {
		entityManager.setEntityClass(Setting.class);
		List<Setting> list = entityManager.findAll(Order.asc("key"));
		Map<String, Setting> temp = new ConcurrentHashMap<>();
		for (Setting s : list)
			temp.put(s.getKey(), s);
		settings = temp;
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
		Setting s = settings.get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue()))
			return Integer.parseInt(s.getValue().trim());
		return defaultValue;
	}

	public boolean getBooleanValue(String key) {
		return getBooleanValue(key, false);
	}

	public boolean getBooleanValue(String key, boolean defaultValue) {
		Setting s = settings.get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue())) {
			String value = s.getValue().trim();
			return value.equals("true");
		}
		return defaultValue;
	}

	public List<Setting> getAllBooleanSettings() {
		List<Setting> list = new ArrayList<>();
		for (Setting s : settings.values()) {
			String value = s.getValue();
			if ("true".equals(value) || "false".equals(value))
				list.add(s);
		}
		Collections.sort(list, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		return list;
	}

	public String getStringValue(String key) {
		return getStringValue(key, null);
	}

	public String getStringValue(String key, String defaultValue) {
		Setting s = settings.get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue()))
			return s.getValue().trim();
		return defaultValue;
	}

	public String[] getStringArray(String key) {
		Setting s = settings.get(key);
		if (s != null && StringUtils.isNotBlank(s.getValue()))
			return s.getValue().trim().split("\\s*,\\s*");
		return new String[0];
	}

	@EventListener
	public void onApplicationEvent(EntityOperationEvent<Setting> event) {
		Setting settingInEvent = event.getEntity();
		if (event.getType() == EntityOperationType.CREATE) {
			settings.put(settingInEvent.getKey(), settingInEvent);
		} else {
			Setting settingInMemory = null;
			for (Setting setting : settings.values()) {
				if (setting.getId().equals(settingInEvent.getId())) {
					settingInMemory = setting;
					break;
				}
			}
			if (settingInMemory != null)
				if (event.getType() == EntityOperationType.UPDATE) {
					settings.remove(settingInMemory.getKey());
					BeanUtils.copyProperties(settingInEvent, settingInMemory);
					settings.put(settingInMemory.getKey(), settingInMemory);
				} else if (event.getType() == EntityOperationType.DELETE) {
					settings.remove(settingInMemory.getKey());
				}
		}
	}

	@Setup
	@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
	public void setup() {
		entityManager.setEntityClass(Setting.class);
		Date now = new Date();
		Setting sd = entityManager.findOne(Constants.SETTING_KEY_SETUP_DATETIME);
		if (sd == null) {
			sd = new Setting(Constants.SETTING_KEY_SETUP_DATETIME, DateUtils.formatDatetime(now));
			sd.setReadonly(true);
			entityManager.save(sd);
		}
		sd = entityManager.findOne(Constants.SETTING_KEY_SETUP_TIMESTAMP);
		if (sd == null) {
			sd = new Setting(Constants.SETTING_KEY_SETUP_TIMESTAMP, String.valueOf(now.getTime()));
			sd.setReadonly(true);
			sd.setHidden(true);
			entityManager.save(sd);
		}
		try (InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("resources/data/setting.txt")) {
			if (is == null)
				return;
			Setting temp;
			for (String s : IOUtils.readLines(is, "UTF-8")) {
				if (StringUtils.isBlank(s) || s.trim().startsWith("#"))
					continue;
				String arr[] = s.trim().split("#", 2);
				String description = null;
				if (arr.length == 2)
					description = arr[1].trim();
				arr = arr[0].trim().split("\\s*=\\s*", 2);
				if (arr.length < 2)
					continue;
				if ((temp = entityManager.findOne(arr[0])) != null)
					if (AppInfo.getStage() == Stage.DEVELOPMENT)
						entityManager.delete(temp);
					else
						continue;
				Setting setting = new Setting(arr[0], arr[1]);
				setting.setDescription(description);
				entityManager.save(setting);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
