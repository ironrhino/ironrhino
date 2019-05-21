package org.ironrhino.sample.api.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collection")
@ApiModule("集合类型示例")
public class CollectionController {

	@Order(1)
	@Api("list")
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	public List<String> list(@RequestBody List<String> list) {
		return list;
	}

	@Order(2)
	@Api("set")
	@RequestMapping(value = "/set", method = RequestMethod.POST)
	public Set<String> list(@RequestBody Set<String> set) {
		return set;
	}

	@Order(3)
	@Api("map")
	@RequestMapping(value = "/map", method = RequestMethod.POST)
	public Map<String, String> map(@RequestBody Map<String, String> map) {
		return map;
	}

}
