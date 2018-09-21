package org.ironrhino.sample.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.security.role.UserRole;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class UploadController {

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public String upload(@RequestBody String string) {
		return string;
	}

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> upload(@RequestParam String name, @RequestParam MultipartFile file) {
		Map<String, Object> result = new HashMap<>();
		result.put("name", name);
		if (!file.isEmpty()) {
			result.put("size", file.getSize());
			result.put("contentType", file.getContentType());
			result.put("filename", file.getName());
			result.put("originalFilename", file.getOriginalFilename());
		}
		return result;
	}

}