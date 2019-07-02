package org.ironrhino.core.search;

import java.util.List;
import java.util.Map;

import org.ironrhino.core.model.ResultPage;

public interface SearchService<T> {

	ResultPage<T> search(ResultPage<T> resultPage);

	List<T> search(SearchCriteria searchCriteria);

	ResultPage<T> search(ResultPage<T> resultPage, Mapper<T> mapper);

	List<T> search(SearchCriteria searchCriteria, Mapper<T> mapper);

	List<T> search(SearchCriteria searchCriteria, Mapper<T> mapper, int limit);

	Map<String, Integer> countTermsByField(SearchCriteria searchCriteria, String field);

	@FunctionalInterface
	interface Mapper<T> {

		T map(T source);

	}

}
