package org.ironrhino.common.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.model.tuples.Pair;
import org.ironrhino.common.service.PageViewService;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.DateUtils;

@AutoConfig
public class PageViewAction extends BaseAction {

	private static final long serialVersionUID = -6901193289995112304L;

	private Date date;

	private Date from;

	private Date to;

	private List<Pair<Date, Long>> dataList;

	private Pair<Date, Long> max;

	private Long total;

	private int limit;

	private String domain;

	private Set<String> domains;

	private Map<String, Long> dataMap;

	@Autowired
	private transient PageViewService pageViewService;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Set<String> getDomains() {
		return domains;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
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

	public List<Pair<Date, Long>> getDataList() {
		return dataList;
	}

	public Pair<Date, Long> getMax() {
		return max;
	}

	public Long getTotal() {
		return total;
	}

	public Map<String, Long> getDataMap() {
		return dataMap;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public String execute() {
		domains = pageViewService.getDomains();
		return SUCCESS;
	}

	public String pv() {
		if (from != null && to != null && from.before(to)) {
			dataList = new ArrayList<Pair<Date, Long>>();
			Date date = from;
			while (!date.after(to)) {
				String key = DateUtils.formatDate8(date);
				Long value = pageViewService.getPageView(key, domain);
				dataList.add(new Pair<Date, Long>(date, value));
				date = DateUtils.addDays(date, 1);
			}
			Pair<String, Long> p = pageViewService.getMaxPageView(domain);
			if (p != null)
				max = new Pair<Date, Long>(DateUtils.parseDate8(p.getA()),
						p.getB());
			long value = pageViewService.getPageView(null, domain);
			if (value > 0)
				total = value;
			return "linechart";
		} else {
			if (date == null)
				date = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			dataList = new ArrayList<Pair<Date, Long>>();
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
					dataList.add(new Pair<Date, Long>(c.getTime(), value));
				} else {
					dataList.add(new Pair<Date, Long>(cal.getTime(), 0L));
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
		dataList = new ArrayList<Pair<Date, Long>>();
		Date date = from;
		while (!date.after(to)) {
			String key = DateUtils.formatDate8(date);
			Long value = pageViewService.getUniqueIp(key, domain);
			dataList.add(new Pair<Date, Long>(date, value));
			date = DateUtils.addDays(date, 1);
		}
		Pair<String, Long> p = pageViewService.getMaxUniqueIp(domain);
		if (p != null)
			max = new Pair<Date, Long>(DateUtils.parseDate8(p.getA()), p.getB());
		return "linechart";
	}

	public String usid() {
		if (!(from != null && to != null && from.before(to))) {
			to = DateUtils.beginOfDay(new Date());
			from = DateUtils.addDays(to, -30);
		}
		dataList = new ArrayList<Pair<Date, Long>>();
		Date date = from;
		while (!date.after(to)) {
			String key = DateUtils.formatDate8(date);
			Long value = pageViewService.getUniqueSessionId(key, domain);
			dataList.add(new Pair<Date, Long>(date, value));
			date = DateUtils.addDays(date, 1);
		}
		Pair<String, Long> p = pageViewService.getMaxUniqueSessionId(domain);
		if (p != null)
			max = new Pair<Date, Long>(DateUtils.parseDate8(p.getA()), p.getB());
		return "linechart";
	}

	public String uu() {
		if (!(from != null && to != null && from.before(to))) {
			to = DateUtils.beginOfDay(new Date());
			from = DateUtils.addDays(to, -30);
		}
		dataList = new ArrayList<Pair<Date, Long>>();
		Date date = from;
		while (!date.after(to)) {
			String key = DateUtils.formatDate8(date);
			Long value = pageViewService.getUniqueUsername(key, domain);
			dataList.add(new Pair<Date, Long>(date, value));
			date = DateUtils.addDays(date, 1);
		}
		Pair<String, Long> p = pageViewService.getMaxUniqueUsername(domain);
		if (p != null)
			max = new Pair<Date, Long>(DateUtils.parseDate8(p.getA()), p.getB());
		return "linechart";
	}

	public String url() {
		if (limit <= 0)
			limit = 20;
		if (date == null
				&& ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopPageViewUrls(day, limit, domain);
		return "list";
	}

	public String fr() {
		if (limit <= 0)
			limit = 20;
		if (date == null
				&& ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopForeignReferers(day, limit, domain);
		return "list";
	}

	public String kw() {
		if (limit <= 0)
			limit = 20;
		if (date == null
				&& ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopKeywords(day, limit, domain);
		return "list";
	}

	public String se() {
		if (limit <= 0)
			limit = 10;
		if (date == null
				&& ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopSearchEngines(day, limit, domain);
		return "piechart";
	}

	public String pr() {
		if (limit <= 0)
			limit = 10;
		if (date == null
				&& ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopProvinces(day, limit, domain);
		return "piechart";
	}

	public String ct() {
		if (limit <= 0)
			limit = 10;
		if (date == null
				&& ServletActionContext.getRequest().getParameter("date") == null)
			date = new Date();
		String day = date != null ? DateUtils.formatDate8(date) : null;
		dataMap = pageViewService.getTopCities(day, limit, domain);
		return "piechart";
	}

}
