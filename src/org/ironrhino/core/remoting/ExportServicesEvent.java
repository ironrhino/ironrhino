package org.ironrhino.core.remoting;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ironrhino.core.event.InstanceLifecycleEvent;

import lombok.Getter;

@Getter
public class ExportServicesEvent extends InstanceLifecycleEvent {

	private static final long serialVersionUID = 4564138152726138645L;

	private List<String> exportServices;

	private Map<String, String> servicePaths;

	public ExportServicesEvent(List<String> exportServices) {
		this.exportServices = exportServices;
		this.servicePaths = Collections.emptyMap();
	}

	public ExportServicesEvent(List<String> exportServices, Map<String, String> servicePaths) {
		this.exportServices = exportServices;
		this.servicePaths = servicePaths;
	}

}
