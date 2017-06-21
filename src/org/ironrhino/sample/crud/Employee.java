package org.ironrhino.sample.crud;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;

import com.fasterxml.jackson.annotation.JsonIgnore;

@AutoConfig
@Entity
@Table(name = "sample_employee")
@Richtable(showQueryForm = true)
public class Employee implements Persistable<String> {

	private static final long serialVersionUID = 2110061290463634971L;

	@UiConfig(width = "100px", alias = "employeeNo", regex = "\\d{5}", description = "employeeNo.description")
	@Id
	@Column(name = "employeeNo", length = 5)
	private String id;

	@UiConfig(width = "200px")
	@SearchableComponent
	private String name;

	@SearchableComponent(nestSearchableProperties = "name")
	@UiConfig(width = "200px")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company")
	private Company company;

	@Lob
	@UiConfig(type = "textarea")
	private String description;

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		if (StringUtils.isNotBlank(id))
			this.id = id;
	}

	@Override
	@JsonIgnore
	public boolean isNew() {
		return id == null || StringUtils.isBlank(id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	@Override
	public String toString() {
		return this.name;
	}

}