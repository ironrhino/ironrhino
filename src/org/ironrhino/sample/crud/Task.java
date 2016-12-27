package org.ironrhino.sample.crud;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;

import com.fasterxml.jackson.annotation.JsonIgnore;

@AutoConfig
@Entity
@Table(name = "sample_task")
public class Task implements Persistable<String> {

	private static final long serialVersionUID = 2110061290463634971L;

	@UiConfig(width = "100px", alias = "taskNo", readonly = @Readonly(true), description = "taskNo.description")
	@Id
	@GeneratedValue(generator = "taskNo")
	@GenericGenerator(name = "taskNo", strategy = "taskNo")
	@Column(name = "taskNo", length = 15)
	private String id;

	@UiConfig(width = "200px")
	@SearchableComponent
	@Column(nullable = false)
	private String title;

	@SearchableComponent(nestSearchableProperties = "name")
	@UiConfig(width = "200px")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee")
	private Employee employee;

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return this.title;
	}

}