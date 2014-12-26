package org.ironrhino.sample.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.springframework.core.annotation.Order;
import org.springframework.web.multipart.MultipartFile;

@ApiModule(value = "上传API")
@Order(2)
public class UploadControllerDoc extends UploadController {

	@Override
	@Api("上传文件")
	public Map<String, Object> upload(String name, MultipartFile file) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("name", "name");
		result.put("size", 10000);
		result.put("contentType", "application/zip");
		result.put("filename", "file");
		result.put("originalFilename", "test.zip");
		return result;
	}

}
