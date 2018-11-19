package org.ironrhino.sample.crud;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.common.model.Gender;
import org.ironrhino.core.hibernate.convert.EnumArrayConverter;
import org.ironrhino.core.hibernate.convert.EnumSetConverter;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseRecordableEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.struts.ValidationException;

import lombok.Getter;
import lombok.Setter;

@Searchable
@AutoConfig
@Table(name = "sample_customer")
@Entity
@Richtable(gridColumns = 3, celleditable = false, readonly = @Readonly(expression = "entity.balance<10", deletable = true), showQueryForm = true)
@Getter
@Setter
public class Customer extends BaseRecordableEntity {

	private static final long serialVersionUID = -2413944328894923968L;

	@SearchableProperty
	@UiConfig(width = "100px")
	@CaseInsensitive
	@NaturalId(mutable = true)
	private String name;

	@UiConfig(width = "60px", cssClass = "conjunct", dynamicAttributes = "{\"data-replacement\":\"control-group-customer-age\"}")
	@Column(nullable = false)
	private Gender gender;

	@UiConfig(width = "60px", hiddenInInput = @Hidden(expression = "entity.gender??&&entity.gender.name()=='FEMALE'"), description = "age.description", queryWithRange = true)
	@Min(1)
	@Max(100)
	private Integer age;

	@UiConfig(width = "60px")
	private boolean enabled;

	@UiConfig(width = "80px", type = "dictionary", cssClass = "chosen", templateName = "customer_category")
	private String category;

	@UiConfig(type = "dictionary", cssClass = "chosen", templateName = "customer_category", hiddenInList = @Hidden(true), description = "potentialCategories.description")
	private Set<String> potentialCategories;

	@UiConfig(width = "80px")
	@Column(nullable = false)
	private CustomerRank rank = CustomerRank.BRONZE;

	@UiConfig(hiddenInList = @Hidden(true), description = "potentialRanks.description")
	private CustomerRank[] potentialRanks;

	@UiConfig(width = "80px", template = "${value?string('#,###.00')}", showSum = true, description = "balance.description")
	@Column(nullable = false)
	@DecimalMax("100000.00")
	@DecimalMin("1.00")
	private BigDecimal balance;

	@UiConfig(width = "80px", template = "${(value*100)?string('0.0000')}%", description = "percentage.description")
	@Formula("balance/(select sum(a.balance) from sample_customer a where a.gender={alias}.gender)")
	private BigDecimal percentage;

	@SearchableComponent
	private Set<String> tags;

	@UiConfig(type = "treeselect", width = "100px", description = "activeRegions.description", pickUrl = "/common/region/children", template = "<#if value?has_content><#list value as id><span class=\"label\">${beans['regionTreeControl'].tree.getDescendantOrSelfById(id).name}</span><#sep> </#list></#if>")
	private Long[] activeRegions;

	@SearchableComponent(nestSearchableProperties = "name")
	@UiConfig(width = "120px", pickUrl = "/sample/company/pick?columns=name,type&creatable=true&editable=true", description = "company.description")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company")
	private Company company;

	@UiConfig(width = "120px", description = "relatedCompanies.description")
	@ManyToMany
	@JoinTable(name = "sample_company_customer", joinColumns = @JoinColumn(name = "customer"), inverseJoinColumns = @JoinColumn(name = "company"))
	private Set<Company> relatedCompanies;

	@Valid
	@UiConfig(cssClass = "nullable", description = "addresses.description")
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "sample_customer_address", joinColumns = @JoinColumn(name = "customer"))
	@OrderColumn(name = "lineNumber", nullable = false)
	@Fetch(FetchMode.SUBSELECT)
	private List<CustomerAddress> addresses;

	@Override
	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 97)
	public Date getCreateDate() {
		return super.getCreateDate();
	}

	@Override
	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 98)
	public Date getModifyDate() {
		return super.getModifyDate();
	}

	@Override
	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 99)
	public String getCreateUser() {
		return super.getCreateUser();
	}

	@Override
	@UiConfig(hiddenInList = @Hidden(true), displayOrder = 100)
	public String getModifyUser() {
		return super.getModifyUser();
	}

	@PrePersist
	@PreUpdate
	private void validate() {
		if (gender == Gender.FEMALE)
			age = null;
		ValidationException ve = new ValidationException();
		if (potentialCategories != null && potentialCategories.contains(this.category)) {
			ve.addFieldError("customer.potentialCategories", "不能包含当前分类");
		}
		if (potentialRanks != null && Arrays.asList(potentialRanks).contains(this.rank)) {
			ve.addFieldError("customer.potentialRanks", "不能包含当前等级");
		}
		if (ve.hasError())
			throw ve;
	}

	@Converter(autoApply = true)
	public static class CustomerRankSetConverter extends EnumSetConverter<CustomerRank> {

	}

	@Converter(autoApply = true)
	public static class CustomerRankArrayConverter extends EnumArrayConverter<CustomerRank>
			implements AttributeConverter<CustomerRank[], String> {

	}
}
