package org.ironrhino.core.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.ironrhino.core.metadata.NotInJson;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableId;

@MappedSuperclass
public class BaseEntity extends Entity<String> {

	private static final long serialVersionUID = 5290168777920037800L;

	@SearchableId(type = "string", index = Index.NOT_ANALYZED)
	@Id
	@GeneratedValue(generator = "stringId")
	@GenericGenerator(name = "stringId", strategy = "org.ironrhino.core.hibernate.StringIdGenerator")
	@Column(length = 32)
	protected String id;

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		if (StringUtils.isNotBlank(id))
			this.id = id;
	}

	@Override
	@NotInJson
	public boolean isNew() {
		return id == null || StringUtils.isBlank(id);
	}

}