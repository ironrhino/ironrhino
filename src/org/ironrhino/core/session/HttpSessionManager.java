package org.ironrhino.core.session;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

public interface HttpSessionManager extends HttpSessionStore {

	String REQUEST_ATTRIBUTE_KEY_SESSION_ID_FOR_API = "_session_id_in_request_for_api";

	String REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API = "_session_map_in_request_for_api";

	String REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_SSO = "_session_map_in_request_for_sso";

	String REQUEST_ATTRIBUTE_SESSION_TRACKER_IN_URL = "_session_tracker_in_url_";

	String DEFAULT_SESSION_TRACKER_NAME = "T";
	
	String DEFAULT_SESSION_COOKIE_NAME = "s";

	String DEFAULT_COOKIE_NAME_LOCALE = "locale";

	int DEFAULT_LIFETIME = -1; // in seconds

	int DEFAULT_MAXINACTIVEINTERVAL = 43200; // in seconds

	int DEFAULT_MINACTIVEINTERVAL = 60;// in seconds

	public String getSessionId(HttpServletRequest request);

	public String getSessionTracker(WrappedHttpSession session);

	public String getSessionTrackerName();

	public boolean supportSessionTrackerFromURL();

	public String getLocaleCookieName();

	public Locale getLocale(HttpServletRequest request);

}
