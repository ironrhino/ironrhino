package org.ironrhino.sample.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/upload")
public class UploadController {

	@RequestMapping(method = RequestMethod.GET)
	public String form() {
		return "sample/upload";
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> upload(@RequestParam("name") String name,
			@RequestParam("file") MultipartFile file) {
		Map<String, Object> result = new HashMap<String, Object>();
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