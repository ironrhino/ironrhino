package org.ironrhino.rest.client;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RestApi(restClient = "restClient")
public interface UploadClient {

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String upload(InputStream is);

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String upload(byte[] bytes);

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public Map<String, String> upload(@RequestParam String name, @RequestParam File file);

}
