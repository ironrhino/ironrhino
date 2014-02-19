package org.ironrhino.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

@Searchable(root = false)
public class SchemaField implements Serializable {

	private static final long serialVersionUID = 9104177103768030668L;

	@SearchableProperty(boost = 2, index = Index.NOT_ANALYZED)
	private String name;

	@SearchableProperty(boost = 2, index = Index.NOT_ANALYZED)
	private List<String> values = new ArrayList<String>();

	@SearchableProperty(index = Index.NO)
	private SchemaFieldType type;

	@SearchableProperty(index = Index.NO)
	private boolean required;

	@SearchableProperty(index = Index.NO)
	private boolean strict;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public SchemaFieldType getType() {
		return type;
	}

	public void setType(SchemaFieldType type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SchemaField) {
			SchemaField that = (SchemaField) o;
			return that.getName() != null
					&& that.getName().equals(this.getName());
		}
		return false;
	}

}
