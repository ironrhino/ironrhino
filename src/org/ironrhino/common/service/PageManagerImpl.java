package org.ironrhino.common.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.Page;
import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.cache.EvictCache;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.search.elasticsearch.ElasticSearchCriteria;
import org.ironrhino.core.search.elasticsearch.ElasticSearchService;
import org.ironrhino.core.service.BaseManagerImpl;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ValueThenKeyComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PageManagerImpl extends BaseManagerImpl<Page> implements
		PageManager {

	@Autowired(required = false)
	private transient ElasticSearchService<Page> elasticSearchService;

	@Override
	@Transactional
	@EvictCache(key = "${page.path}", namespace = "page", renew = "${page}")
	public void save(Page page) {
		page.setDraft(null);
		page.setDraftDate(null);
		super.save(page);
	}

	@Override
	@Transactional
	@EvictCache(key = "${page.path}", namespace = "page")
	public void delete(Page page) {
		super.delete(page);
	}

	@Override
	@Transactional
	@EvictCache(key = "${key = [];foreach (page : retval) { key.add(page.path); } return key;}", namespace = "page")
	public List<Page> delete(Serializable... id) {
		return super.delete(id);
	}

	@Override
	@Transactional(readOnly = true)
	@CheckCache(key = "${path}", namespace = "page", eternal = true)
	public Page getByPath(String path) {
		Page page = findByNaturalId(path);
		if (page != null)
			page.setDraft(null);
		return page;
	}

	@Override
	@Transactional
	public Page saveDraft(Page page) {
		Page p = get(page.getId());
		boolean isnew = false;
		if (p == null) {
			isnew = true;
			p = page;
		}
		p.setDraftDate(new Date());
		Map<String, String> draft = new HashMap<String, String>(8);
		draft.put("path", page.getPath());
		draft.put("title", page.getTitle());
		draft.put("content", page.getContent());
		p.setDraft(JsonUtils.toJson(draft));
		if (isnew) {
			p.setTitle("");
			p.setContent("");
		}
		super.save(p);
		return p;
	}

	@Override
	@Transactional(readOnly = true)
	public Page getDraftByPath(String path) {
		Page page = findByNaturalId(path);
		if (page == null || StringUtils.isBlank(page.getDraft()))
			return null;
		pullDraft(page);
		return page;
	}

	@Override
	@Transactional
	public Page dropDraft(String id) {
		Page page = get(id);
		page.setDraft(null);
		page.setDraftDate(null);
		super.save(page);
		return page;
	}

	@Override
	public void pullDraft(Page page) {
		try {
			Map<String, String> map = JsonUtils.fromJson(page.getDraft(),
					JsonUtils.STRING_MAP_TYPE);
			page.setPath(map.get("path"));
			page.setTitle(map.get("title"));
			page.setContent(map.get("content"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Page> findListByTag(String tag) {
		return findListByTag(new String[] { tag });
	}

	@Override
	@Transactional(readOnly = true)
	public List<Page> findListByTag(String... tag) {
		return findListByTag(-1, tag);
	}

	@Override
	@Transactional(readOnly = true)
	public Page[] findPreviousAndNextPage(Page page, String... tags) {
		Page[] arr = new Page[2];
		DetachedCriteria dc = detachedCriteria();
		for (String tag : tags)
			if (StringUtils.isNotBlank(tag))
				dc.add(CriterionUtils.matchTag("tagsAsString", tag));
		dc.add(Restrictions.gt("createDate", page.getCreateDate()));
		dc.add(Restrictions.le("displayOrder", page.getDisplayOrder()));
		dc.addOrder(Order.desc("displayOrder"));
		dc.addOrder(Order.asc("createDate"));
		arr[0] = findByCriteria(dc);
		dc = detachedCriteria();
		for (String tag : tags)
			if (StringUtils.isNotBlank(tag))
				dc.add(CriterionUtils.matchTag("tagsAsString", tag));
		dc.add(Restrictions.lt("createDate", page.getCreateDate()));
		dc.add(Restrictions.ge("displayOrder", page.getDisplayOrder()));
		dc.addOrder(Order.asc("displayOrder"));
		dc.addOrder(Order.desc("createDate"));
		arr[1] = findByCriteria(dc);
		return arr;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<Page> findListByTag(int limit, String... tag) {
		if (tag.length == 0 || StringUtils.isBlank(tag[0]))
			return Collections.EMPTY_LIST;
		List<Page> list;
		if (elasticSearchService != null) {
			String query = null;
			if (tag.length == 1) {
				query = "tags:" + tag[0];
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("tags:").append(tag[0]);
				for (int i = 1; i < tag.length; i++)
					sb.append(" AND ").append("tags:").append(tag[i]);
				query = sb.toString();
			}
			ElasticSearchCriteria criteria = new ElasticSearchCriteria();
			criteria.setQuery(query);
			criteria.setTypes(new String[] { "page" });
			criteria.addSort("displayOrder", false);
			criteria.addSort("createDate", true);
			if (limit > 0)
				list = elasticSearchService.search(criteria, null, limit);
			else
				list = elasticSearchService.search(criteria);
		} else {
			DetachedCriteria dc = detachedCriteria();
			dc.addOrder(Order.asc("displayOrder"));
			dc.addOrder(Order.desc("createDate"));
			for (int i = 0; i < tag.length; i++) {
				if (StringUtils.isNotBlank(tag[i]))
					dc.add(CriterionUtils.matchTag("tagsAsString", tag[i]));
			}
			if (limit > 0)
				list = findListByCriteria(dc, 1, limit);
			else
				list = findListByCriteria(dc);
		}
		return list;
	}

	@Override
	@Transactional(readOnly = true)
	public ResultPage<Page> findResultPageByTag(ResultPage<Page> resultPage,
			String tag) {
		return findResultPageByTag(resultPage, new String[] { tag });
	}

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public ResultPage<Page> findResultPageByTag(ResultPage<Page> resultPage,
			String... tag) {
		if (tag.length == 0 || StringUtils.isBlank(tag[0])) {
			resultPage.setResult(Collections.EMPTY_LIST);
			return resultPage;
		}

		String query = null;
		if (tag.length == 1) {
			query = "tags:" + tag[0];
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("tags:").append(tag[0]);
			for (int i = 1; i < tag.length; i++)
				sb.append(" AND ").append("tags:").append(tag[i]);
			query = sb.toString();
		}
		ElasticSearchCriteria criteria = resultPage.getCriteria();
		if (criteria == null) {
			criteria = new ElasticSearchCriteria();
			resultPage.setCriteria(criteria);
		}
		criteria.setQuery(query);
		criteria.setTypes(new String[] { "page" });
		if (criteria.getSorts().size() == 0) {
			criteria.addSort("displayOrder", false);
			criteria.addSort("createDate", true);
		}

		if (elasticSearchService != null) {
			resultPage = elasticSearchService.search(resultPage);
		} else {
			DetachedCriteria dc = detachedCriteria();
			for (int i = 0; i < tag.length; i++)
				if (StringUtils.isNotBlank(tag[i]))
					dc.add(CriterionUtils.matchTag("tagsAsString", tag[i]));
			for (Map.Entry<String, Boolean> entry : criteria.getSorts()
					.entrySet())
				dc.addOrder(entry.getValue() ? Order.desc(entry.getKey())
						: Order.asc(entry.getKey()));
			resultPage.setCriteria(dc);
			resultPage = findByResultPage(resultPage);
		}
		return resultPage;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public Map<String, Integer> findMatchedTags(String keyword) {
		if (keyword == null || keyword.length() < 2)
			return Collections.EMPTY_MAP;
		if (elasticSearchService != null) {
			ElasticSearchCriteria cc = new ElasticSearchCriteria();
			cc.setQuery(new StringBuilder("tags:").append(keyword).append("*")
					.toString());
			cc.setTypes(new String[] { "page" });
			Map<String, Integer> map = elasticSearchService.countTermsByField(
					cc, "tags");
			Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Integer> entry = it.next();
				if (!entry.getKey().startsWith(keyword))
					it.remove();
			}
			return map;
		} else {
			final Map<String, Integer> map = new HashMap<String, Integer>();
			DetachedCriteria dc = detachedCriteria();
			dc.add(Restrictions.like("tagsAsString", keyword,
					MatchMode.ANYWHERE));
			List<Page> list = findListByCriteria(dc);
			for (Page p : list) {
				for (String tag : p.getTags()) {
					if (!tag.startsWith(keyword))
						continue;
					Integer count = map.get(tag);
					if (count != null)
						map.put(tag, map.get(tag) + 1);
					else
						map.put(tag, 1);
				}
			}

			List<Map.Entry<String, Integer>> _list = new ArrayList<Map.Entry<String, Integer>>(
					map.entrySet());
			Collections.sort(_list, ValueThenKeyComparator
					.<String, Integer> getDefaultInstance());
			Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
			for (Map.Entry<String, Integer> entry : _list)
				sortedMap.put(entry.getKey(), entry.getValue());
			return sortedMap;
		}

	}

}
