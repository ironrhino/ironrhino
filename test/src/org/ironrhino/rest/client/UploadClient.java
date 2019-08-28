package org.ironrhino.rest.client;

import java.io.File;
import java.io.InputStream;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;

@RestApi(restClient = "restClient")
public interface UploadClient {

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	String upload(InputStream is);

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	String upload(byte[] bytes);

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	JsonNode upload(@RequestParam String name, @RequestParam File file);

}
