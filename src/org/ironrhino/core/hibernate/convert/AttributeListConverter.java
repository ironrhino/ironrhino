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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

@Converter(autoApply = true)
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
			map.forEach((k, v) -> {
				attributes.add(new Attribute(k, v));
			});
			return attributes;
		} catch (Exception e) {
			if (e instanceof MismatchedInputException) {
				// keep compatibility with legacy List<Attribute>
				try {
					return JsonUtils.fromJson(string, ATTRIBUTE_LIST);
				} catch (Exception e2) {
					throw new IllegalArgumentException(string + " is not valid json", e2);
				}
			}
			throw new IllegalArgumentException(string + " is not valid json", e);
		}
	}

	private static final TypeReference<List<Attribute>> ATTRIBUTE_LIST = new TypeReference<List<Attribute>>() {
	};

}