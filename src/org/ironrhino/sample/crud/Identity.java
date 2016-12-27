package org.ironrhino.sample.crud;

import java.io.Serializable;

import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.ironrhino.core.metadata.UiConfig;

@Embeddable
public class Identity implements Serializable {

	private static final long serialVersionUID = 3472143402158894620L;

	@UiConfig(type = "dictionary", description = "identityType.description")
	private String identityType;

	private String identityNo;

	public Identity() {

	}

	public Identity(String str) {
		if (StringUtils.isNotBlank(str)) {
			String[] arr = str.split("-");
			identityType = arr[0];
			identityNo = arr[1];
		}
	}

	public String getIdentityType() {
		return identityType;
	}

	public void setIdentityType(String identityType) {
		this.identityType = identityType;
	}

	public String getIdentityNo() {
		return identityNo;
	}

	public void setIdentityNo(String identityNo) {
		this.identityNo = identityNo;
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(Object that) {
		return EqualsBuilder.reflectionEquals(this, that);
	}

	@Override
	public String toString() {
		return identityType + "-" + identityNo;
	}

}