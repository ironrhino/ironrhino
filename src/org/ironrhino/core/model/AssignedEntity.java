package org.ironrhino.core.model;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass
public abstract class AssignedEntity extends Entity<String> {

	private static final long serialVersionUID = -6924908012178703499L;

	@Id
	@GeneratedValue(generator = "assigned")
	@GenericGenerator(name = "assigned", strategy = "assigned")
	private String id;

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		if (StringUtils.isNotBlank(id))
			this.id = id;
	}

	public void clearId() {
		this.id = null;
	}

	@Override
	@JsonIgnore
	public boolean isNew() {
		return id == null || StringUtils.isBlank(id);
	}

}