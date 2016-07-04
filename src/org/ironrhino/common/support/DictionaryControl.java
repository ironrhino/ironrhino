package org.ironrhino.common.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Order;
import org.ironrhino.common.model.Dictionary;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.BeanUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class DictionaryControl {

	@Autowired
	private Logger logger;

	@Autowired
	private EntityManager<Dictionary> entityManager;

	private Map<String, Dictionary> map;

	@PostConstruct
	public void afterPropertiesSet() {
		refresh();
	}

	public void refresh() {
		entityManager.setEntityClass(Dictionary.class);
		List<Dictionary> list = entityManager.findAll(Order.asc("name"));
		Map<String, Dictionary> temp = new ConcurrentHashMap<>();
		for (Dictionary d : list)
			temp.put(d.getName(), d);
		map = temp;
	}

	public Dictionary getDictionary(String name) {
		return map.get(name);
	}

	public Map<String, String> getItemsAsMap(String name) {
		Dictionary dict = map.get(name);
		if (dict == null)
			return Collections.emptyMap();
		return dict.getItemsAsMap();
	}

	public Map<String, Map<String, String>> getItemsAsGroup(String name) {
		Dictionary dict = map.get(name);
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

	@EventListener
	public void onApplicationEvent(EntityOperationEvent<Dictionary> event) {
		Dictionary dictInEvent = event.getEntity();
		if (event.getType() == EntityOperationType.CREATE) {
			map.put(dictInEvent.getName(), dictInEvent);
		} else {
			Dictionary dictInMemory = null;
			for (Dictionary dictionary : map.values()) {
				if (dictionary.getId().equals(dictInEvent.getId())) {
					dictInMemory = dictionary;
					break;
				}
			}
			if (dictInMemory != null)
				if (event.getType() == EntityOperationType.UPDATE) {
					map.remove(dictInMemory.getName());
					BeanUtils.copyProperties(dictInEvent, dictInMemory);
					map.put(dictInMemory.getName(), dictInMemory);
				} else if (event.getType() == EntityOperationType.DELETE) {
					map.remove(dictInMemory.getName());
				}
		}
	}

	@Setup
	@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
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
			for (String s : IOUtils.readLines(is, "UTF-8")) {
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
			logger.error(e.getMessage(), e);
		}
	}
}
