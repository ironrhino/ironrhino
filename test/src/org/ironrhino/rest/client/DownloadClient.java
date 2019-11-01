package org.ironrhino.rest.client;

import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestApi(restClient = "restClient")
public interface DownloadClient {

	@GetMapping(value = "/download")
	public ResponseEntity<Resource> download(@RequestParam String filename);

	@GetMapping(value = "/download")
	public ResponseEntity<InputStream> downloadStream(@RequestParam String filename);

	@GetMapping(value = "/download")
	public Resource downloadDirectResource(@RequestParam String filename);

	@GetMapping(value = "/download")
	public InputStream downloadDirectStream(@RequestParam String filename);

}
