package org.ironrhino.common.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.support.StatControl;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.Value;
import org.ironrhino.core.stat.analysis.TreeNode;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
public class MonitorAction extends BaseAction {

	private static final long serialVersionUID = -8946871669998582841L;

	@Getter
	@Setter
	private Date date;

	@Getter
	@Setter
	private Date from;

	@Getter
	@Setter
	private Date to;

	@Getter
	@Setter
	private boolean localhost;

	@Getter
	@Setter
	private String vtype = "l"; // value type l for longValue or d for
	// doubleValue

	@Getter
	@Setter
	private String ctype = "bar";// chart type, bar,line ...

	@Getter
	@Setter
	private String namespace;

	@Getter
	@Setter
	private String filter;

	@Getter
	private Map<String, List<TreeNode>> result;

	@Getter
	private TreeNode treeNode;

	@Getter
	private List<Tuple<Date, Number>> dataList;

	@Autowired
	private StatControl statControl;

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
				Map<String, List<TreeNode>> temp = new HashMap<>();
				temp.put(namespace, result.get(namespace));
				result = temp;
			}
			if (StringUtils.isNotBlank(filter)) {
				for (Map.Entry<String, List<TreeNode>> entry : result.entrySet()) {
					Iterator<TreeNode> it = entry.getValue().iterator();
					while (it.hasNext()) {
						TreeNode node = it.next();
						String path = node.getName();
						if (filter.equals(path) || filter.startsWith(path + ">")) {
							node.filter(filter);
							node.calculate();
						} else
							it.remove();
					}
				}
			}
		} catch (Exception e) {
			result = new HashMap<>();
		}
		return SUCCESS;
	}

	public String chart() {
		String id = getUid();
		if (StringUtils.isBlank(id))
			return NOTFOUND;
		Date today = new Date();
		if (date == null || date.after(today))
			date = today;
		dataList = new ArrayList<Tuple<Date, Number>>();
		boolean isdouble = "d".equalsIgnoreCase(vtype);
		boolean isline = "line".equalsIgnoreCase(ctype);
		List<Value> list = statControl.getPeriodResult(Key.fromString(id), date, isline, localhost);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		for (int i = 0; i < 24; i++) {
			Value val = list.get(i);
			cal.set(Calendar.HOUR_OF_DAY, i);
			Date d = cal.getTime();
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			c.set(Calendar.MINUTE, 30);
			c.set(Calendar.SECOND, 30);
			dataList.add(Tuple.of(c.getTime(), isdouble ? val.getDoubleValue() : val.getLongValue()));
		}
		return "chart";
	}

	public String pie() {
		String id = getUid();
		if (StringUtils.isBlank(id))
			return NOTFOUND;
		Key key = Key.fromString(id);
		try {
			if (from != null && to != null) {
				result = statControl.getResult(from, to, localhost);
			} else {
				Date today = new Date();
				if (date == null || date.after(today))
					date = today;
				result = statControl.getResult(date, localhost);
			}
			label: for (List<TreeNode> list : result.values()) {
				for (TreeNode t : list) {
					treeNode = t.getDescendantOrSelfByKey(key);
					if (treeNode != null)
						break label;
				}
			}
		} catch (Exception e) {
		}
		return treeNode != null ? "pie" : NOTFOUND;
	}
}
