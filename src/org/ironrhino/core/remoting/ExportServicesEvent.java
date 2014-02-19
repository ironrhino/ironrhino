package org.ironrhino.core.remoting;

import java.util.List;

import org.ironrhino.core.event.InstanceLifecycleEvent;

public class ExportServicesEvent extends InstanceLifecycleEvent {

	private static final long serialVersionUID = 4564138152726138645L;

	private List<String> exportServices;

	public ExportServicesEvent(List<String> exportServices) {
		this.exportServices = exportServices;
	}

	public List<String> getExportServices() {
		return exportServices;
	}

}
