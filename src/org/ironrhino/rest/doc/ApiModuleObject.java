package org.ironrhino.rest.doc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ApiModuleObject implements Serializable {

	private static final long serialVersionUID = 3211669537053956580L;

	private String name;

	private String description;

	private List<ApiDoc> apiDocs = new ArrayList<>();

}
