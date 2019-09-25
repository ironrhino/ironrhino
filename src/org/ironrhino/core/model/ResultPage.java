package org.ironrhino.core.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.RequestContext;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

public class ResultPage<T> implements Serializable {

	private static final long serialVersionUID = -3653886488085413894L;

	public static final int DEFAULT_PAGE_SIZE = 10;

	public static final String PAGENO_PARAM_NAME = "pn";

	public static final String PAGESIZE_PARAM_NAME = "ps";

	public static final int DEFAULT_MAX_PAGESIZE = 1000;

	public static ThreadLocal<Integer> MAX_PAGESIZE = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return DEFAULT_MAX_PAGESIZE;
		}
	};

	@Setter
	private int pageNo = 1;

	@Getter
	private int pageSize = DEFAULT_PAGE_SIZE;

	private int totalPage = 0;

	@Getter
	@Setter
	private long totalResults = -1;

	@JsonIgnore
	private Object criteria;

	@JsonIgnore
	@Getter
	@Setter
	private boolean reverse;

	@JsonIgnore
	@Getter
	@Setter
	private boolean counting = true;

	@JsonIgnore
	@Getter
	@Setter
	private boolean paged = true;

	@JsonIgnore
	@Getter
	private boolean executed;

	@Getter
	@Setter
	private long tookInMillis;

	@JsonIgnore
	private int start = -1;

	@Getter
	private Collection<T> result = new ArrayList<>(0);

	public int getStart() {
		return (this.pageNo - 1) * this.pageSize;
	}

	public void setStart(int start) {
		this.pageNo = start / pageSize + 1;
	}

	public int getPageNo() {
		if (start >= 0)
			return start / pageSize + 1;
		return pageNo;
	}

	public void setPageSize(int pageSize) {
		if (pageSize > MAX_PAGESIZE.get())
			pageSize = MAX_PAGESIZE.get();
		if (pageSize < 1)
			pageSize = DEFAULT_PAGE_SIZE;
		this.pageSize = pageSize;
	}

	public void setResult(Collection<T> result) {
		this.result = result;
		this.executed = true;
	}

	public int getTotalPage() {
		totalPage = (int) (totalResults % pageSize == 0 ? totalResults / pageSize : totalResults / pageSize + 1);
		return totalPage;
	}

	@SuppressWarnings("unchecked")
	public <CT> CT getCriteria() {
		return (CT) criteria;
	}

	public <CT> void setCriteria(CT criteria) {
		this.criteria = criteria;
	}

	@JsonIgnore
	public boolean isFirst() {
		return this.pageNo <= 1;
	}

	@JsonIgnore
	public boolean isLast() {
		return this.pageNo >= getTotalPage();
	}

	@JsonIgnore
	public int getPreviousPage() {
		return this.pageNo > 1 ? this.pageNo - 1 : 1;
	}

	@JsonIgnore
	public int getNextPage() {
		return this.pageNo < getTotalPage() ? this.pageNo + 1 : getTotalPage();
	}

	@JsonIgnore
	public boolean isDefaultPageSize() {
		return this.pageSize == DEFAULT_PAGE_SIZE;
	}

	@JsonIgnore
	public boolean isCanListAll() {
		return this.totalResults <= DEFAULT_MAX_PAGESIZE;
	}

	public String renderUrl(int pn) {
		HttpServletRequest request = RequestContext.getRequest();
		String requestURI = (String) request.getAttribute("struts.request_uri");
		if (requestURI == null)
			requestURI = (String) request.getAttribute("javax.servlet.forward.request_uri");
		if (requestURI == null)
			requestURI = request.getRequestURI();
		StringBuilder sb = new StringBuilder(requestURI);
		String parameterString = _getParameterString();
		if (StringUtils.isNotBlank(parameterString))
			sb.append("?").append(parameterString);
		if (isDefaultPageSize()) {
			if (pn <= 1)
				return sb.toString();
			else
				return sb.append(StringUtils.isNotBlank(parameterString) ? "&" : "?").append(PAGENO_PARAM_NAME)
						.append("=").append(pn).toString();
		} else {
			if (pn <= 1)
				return sb.append(StringUtils.isNotBlank(parameterString) ? "&" : "?").append(PAGESIZE_PARAM_NAME)
						.append("=").append(pageSize).toString();
			else
				return sb.append(StringUtils.isNotBlank(parameterString) ? "&" : "?").append(PAGENO_PARAM_NAME)
						.append("=").append(pn).append("&").append(PAGESIZE_PARAM_NAME).append("=").append(pageSize)
						.toString();
		}
	}

	private String _parameterString;

	private String _getParameterString() {
		if (_parameterString == null) {
			StringBuilder sb = new StringBuilder();
			Map<String, String[]> map = RequestContext.getRequest().getParameterMap();
			for (Map.Entry<String, String[]> entry : map.entrySet()) {
				String name = entry.getKey();
				String[] values = entry.getValue();
				if (values.length == 1 && values[0].equals("") || name.equals("_") || name.equals(PAGENO_PARAM_NAME)
						|| name.equals(PAGESIZE_PARAM_NAME)
						|| name.startsWith(StringUtils.uncapitalize(ResultPage.class.getSimpleName()) + '.'))
					continue;
				try {
					for (String value : values)
						sb.append(name).append('=').append(
								URLEncoder.encode(value.length() > 256 ? value.substring(0, 256) : value, "UTF-8"))
								.append('&');
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			if (sb.length() > 0)
				sb.deleteCharAt(sb.length() - 1);
			_parameterString = sb.toString();
		}
		return _parameterString;
	}

}
