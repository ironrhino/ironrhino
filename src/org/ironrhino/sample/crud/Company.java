package org.ironrhino.sample.crud;

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.MatchMode;
import org.ironrhino.common.model.Region;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

@Searchable
@AutoConfig
@Table(name = "sample_company")
@Entity
@Richtable(showQueryForm = true)
public class Company extends BaseEntity {

	private static final long serialVersionUID = -2413944328894923968L;

	@SearchableProperty
	@UiConfig(group = "baseInfo", queryMatchMode = MatchMode.EXACT)
	@NaturalId(mutable = true)
	private String name;

	@UiConfig(width = "100px", group = "baseInfo", shownInPick = true)
	@Enumerated
	@Column(nullable = false)
	private CompanyType type;

	@UiConfig(width = "100px", regex = "\\w{9}", group = "baseInfo", shownInPick = true)
	@Column(nullable = false, unique = true, length = 9)
	private String organizationCode;

	@UiConfig(width = "150px", template = "<#if value?has_content>${value.fullname}</#if>", group = "contactInfo")
	@ManyToOne(fetch = FetchType.LAZY)
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
	private Collection<Customer> customers;

	@UiConfig(width = "150px", description = "relatedCustomers.description", group = "customer")
	@ManyToMany
	@JoinTable(name = "sample_company_related_customer", joinColumns = @JoinColumn(name = "company"), inverseJoinColumns = @JoinColumn(name = "customer"))
	private Collection<Customer> relatedCustomers;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CompanyType getType() {
		return type;
	}

	public void setType(CompanyType type) {
		this.type = type;
	}

	public String getOrganizationCode() {
		return organizationCode;
	}

	public void setOrganizationCode(String organizationCode) {
		this.organizationCode = organizationCode;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getIntro() {
		return intro;
	}

	public void setIntro(String intro) {
		this.intro = intro;
	}

	public Collection<Customer> getCustomers() {
		return customers;
	}

	public void setCustomers(Collection<Customer> customers) {
		this.customers = customers;
	}

	public Collection<Customer> getRelatedCustomers() {
		return relatedCustomers;
	}

	public void setRelatedCustomers(Collection<Customer> relatedCustomers) {
		this.relatedCustomers = relatedCustomers;
	}

}
