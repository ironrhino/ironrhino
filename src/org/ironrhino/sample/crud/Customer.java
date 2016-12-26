package org.ironrhino.sample.crud;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.common.model.Gender;
import org.ironrhino.core.hibernate.convert.EnumSetConverter;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseRecordableEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.struts.ValidationException;

@Searchable
@AutoConfig
@Table(name = "sample_customer")
@Entity
@Richtable(gridColumns = 3, celleditable = false, readonly = @Readonly(expression = "entity.balance<10", deletable = true), showQueryForm = true)
public class Customer extends BaseRecordableEntity {

	private static final long serialVersionUID = -2413944328894923968L;

	@SearchableProperty
	@UiConfig(width = "100px")
	@NaturalId(mutable = true)
	private String name;

	@UiConfig(width = "100px")
	@Enumerated
	@Column(nullable = false)
	private Gender gender;

	@UiConfig(width = "100px")
	private Integer age;

	@UiConfig(width = "100px", type = "dictionary", templateName = "customer_category")
	private String category;

	@UiConfig(type = "dictionary", templateName = "customer_category", hiddenInList = @Hidden(true), description = "potentialCategories.description")
	private Set<String> potentialCategories;

	@UiConfig(width = "100px")
	@Enumerated
	@Column(nullable = false)
	private CustomerRank rank = CustomerRank.BRONZE;

	@UiConfig(hiddenInList = @Hidden(true), description = "potentialRanks.description")
	private Set<CustomerRank> potentialRanks;

	@UiConfig(width = "100px", template = "${value?string('#,###.00')}", showSum = true, description = "balance.description")
	@Column(nullable = false)
	private BigDecimal balance;

	@SearchableComponent
	private Set<String> tags;

	@UiConfig(type = "treeselect", width = "200px", description = "activeRegions.description", pickUrl = "/common/region/children", template = "<#if value?has_content><#list value as id>${beans['regionTreeControl'].tree.getDescendantOrSelfById(id).name}<#sep> </#list></#if>")
	private Set<Long> activeRegions;

	@SearchableComponent(nestSearchableProperties = "name")
	@UiConfig(width = "200px", pickUrl = "/sample/company/pick?columns=name,type&creatable=true&editable=true", description = "company.description")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company")
	private Company company;

	@UiConfig(cssClass = "nullable", description = "addresses.description")
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "sample_customer_address", joinColumns = @JoinColumn(name = "customer"))
	@OrderColumn(name = "lineNumber", nullable = false)
	@Fetch(FetchMode.SUBSELECT)
	private List<CustomerAddress> addresses;

	@Version
	private int version = -1;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Set<String> getPotentialCategories() {
		return potentialCategories;
	}

	public void setPotentialCategories(Set<String> potentialCategories) {
		this.potentialCategories = potentialCategories;
	}

	public CustomerRank getRank() {
		return rank;
	}

	public void setRank(CustomerRank rank) {
		this.rank = rank;
	}

	public Set<CustomerRank> getPotentialRanks() {
		return potentialRanks;
	}

	public void setPotentialRanks(Set<CustomerRank> potentialRanks) {
		this.potentialRanks = potentialRanks;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public Set<Long> getActiveRegions() {
		return activeRegions;
	}

	public void setActiveRegions(Set<Long> activeRegions) {
		this.activeRegions = activeRegions;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public List<CustomerAddress> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<CustomerAddress> addresses) {
		this.addresses = addresses;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 97)
	public Date getCreateDate() {
		return super.getCreateDate();
	}

	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 98)
	public Date getModifyDate() {
		return super.getModifyDate();
	}

	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 99)
	public String getCreateUser() {
		return super.getCreateUser();
	}

	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 100)
	public String getModifyUser() {
		return super.getModifyUser();
	}

	@PrePersist
	@PreUpdate
	private void validate() {
		ValidationException ve = new ValidationException();
		if (potentialCategories != null && potentialCategories.contains(this.category)) {
			ve.addFieldError("customer.potentialCategories", "不能包含当前分类");
		}
		if (potentialRanks != null && potentialRanks.contains(this.rank)) {
			ve.addFieldError("customer.potentialRanks", "不能包含当前等级");
		}
		if (balance == null || balance.doubleValue() > 100000) {
			ve.addFieldError("customer.balance", "余额不能大于100,000.00");
		}
		if (ve.hasError())
			throw ve;
	}

	@Converter(autoApply = true)
	public static class CustomerRankSetConverter extends EnumSetConverter<CustomerRank> {

	}
}
