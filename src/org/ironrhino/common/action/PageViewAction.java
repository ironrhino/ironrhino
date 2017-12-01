package org.ironrhino.common.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.service.PageViewService;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
public class PageViewAction extends BaseAction {

	private static final long serialVersionUID = -6901193289995112304L;

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
	private List<Tuple<Date, Long>> dataList;

	@Getter
	private Tuple<Date, Long> max;

	@Getter
	private Long total;

	@Setter
	private int limit;

	@Getter
	@Setter
	private String domain;

	@Getter
	private Set<String> domains;

	@Getter
	private Map<String, Long> dataMap;

	@Autowired
	private PageViewService pageViewService;

	@Override
	public String execute() {
		domains = pageViewService.getDomains();
		return SUCCESS;
	}

	public String pv() {
		if (from != null && to != null && from.before(to)) {
			dataList = new ArrayList<>();
			Date date = from;
			while (!date.after(to)) {
				String key = DateUtils.formatDate8(date);
				Long value = pageViewService.getPageView(key, domain);
				dataList.add(Tuple.of(date, value));
				date = DateUtils.addDays(date, 1);
			}
			Tuple<String, Long> p = pageViewService.getMaxPageView(domain);
			if (p != null)
				max = Tuple.of(DateUtils.parseDate8(p.getKey()), p.getValue());
			long value = pageViewService.getPageView(null, domain);
			if (value > 0)
				total = value;
			return "linechart";
		} else {
			if (date == null)
				date = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			dataList = new ArrayList<>();
			for (int i = 0; i < 24; i++) {
				cal.set(Calendar.HOUR_OF_DAY, i);
				if (cal.getTime().before(new Date())) {
					Date d = cal.getTime();
					String key = DateUtils.format(d, "yyyyMMddHH");
					Long value = pageViewService.getPageView(key, domain);
					Calendar c = Calendar.getInstance();
					c.setTime(d);
					c.set(Calendar.MINUTE, 30);
					c.set(Calendar.SECOND, 30);
					dataList.add(Tuple.of(c.getTime(), value));
				} else {
					dataList.add(Tuple.of(cal.getTime(), 0L));
				}
			}
			return "barchart";
		}
	}

	public String uip() {
		if (!(from != null && to != null && from.before(to))) {
			to = DateUtils.beginOfDay(new Date());
			from = DateUtils.addDays(to, -30);
		}
		dataList = new ArrayList<>();
		Date date = from;
		while (!date.after(to)) {
			String key = DateUtils.formatDate8(date);
			Long value = pageViewService.getUniqueIp(key, domain);
			dataList.add(Tuple.of(date, value));
			date = DateUtils.addDays(date, 1);
		}
		Tuple<String, Long> p = pageViewService.getMaxUniqueIp(domain);
		if (p != null)
			max = Tuple.of(DateUtils.parseDate8(p.getKey()), p.getValue());
		return "linechart";
	}

	public String usid() {
		if (!(from != null && to != null && from.before(to))) {
			to = DateUtils.beginOfDay(new Date());
			from = DateUtils.addDays(to, -30);
		}
		dataList = new ArrayList<>();
		Date date = from;
		while (!date.after(to)) {
			String key = DateUtils.formatDate8(date);
			Long value = pageViewService.getUniqueSessionId(key, domain);
			dataList.add(Tuple.of(date, value));
			date = DateUtils.addDays(date, 1);
		}
		Tuple<String, Long> p = pageViewService.getMaxUniqueSessionId(domain);
		if (p != null)
			max = Tuple.of(DateUtils.parseDate8(p.getKey()), p.getValue());
		return "linechart";
	}

	public String uu() {
		if (!(from != null && to != null && from.before(to))) {
			to = DateUtils.beginOfDay(new Date());
			from = DateUtils.addDays(to, -30);
		}
		dataList = new ArrayList<>();
		Date date = from;
		while (!date.after(to)) {
			String key = DateUtils.formatDate8(date);
			Long value = pageViewService.getUniqueUsername(key, domain);
			dataList.add(Tuple.of(date, value));
			date = DateUtils.addDays(date, 1);
		}
		Tuple<String, Long> p = pageViewService.getMaxUniqueUsername(domain);
		if (p != null)
			max = Tuple.of(DateUtils.parseDate8(p.getKey()), p.getValue());
		return "linechart";
	}

	public String url() {
		if (limit <= 0)
			limit = 20;
		if (date == null && ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopPageViewUrls(day, limit, domain);
		return "list";
	}

	public String fr() {
		if (limit <= 0)
			limit = 20;
		if (date == null && ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopForeignReferers(day, limit, domain);
		return "list";
	}

	public String kw() {
		if (limit <= 0)
			limit = 20;
		if (date == null && ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopKeywords(day, limit, domain);
		return "list";
	}

	public String se() {
		if (limit <= 0)
			limit = 10;
		if (date == null && ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopSearchEngines(day, limit, domain);
		return "piechart";
	}

	public String pr() {
		if (limit <= 0)
			limit = 10;
		if (date == null && ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopProvinces(day, limit, domain);
		return "piechart";
	}

	public String ct() {
		if (limit <= 0)
			limit = 10;
		if (date == null && ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopCities(day, limit, domain);
		return "piechart";
	}

}
