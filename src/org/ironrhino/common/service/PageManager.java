package org.ironrhino.common.service;

import java.util.List;
import java.util.Map;

import org.ironrhino.common.model.Page;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.service.BaseManager;

public interface PageManager extends BaseManager<Page> {

	public Page getByPath(String path);

	public Page saveDraft(Page page);

	public Page getDraftByPath(String path);

	public Page dropDraft(String id);

	public void pullDraft(Page page);

	public List<Page> findListByTag(String tag);

	public List<Page> findListByTag(String... tag);

	public Page[] findPreviousAndNextPage(Page page, String... tags);

	public List<Page> findListByTag(int limit, String... tag);

	public ResultPage<Page> findResultPageByTag(ResultPage<Page> resultPage,
			String tag);

	public ResultPage<Page> findResultPageByTag(ResultPage<Page> resultPage,
			String... tag);

	public Map<String, Integer> findMatchedTags(String keyword);

}
