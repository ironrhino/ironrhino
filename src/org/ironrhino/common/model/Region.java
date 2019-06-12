package org.ironrhino.common.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.ironrhino.common.util.LocationUtils;
import org.ironrhino.core.aop.PublishAware;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.FullnameSeperator;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.util.PinyinUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PublishAware
@AutoConfig
@Searchable
@Entity
@Table(name = "common_region")
@FullnameSeperator(independent = false, seperator = "")
@Getter
@Setter
@NoArgsConstructor
@Richtable(actionColumnButtons = "<@btn view='input' label='edit'/><#if 'treeview'!=Parameters.view!><a class=\"btn ajax view\" href=\"${actionBaseUrl+\"?parent=\"+entity.id}<#if tree??>&tree=${tree}</#if>\">${getText(\"enter\")}</a></#if>", bottomButtons = "<#if !(tree?? && tree gt 0 && (!parent??||parent lt 1))><@btn view='input' label='create'/> </#if><@btn action='save' confirm=true/> <@btn action='delete' confirm=true/> <#if 'treeview'!=Parameters.view! && region?? && parent??><#if region.parent?? && (!tree??||parent!=tree)><a class=\"btn ajax view\" href=\"${actionBaseUrl+\"?parent=\"+region.parent.id}<#if tree??>&tree=${tree}</#if>\" rel=\"up\">${getText(\"upward\")}</a><#else><a class=\"btn ajax view\" href=\"${actionBaseUrl}<#if tree??>?tree=${tree}</#if>\" rel=\"up\">${getText(\"upward\")}</a></#if></#if>")
public class Region extends BaseTreeableEntity<Region> {

	private static final long serialVersionUID = 8878381261391688086L;

	@Embedded
	@UiConfig(hiddenInList = @Hidden(true), cssClass = "latlng", embeddedAsSingle = true, dynamicAttributes = "{\"data-address\":\"${region.fullname!}\"}")
	private @Valid Coordinate coordinate;

	@UiConfig(hidden = true)
	private String fullname;

	@Column(length = 6)
	@UiConfig(width = "100px")
	private String areacode;

	@Column(length = 6)
	@UiConfig(width = "100px")
	private String postcode;

	@Min(1)
	@UiConfig(width = "100px")
	private Integer rank;

	public Region(String name) {
		this.name = name;
	}

	public Region(String name, int displayOrder) {
		this.name = name;
		this.displayOrder = displayOrder;
	}

	@Override
	@NotInCopy
	@SearchableProperty(boost = 2)
	@Access(AccessType.PROPERTY)
	public String getFullname() {
		if (fullname == null)
			fullname = super.getFullname();
		return fullname;
	}

	@Override
	public void setParent(Region parent) {
		super.setParent(parent);
		if (this.fullname != null)
			this.fullname = super.getFullname();
	}

	@JsonIgnore
	@SearchableProperty(boost = 3, index = Index.NOT_ANALYZED)
	public String getNameAsPinyin() {
		return PinyinUtils.pinyin(name);
	}

	@JsonIgnore
	@SearchableProperty(boost = 3, index = Index.NOT_ANALYZED)
	public String getNameAsPinyinAbbr() {
		return PinyinUtils.pinyinAbbr(name);
	}

	@JsonIgnore
	@SearchableProperty(boost = 3, index = Index.NOT_ANALYZED)
	public String getShortFullname() {
		return LocationUtils.shortenAddress(getFullname());
	}

	public Region getDescendantOrSelfByAreacode(String areacode) {
		if (areacode == null)
			throw new IllegalArgumentException("areacode must not be null");
		if (areacode.equals(this.getAreacode()))
			return this;
		for (Region t : getChildren()) {
			Region tt = t.getDescendantOrSelfByAreacode(areacode);
			if (tt != null)
				return tt;
		}
		return null;
	}

}
