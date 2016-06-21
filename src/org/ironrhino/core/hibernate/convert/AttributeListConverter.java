package org.ironrhino.core.hibernate.convert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.model.Attribute;
import org.ironrhino.core.util.JsonUtils;

@Converter
public class AttributeListConverter implements AttributeConverter<List<Attribute>, String> {

	@Override
	public String convertToDatabaseColumn(List<Attribute> list) {
		if (list == null)
			return null;
		if (list.isEmpty())
			return "";
		Map<String, String> map = new LinkedHashMap<>();
		for (Attribute attr : list)
			if (StringUtils.isNotBlank(attr.getName()))
				map.put(attr.getName(), attr.getValue());
		return JsonUtils.toJson(map);
	}

	@Override
	public List<Attribute> convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		if (StringUtils.isEmpty(string))
			return new ArrayList<>();
		try {
			Map<String, String> map = JsonUtils.fromJson(string, JsonUtils.STRING_MAP_TYPE);
			List<Attribute> attributes = new ArrayList<>(map.size());
			for (Map.Entry<String, String> entry : map.entrySet())
				attributes.add(new Attribute(entry.getKey(), entry.getValue()));
			return attributes;
		} catch (Exception e) {
			throw new IllegalArgumentException(string + " is not valid json ", e);
		}
	}

}