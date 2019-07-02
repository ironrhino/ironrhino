package org.ironrhino.common.service;

import java.util.List;
import java.util.Map;

import org.ironrhino.common.model.Page;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.service.BaseManager;

public interface PageManager extends BaseManager<Page> {

	Page getByPath(String path);

	Page saveDraft(Page page);

	Page getDraftByPath(String path);

	Page dropDraft(String id);

	void pullDraft(Page page);

	List<Page> findListByTag(String tag);

	List<Page> findListByTag(String... tag);

	Page[] findPreviousAndNextPage(Page page, String... tags);

	List<Page> findListByTag(int limit, String... tag);

	ResultPage<Page> findResultPageByTag(ResultPage<Page> resultPage, String tag);

	ResultPage<Page> findResultPageByTag(ResultPage<Page> resultPage, String... tag);

	Map<String, Integer> findMatchedTags(String keyword);

}
