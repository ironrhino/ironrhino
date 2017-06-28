package org.ironrhino.core.security.verfication;

import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface VerificationAware extends UserDetails {

	@JsonIgnore
	public default boolean isVerificationRequired() {
		return true;
	}

	@JsonIgnore
	public default boolean isPasswordRequired() {
		return getPassword() != null;
	}

	@JsonIgnore
	public String getReceiver();

}
