package org.ironrhino.sample.crud;

import java.time.MonthDay;
import java.time.YearMonth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.PastOrPresent;

import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Entity
@Table(name = "sample_employee")
@Richtable(showQueryForm = true)
@Getter
@Setter
public class Employee implements Persistable<String> {

	private static final long serialVersionUID = 2110061290463634971L;

	@UiConfig(width = "100px", alias = "employeeNo", regex = "\\d{5}", description = "employeeNo.description")
	@Id
	@Column(name = "employeeNo", length = 5)
	private String id;

	@UiConfig(width = "200px", template = "${value[0..<1]}**", readonly = @Readonly(expression = "!entity.new"))
	@SearchableComponent
	private String name;

	@SearchableComponent(nestSearchableProperties = "name")
	@UiConfig(width = "200px")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company")
	private Company company;

	@UiConfig(width = "80px", alias = "入职年月")
	@Column(length = 7)
	@PastOrPresent
	private YearMonth since;

	@UiConfig(width = "120px", alias = "生日")
	@Column(length = 7)
	private MonthDay birthday;

	@Lob
	@UiConfig(type = "textarea")
	private String description;

	@Override
	public String toString() {
		return this.name;
	}

}