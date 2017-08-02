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

import org.hibernate.annotations.GenericGenerator;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Entity
@Table(name = "sample_task")
@Richtable(order = "id desc")
@Getter
@Setter
public class Task implements Persistable<String> {

	private static final long serialVersionUID = 2110061290463634971L;

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

	@UiConfig(width = "100px", displayOrder = -1, description = "taskNo.description")
	public String getTaskNo() {
		return getId();
	}

	@Override
	public String toString() {
		return this.title;
	}

}