package org.ironrhino.rest.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ApiModuleObject implements Serializable {

	private static final long serialVersionUID = 3211669537053956580L;

	private String name;

	private String description;

	private List<ApiDoc> apiDocs = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ApiDoc> getApiDocs() {
		return apiDocs;
	}

	public void setApiDocs(List<ApiDoc> apiDocs) {
		this.apiDocs = apiDocs;
	}

}
