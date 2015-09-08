package org.ironrhino.security.oauth.client.service;

import javax.servlet.http.HttpServletRequest;

import org.ironrhino.security.oauth.client.model.OAuthToken;
import org.ironrhino.security.oauth.client.model.Profile;

public interface OAuthProvider extends Comparable<OAuthProvider> {

	public String getVersion();

	public String getName();

	public String getLogo();

	public boolean isEnabled();

	public String getAuthRedirectURL(HttpServletRequest request, String targetUrl) throws Exception;

	public Profile getProfile(HttpServletRequest request) throws Exception;

	public OAuthToken getToken(HttpServletRequest request) throws Exception;

}
