package org.ironrhino.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

import lombok.Data;

@Searchable(root = false)
@Data
public class SchemaField implements Serializable {

	private static final long serialVersionUID = 9104177103768030668L;

	@SearchableProperty(boost = 2)
	private String name;

	@SearchableProperty(boost = 2)
	private List<String> values = new ArrayList<>();

	@SearchableProperty(index = Index.NO)
	private SchemaFieldType type;

	@SearchableProperty(index = Index.NO)
	private boolean required;

	@SearchableProperty(index = Index.NO)
	private boolean strict;

}
