package org.ironrhino.sample.api.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ApiModule(value = "下载API")
@Order(3)
@RestController
@RequestMapping("/download")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class DownloadController {

	@Api("下载")
	@GetMapping
	public ResponseEntity<Resource> download(@RequestParam String filename) {
		HttpHeaders respHeaders = new HttpHeaders();
		respHeaders.setContentType(MediaType.TEXT_PLAIN);
		respHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=" + (filename.endsWith(".txt") ? filename : filename + ".txt"));
		InputStream is = new ByteArrayInputStream(filename.getBytes());
		return new ResponseEntity<>(new InputStreamResource(is), respHeaders, HttpStatus.OK);
	}

}