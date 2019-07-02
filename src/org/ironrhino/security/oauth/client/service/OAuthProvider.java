package org.ironrhino.security.oauth.client.service;

import javax.servlet.http.HttpServletRequest;

import org.ironrhino.security.oauth.client.model.OAuthToken;
import org.ironrhino.security.oauth.client.model.Profile;

public interface OAuthProvider extends Comparable<OAuthProvider> {

	String getVersion();

	String getName();

	String getLogo();

	boolean isEnabled();

	String getAuthRedirectURL(HttpServletRequest request, String targetUrl) throws Exception;

	Profile getProfile(HttpServletRequest request) throws Exception;

	OAuthToken getToken(HttpServletRequest request) throws Exception;

}
