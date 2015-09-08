package org.ironrhino.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.ironrhino.core.aop.PublishAware;
import org.ironrhino.core.hibernate.convert.AttributeListConverter;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Attributable;
import org.ironrhino.core.model.Attribute;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;

@PublishAware
@AutoConfig
@Entity
@Table(name = "common_treenode")
@Searchable
public class TreeNode extends BaseTreeableEntity<TreeNode>implements Attributable {

	private static final long serialVersionUID = 8878337541387688086L;

	@UiConfig(type = "textarea", displayOrder = 100)
	@Column(length = 4000)
	private String description;

	@NotInCopy
	@Column(length = 4000)
	@Convert(converter = AttributeListConverter.class)
	@UiConfig(hiddenInList = @Hidden(true) )
	private List<Attribute> attributes = new ArrayList<>();

	public TreeNode() {

	}

	public TreeNode(String name) {
		this.name = name;
	}

	public TreeNode(String name, int displayOrder) {
		this.name = name;
		this.displayOrder = displayOrder;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public List<Attribute> getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return getDescription();
	}

}
