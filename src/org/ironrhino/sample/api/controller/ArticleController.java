package org.ironrhino.sample.api.controller;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ironrhino.rest.RestStatus;
import org.ironrhino.sample.api.model.Article;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArticleController implements ArticleService {

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

	@Override
	public Collection<Article> list() {
		return articles.values();
	}

	@Override
	public Article view(Integer id) {
		Article article = articles.get(id);
		if (article == null)
			throw RestStatus.NOT_FOUND;
		return article;
	}

	@Override
	public Article postForm(Article article) {
		return article;
	}

}
