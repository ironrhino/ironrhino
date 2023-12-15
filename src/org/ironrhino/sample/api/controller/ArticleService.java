package org.ironrhino.sample.api.controller;

import java.util.Collection;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.View;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.ironrhino.rest.doc.annotation.Status;
import org.ironrhino.sample.api.model.Article;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.annotation.JsonView;

@RequestMapping("/article")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@ApiModule(value = "文章API")
public interface ArticleService {

	@Order(1)
	@Api(value = "获取文章列表", description = "只是文章概要信息, 不包含文章内容")
	@RequestMapping(method = RequestMethod.GET)
	@JsonView(View.Summary.class)
	Collection<Article> list();

	@Order(2)
	@Api(value = "获取文章详情", statuses = { @Status(code = 404, description = "文章不存在") })
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@JsonView(View.Detail.class)
	Article view(@PathVariable Integer id);

	@Order(3)
	@Api(value = "发布文章")
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	RestStatus put(@PathVariable Integer id, @RequestBody Article article);

	@RequestMapping(method = RequestMethod.POST)
	Article postForm(Article article);

	@RequestMapping(method = RequestMethod.POST, path = "/@ModelAttribute")
	Article postFormWithExplicitModelAttribute(@ModelAttribute Article article);
}
