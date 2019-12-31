package org.ironrhino.core.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

	public static final String MARKER_PARAM_NAME = "m";

	public static final String PREVIOUSMARKER_PARAM_NAME = "pm";

	public static final int DEFAULT_MAX_PAGESIZE = 1000;

	public static ThreadLocal<Integer> MAX_PAGESIZE = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return DEFAULT_MAX_PAGESIZE;
		}
	};

	@Setter
	@Getter
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
	private boolean counting = true;

	@JsonIgnore
	@Getter
	@Setter
	private boolean paged = true;

	@Getter
	@Setter
	private long tookInMillis = -1;

	@Getter
	private Collection<T> result;

	@JsonIgnore
	@Getter
	@Setter
	boolean useKeysetPagination;

	@JsonIgnore
	@Getter
	@Setter
	protected String marker;

	@JsonIgnore
	@Getter
	@Setter
	protected String previousMarker;

	@JsonIgnore
	@Getter
	@Setter
	protected String nextMarker;

	public void setPageSize(int pageSize) {
		if (pageSize > MAX_PAGESIZE.get())
			pageSize = MAX_PAGESIZE.get();
		if (pageSize < 1)
			pageSize = DEFAULT_PAGE_SIZE;
		this.pageSize = pageSize;
	}

	public void setResult(Collection<T> result) {
		this.result = result;
	}

	public int getStart() {
		return (pageNo - 1) * pageSize;
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

	public String renderUrl(int pn) throws UnsupportedEncodingException {
		return doRenderUrl(PAGENO_PARAM_NAME, pn > 1 ? String.valueOf(pn) : null);
	}

	public String renderUrlWithMarker(boolean forward) throws UnsupportedEncodingException {
		String url;
		if (forward) {
			url = doRenderUrl(MARKER_PARAM_NAME, nextMarker);
			if (StringUtils.isNotBlank(marker)) {
				List<String> history = new ArrayList<>();
				if (StringUtils.isNotBlank(previousMarker))
					history.addAll(Arrays.asList(previousMarker.split(",")));
				history.add(marker);
				int maxlength = 5;
				if (history.size() > maxlength) {
					history = history.subList(history.size() - maxlength, history.size());
				}
				url += '&' + PREVIOUSMARKER_PARAM_NAME + '=' + URLEncoder.encode(String.join(",", history), "UTF-8");
			}
		} else {
			if (StringUtils.isNotBlank(previousMarker)) {
				int index = previousMarker.lastIndexOf(',');
				if (index > 0) {
					url = doRenderUrl(MARKER_PARAM_NAME, previousMarker.substring(index + 1));
					url += '&' + PREVIOUSMARKER_PARAM_NAME + '='
							+ URLEncoder.encode(previousMarker.substring(0, index), "UTF-8");
				} else {
					url = doRenderUrl(MARKER_PARAM_NAME, previousMarker);
				}
			} else {
				url = doRenderUrl(MARKER_PARAM_NAME, "");
			}
		}
		return url;
	}

	private String doRenderUrl(String name, String value) throws UnsupportedEncodingException {
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
			if (StringUtils.isBlank(value))
				return sb.toString();
			else
				return sb.append(StringUtils.isNotBlank(parameterString) ? "&" : "?").append(name).append("=")
						.append(value).toString();
		} else {
			if (StringUtils.isBlank(value))
				return sb.append(StringUtils.isNotBlank(parameterString) ? "&" : "?").append(PAGESIZE_PARAM_NAME)
						.append("=").append(pageSize).toString();
			else
				return sb.append(StringUtils.isNotBlank(parameterString) ? "&" : "?").append(name).append("=")
						.append(value).append("&").append(PAGESIZE_PARAM_NAME).append("=").append(pageSize).toString();
		}
	}

	private String _parameterString;

	private String _getParameterString() throws UnsupportedEncodingException {
		if (_parameterString == null) {
			StringBuilder sb = new StringBuilder();
			Map<String, String[]> map = RequestContext.getRequest().getParameterMap();
			for (Map.Entry<String, String[]> entry : map.entrySet()) {
				String name = entry.getKey();
				String[] values = entry.getValue();
				if (values.length == 1 && values[0].isEmpty() || name.equals("_") || name.equals(PAGENO_PARAM_NAME)
						|| name.equals(PAGESIZE_PARAM_NAME) || name.equals(MARKER_PARAM_NAME)
						|| name.equals(PREVIOUSMARKER_PARAM_NAME)
						|| name.startsWith(StringUtils.uncapitalize(ResultPage.class.getSimpleName()) + '.'))
					continue;
				for (String value : values)
					sb.append(name).append('=')
							.append(URLEncoder.encode(value.length() > 256 ? value.substring(0, 256) : value, "UTF-8"))
							.append('&');
			}
			if (sb.length() > 0)
				sb.deleteCharAt(sb.length() - 1);
			_parameterString = sb.toString();
		}
		return _parameterString;
	}

}
