package org.ironrhino.common.action;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.support.StatControl;
import org.ironrhino.core.chart.openflashchart.Chart;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.analysis.TreeNode;
import org.ironrhino.core.struts.BaseAction;

@AutoConfig
public class MonitorAction extends BaseAction {

	private static final long serialVersionUID = -8946871669998582841L;

	private Date date;

	private Date from;

	private Date to;

	private boolean localhost;

	private String vtype = "l"; // value type l for longValue or d for
	// doubleValue

	private String ctype = "bar";// chart type, bar,line ...

	private String namespace;

	private String filter;

	private Map<String, List<TreeNode>> result;

	private Chart chart;

	@Autowired
	private transient StatControl statControl;

	public boolean isLocalhost() {
		return localhost;
	}

	public void setLocalhost(boolean localhost) {
		this.localhost = localhost;
	}

	public String getVtype() {
		return vtype;
	}

	public void setVtype(String vtype) {
		this.vtype = vtype;
	}

	public String getCtype() {
		return ctype;
	}

	public void setCtype(String ctype) {
		this.ctype = ctype;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public Date getFrom() {
		return from;
	}

	public void setFrom(Date from) {
		this.from = from;
	}

	public Date getTo() {
		return to;
	}

	public void setTo(Date to) {
		this.to = to;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Chart getChart() {
		return chart;
	}

	public Map<String, List<TreeNode>> getResult() {
		return result;
	}

	@Override
	public String execute() {
		try {
			if (from != null && to != null) {
				result = statControl.getResult(from, to, localhost);
			} else {
				Date today = new Date();
				if (date == null || date.after(today))
					date = today;
				result = statControl.getResult(date, localhost);
			}
			if (namespace != null) {
				Map<String, List<TreeNode>> temp = new HashMap<String, List<TreeNode>>();
				temp.put(namespace, result.get(namespace));
				result = temp;
			}
			if (StringUtils.isNotBlank(filter)) {
				for (Map.Entry<String, List<TreeNode>> entry : result
						.entrySet()) {
					Iterator<TreeNode> it = entry.getValue().iterator();
					while (it.hasNext()) {
						TreeNode node = it.next();
						String path = node.getName();
						if (filter.equals(path)
								|| filter.startsWith(path + ">")) {
							node.filter(filter);
							node.calculate();
						} else
							it.remove();
					}
				}
			}
		} catch (Exception e) {
			result = new HashMap<String, List<TreeNode>>();
		}
		return SUCCESS;
	}

	public String chart() {
		Date today = new Date();
		if (date == null || date.after(today))
			date = today;
		return "chart";
	}

	@JsonConfig(root = "chart")
	public String data() {
		chart = statControl.getChart(Key.fromString(getUid()), date, vtype,
				ctype, localhost);
		return JSON;
	}
}
