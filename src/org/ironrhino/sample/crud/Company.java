package org.ironrhino.sample.crud;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.common.model.Region;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;

@AutoConfig
@Table(name = "sample_company")
@Entity
public class Company extends BaseEntity {

	private static final long serialVersionUID = -2413944328894923968L;

	@UiConfig(group = "baseInfo")
	@NaturalId(mutable = true)
	private String name;

	@UiConfig(width = "100px", group = "baseInfo", shownInPick = true)
	@Enumerated
	@Column(nullable = false)
	private CompanyType type;

	@UiConfig(width = "100px", regex = "\\w{9}", group = "baseInfo", shownInPick = true)
	@Column(nullable = false, unique = true, length = 9)
	private String organizationCode;

	@UiConfig(width = "200px", template = "<#if value??>${value.fullname}</#if>", group = "contactInfo")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region")
	private Region region;

	@UiConfig(width = "200px", group = "contactInfo")
	private String address;

	@UiConfig(width = "100px", regex = "\\d+", group = "contactInfo")
	private String phone;

	@Lob
	@UiConfig(type = "textarea", cssClass = "htmlarea", hiddenInList = @Hidden(true), group = "intro")
	private String intro;

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

}
