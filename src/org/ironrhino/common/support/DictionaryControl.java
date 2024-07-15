package org.ironrhino.common.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Order;
import org.ironrhino.common.model.Dictionary;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DictionaryControl {

	@Autowired
	private EntityManager<Dictionary> entityManager;

	private volatile Map<String, Dictionary> map;

	private Map<String, Dictionary> getRequiredMap() {
		Map<String, Dictionary> temp = map;
		if (temp == null) {
			map = temp = reload();
		}
		return temp;
	}

	private Map<String, Dictionary> reload() {
		entityManager.setEntityClass(Dictionary.class);
		List<Dictionary> list = entityManager.findAll(Order.asc("name"));
		Map<String, Dictionary> temp = new HashMap<>();
		for (Dictionary d : list)
			temp.put(d.getName(), d);
		return temp;
	}

	@Scheduled(fixedDelayString = "${dictionaryControl.refresh.fixedDelay:5}", initialDelayString = "${dictionaryControl.refresh.initialDelay:5}", timeUnit = TimeUnit.MINUTES)
	public void refresh() {
		if (map != null)
			map = reload();
	}

	public Dictionary getDictionary(String name) {
		return getRequiredMap().get(name);
	}

	public Map<String, String> getItemsAsMap(String name) {
		Dictionary dict = getRequiredMap().get(name);
		if (dict == null)
			return Collections.emptyMap();
		return dict.getItemsAsMap();
	}

	public Map<String, Map<String, String>> getItemsAsGroup(String name) {
		Dictionary dict = getRequiredMap().get(name);
		if (dict == null)
			return Collections.emptyMap();
		return dict.getItemsAsGroup();
	}

	public String getDictionaryLabel(String name, String value) {
		if (value == null)
			return null;
		for (Map.Entry<String, String> entry : getItemsAsMap(name).entrySet()) {
			if (value.equals(entry.getKey()))
				return StringUtils.isNotBlank(entry.getValue()) ? entry.getValue() : entry.getKey();
		}
		return value;
	}

	public String getDictionaryValue(String name, String label) {
		if (label == null)
			return null;
		for (Map.Entry<String, String> entry : getItemsAsMap(name).entrySet()) {
			if (label.equals(StringUtils.isNotBlank(entry.getValue()) ? entry.getValue() : entry.getKey()))
				return entry.getKey();
		}
		return label;
	}

	@EventListener
	public void onApplicationEvent(EntityOperationEvent<Dictionary> event) {
		map = null;
	}

	@Setup
	@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
	@Transactional
	public void setup() {
		entityManager.setEntityClass(Dictionary.class);
		try (InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("resources/data/dictionary.txt")) {
			if (is == null)
				return;
			Dictionary temp = null;
			String name = null;
			String description = null;
			List<LabelValue> items = null;
			List<String> lines;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				lines = br.lines().collect(Collectors.toList());
			}
			for (String s : lines) {
				if (StringUtils.isBlank(s) || s.trim().startsWith("#")) {
					if (name != null && items != null) {
						temp = entityManager.findOne(name);
						if (AppInfo.getStage() == Stage.DEVELOPMENT && temp != null) {
							entityManager.delete(temp);
							temp = null;
						}
						if (temp == null) {
							Dictionary dictionary = new Dictionary();
							dictionary.setName(name);
							dictionary.setItems(items);
							dictionary.setDescription(description);
							entityManager.save(dictionary);
						}
						name = null;
						description = null;
						items = null;
					}
					continue;
				}
				if (name == null) {
					String[] arr = s.split("#", 2);
					name = arr[0].trim();
					if (arr.length > 1)
						description = arr[1].trim();
				} else {
					String[] arr = s.split("\\s*=\\s*", 2);
					if (items == null)
						items = new ArrayList<>();
					items.add(new LabelValue(arr.length > 1 ? arr[1] : null, arr[0]));
				}
			}
			if (name != null && items != null) {
				temp = entityManager.findOne(name);
				if (AppInfo.getStage() == Stage.DEVELOPMENT && temp != null) {
					entityManager.delete(temp);
					temp = null;
				}
				if (temp == null) {
					Dictionary dictionary = new Dictionary();
					dictionary.setName(name);
					dictionary.setItems(items);
					dictionary.setDescription(description);
					entityManager.save(dictionary);
				}
				name = null;
				description = null;
				items = null;
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
}
