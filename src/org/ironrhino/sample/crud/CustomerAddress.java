package org.ironrhino.sample.crud;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.ironrhino.common.model.Region;
import org.ironrhino.core.metadata.UiConfig;

import lombok.Data;

@Embeddable
@Data
public class CustomerAddress implements Serializable {

	private static final long serialVersionUID = -2175577393105618397L;

	@UiConfig(width = "150px", cssClass = "input-medium decrease")
	@Column(nullable = false)
	private AddressType type;

	@UiConfig(width = "150px")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver")
	private Employee receiver;

	@UiConfig(width = "250px", template = "<#if value?has_content>${value.fullname}</#if>", description = "region.description")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "region")
	private Region region;

	@UiConfig(cssClass = "input-xxlarge")
	private String address;

	@UiConfig(width = "80px")
	private boolean active;

}