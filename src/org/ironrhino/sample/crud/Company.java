package org.ironrhino.sample.crud;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.GroupSequence;

import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.MatchMode;
import org.hibernate.validator.constraints.Length;
import org.ironrhino.common.model.Region;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.validation.constraints.OrganizationCode;
import org.ironrhino.core.validation.constraints.SocialCreditIdentifier;
import org.ironrhino.core.validation.groups.Steps.Step1;
import org.ironrhino.core.validation.groups.Steps.Step2;

import lombok.Getter;
import lombok.Setter;

@GroupSequence({ Step1.class, Step2.class, Company.class })
@Searchable
@AutoConfig
@Table(name = "sample_company")
@Entity
@Richtable(showQueryForm = true)
@Getter
@Setter
public class Company extends BaseEntity {

	private static final long serialVersionUID = -2413944328894923968L;

	@Length(min = 2, max = 50)
	@SearchableProperty
	@UiConfig(group = "baseInfo", queryMatchMode = MatchMode.EXACT)
	@NaturalId(mutable = true)
	private String name;

	@UiConfig(width = "100px", group = "baseInfo", shownInPick = true)
	@Column(nullable = false)
	private CompanyType type;

	@OrganizationCode(groups = Step1.class)
	@UiConfig(width = "100px", group = "baseInfo", shownInPick = true)
	@Column(nullable = false, unique = true, length = 9)
	private String organizationCode;

	@SocialCreditIdentifier(groups = Step2.class)
	@UiConfig(width = "100px", group = "baseInfo", hiddenInList = @Hidden(true), shownInPick = true)
	@Column(unique = true, length = 18)
	private String socialCreditIdentifier;

	@UiConfig(width = "150px", template = "<#if value?has_content>${value.fullname}</#if>", group = "contactInfo")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "region")
	private Region region;

	@UiConfig(width = "150px", group = "contactInfo")
	private String address;

	@UiConfig(width = "100px", regex = "\\d+", group = "contactInfo")
	private String phone;

	@Lob
	@UiConfig(type = "textarea", cssClass = "htmlarea", hiddenInList = @Hidden(true), group = "intro")
	private String intro;

	@UiConfig(width = "150px", description = "customers.description", group = "customer")
	@OneToMany(mappedBy = "company")
	@OrderBy("name asc")
	private Set<Customer> customers;

	@UiConfig(width = "150px", description = "relatedCustomers.description", group = "customer")
	@ManyToMany
	@JoinTable(name = "sample_company_customer", joinColumns = @JoinColumn(name = "company"), inverseJoinColumns = @JoinColumn(name = "customer"))
	private Set<Customer> relatedCustomers;

	@UiConfig(width = "80px", group = "contactInfo", description = "一对一关系")
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "company")
	private Boss boss;

}
