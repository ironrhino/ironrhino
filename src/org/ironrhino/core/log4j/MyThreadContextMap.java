package org.ironrhino.core.log4j;

import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.ironrhino.core.util.AppInfo;

public class MyThreadContextMap extends DefaultThreadContextMap {

	private static final long serialVersionUID = -4169775416911265613L;

	public MyThreadContextMap() {
		this(true);
	}

	public MyThreadContextMap(boolean useMap) {
		super(useMap);
		put("server", " server:" + AppInfo.getInstanceId(true));
	}

}
