package org.ironrhino.common.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Order;
import org.ironrhino.common.model.Dictionary;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DictionaryControl implements
		ApplicationListener<EntityOperationEvent> {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private Map<String, Dictionary> map;

	@Resource
	private EntityManager<Dictionary> entityManager;

	@PostConstruct
	public void afterPropertiesSet() {
		refresh();
	}

	public void refresh() {
		entityManager.setEntityClass(Dictionary.class);
		List<Dictionary> list = entityManager.findAll(Order.asc("name"));
		Map<String, Dictionary> temp = new ConcurrentHashMap<String, Dictionary>();
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
				return StringUtils.isNotBlank(entry.getValue()) ? entry
						.getValue() : entry.getKey();
		}
		return value;
	}

	@Override
	public void onApplicationEvent(EntityOperationEvent event) {
		if (event.getEntity() instanceof Dictionary) {
			Dictionary dictInEvent = (Dictionary) event.getEntity();
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
	}
}
