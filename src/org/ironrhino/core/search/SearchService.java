package org.ironrhino.core.search;

import java.util.List;
import java.util.Map;

import org.ironrhino.core.model.ResultPage;

public interface SearchService<T> {

	public ResultPage<T> search(ResultPage<T> resultPage);

	public List<T> search(SearchCriteria searchCriteria);

	public ResultPage<T> search(ResultPage<T> resultPage, Mapper<T> mapper);

	public List<T> search(SearchCriteria searchCriteria, Mapper<T> mapper);

	public List<T> search(SearchCriteria searchCriteria, Mapper<T> mapper,
			int limit);

	public Map<String, Integer> countTermsByField(
			SearchCriteria searchCriteria, String field);

	public static interface Mapper<T> {

		public T map(T source);

	}

}
