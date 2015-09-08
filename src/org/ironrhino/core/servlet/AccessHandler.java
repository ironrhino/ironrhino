package org.ironrhino.core.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccessHandler {

	public String getPattern() {
		return null;
	}

	public String getExcludePattern() {
		return null;
	}

	public boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return false;
	}

}
