package org.ironrhino.rest.client;

import org.ironrhino.sample.api.controller.ArticleService;

@RestApi(restClient = "restClient")
public interface ArticleClient extends ArticleService {

}
