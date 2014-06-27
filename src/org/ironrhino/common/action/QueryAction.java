package org.ironrhino.common.action;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.jdbc.JdbcQueryService;
import org.ironrhino.core.jdbc.LineHandler;
import org.ironrhino.core.jdbc.QueryCriteria;
import org.ironrhino.core.jdbc.SqlUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class QueryAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	protected String sql;

	protected Set<String> params;

	protected List<String> tables;

	protected Map<String, String> paramMap = new HashMap<String, String>();

	protected ResultPage<Map<String, Object>> resultPage;

	@Autowired
	protected transient JdbcQueryService jdbcQueryService;

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public Map<String, String> getParamMap() {
		return paramMap;
	}

	public void setParamMap(Map<String, String> paramMap) {
		this.paramMap = paramMap;
	}

	public Set<String> getParams() {
		return params;
	}

	public List<String> getTables() {
		return tables;
	}

	public ResultPage<Map<String, Object>> getResultPage() {
		return resultPage;
	}

	public void setResultPage(ResultPage<Map<String, Object>> resultPage) {
		this.resultPage = resultPage;
	}

	@Override
	public String execute() {
		tables = jdbcQueryService.getTables();
		if (StringUtils.isNotBlank(sql)) {
			jdbcQueryService.validate(sql);
			params = SqlUtils.extractParameters(sql);
			if (params.size() > 0) {
				for (String s : params) {
					if (!paramMap.containsKey(s)) {
						return SUCCESS;
					}
				}
			}
			if (resultPage == null) {
				resultPage = new ResultPage<Map<String, Object>>();
			}
			Map<String, String> copy = new HashMap<String, String>();
			copy.putAll(paramMap);
			resultPage.setCriteria(new QueryCriteria(sql, copy));
			resultPage = jdbcQueryService.query(resultPage);
		}
		return SUCCESS;
	}

	public String export() throws Exception {
		if (StringUtils.isNotBlank(sql)) {
			jdbcQueryService.validate(sql);
			params = SqlUtils.extractParameters(sql);
			if (params.size() > 0) {
				for (String s : params) {
					if (!paramMap.containsKey(s)) {
						return SUCCESS;
					}
				}
			}
			long count = jdbcQueryService.count(sql, paramMap);
			if (count > 10 * ResultPage.DEFAULT_MAX_PAGESIZE)
				throw new ErrorMessage("query.result.number.exceed",
						new Object[] { 10 * ResultPage.DEFAULT_MAX_PAGESIZE });
			HttpServletResponse response = ServletActionContext.getResponse();
			response.setHeader("Content-type", "text/csv");
			response.setHeader("Content-disposition",
					"attachment;filename=data.csv");
			final PrintWriter writer = response.getWriter();
			jdbcQueryService.query(sql, paramMap, new LineHandler() {
				@Override
				public boolean isWithHeader() {
					return true;
				}

				@Override
				public String getColumnSeperator() {
					return ",";
				}

				@Override
				public String getLineSeperator() {
					return "\r\n";
				}

				@Override
				public void handleLine(int index, String line) {
					writer.write(line);
					writer.write(getLineSeperator());
					if (index > 0 && index % 100 == 0)
						writer.flush();
				}
			});
			writer.close();
		}
		return NONE;
	}
}
