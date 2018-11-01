package org.ironrhino.sample.api.controller;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.View;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.ironrhino.rest.doc.annotation.Status;
import org.ironrhino.sample.api.model.Article;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/article")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@ApiModule(value = "文章API")
public class ArticleController {

	private Map<Integer, Article> articles;

	public ArticleController() {
		articles = new LinkedHashMap<>();
		for (int i = 1; i <= 10; i++) {
			Article article = new Article();
			article.setId(i);
			article.setTitle("Title" + i);
			article.setAuthor("Author" + i);
			article.setPublishDate(LocalDate.now());
			article.setContent("this is content");
			articles.put(article.getId(), article);
		}
	}

	@Order(1)
	@Api(value = "获取文章列表", description = "只是文章概要信息, 不包含文章内容")
	@RequestMapping(method = RequestMethod.GET)
	@JsonView(View.Summary.class)
	public Collection<Article> list() {
		return articles.values();
	}

	@Order(3)
	@Api(value = "获取文章详情", statuses = { @Status(code = 404, description = "文章不存在") })
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@JsonView(View.Detail.class)
	public Article view(@PathVariable Integer id) {
		Article article = articles.get(id);
		if (article == null)
			throw RestStatus.NOT_FOUND;
		return article;
	}

}
