package org.ironrhino.core.remoting.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.remoting.InvocationSample;
import org.ironrhino.core.remoting.InvocationWarning;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.remoting.StatsType;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class StatsAction extends BaseAction {

	private static final long serialVersionUID = -6901193289995112304L;

	private Date date;

	private Date from;

	private Date to;

	private int limit = 10;

	private List<Tuple<Date, Long>> dataList;

	private Tuple<Date, Long> max;

	private Long total;

	private String service;

	private StatsType type = StatsType.SERVER_SIDE;

	private Map<String, Set<String>> services;

	private Map<String, Long> hotspots;

	private List<InvocationWarning> warnings;

	private List<InvocationSample> samples;

	@Autowired(required = false)
	private transient ServiceStats serviceStats;

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public StatsType getType() {
		return type;
	}

	public void setType(StatsType type) {
		this.type = type;
	}

	public Map<String, Set<String>> getServices() {
		return services;
	}

	public Map<String, Long> getHotspots() {
		return hotspots;
	}

	public List<InvocationWarning> getWarnings() {
		return warnings;
	}

	public List<InvocationSample> getSamples() {
		return samples;
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

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public List<Tuple<Date, Long>> getDataList() {
		return dataList;
	}

	public Tuple<Date, Long> getMax() {
		return max;
	}

	public Long getTotal() {
		return total;
	}

	@Override
	public String execute() {
		if(serviceStats == null){
			addActionError("Require bean serviceStats");
			return ERROR;
		}
		services = serviceStats.getServices();
		return SUCCESS;
	}

	public String hotspots() {
		hotspots = serviceStats.findHotspots(limit);
		return "hotspots";
	}

	public String warnings() {
		warnings = serviceStats.getWarnings();
		return "warnings";
	}

	public String samples() {
		if (StringUtils.isNotBlank(service))
			samples = serviceStats.getSamples(service, type);
		return "samples";
	}

	public String count() {
		if (from != null && to != null && from.before(to)) {
			dataList = new ArrayList<>();
			Date date = from;
			while (!date.after(to)) {
				String key = DateUtils.formatDate8(date);
				Long value = serviceStats.getCount(service, key, type);
				dataList.add(new Tuple<>(date, value));
				date = DateUtils.addDays(date, 1);
			}
			Tuple<String, Long> p = serviceStats.getMaxCount(service, type);
			if (p != null)
				max = new Tuple<>(DateUtils.parseDate8(p.getKey()), p.getValue());
			long value = serviceStats.getCount(service, null, type);
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
					Long value = serviceStats.getCount(service, key, type);
					Calendar c = Calendar.getInstance();
					c.setTime(d);
					c.set(Calendar.MINUTE, 30);
					c.set(Calendar.SECOND, 30);
					dataList.add(new Tuple<>(c.getTime(), value));
				} else {
					dataList.add(new Tuple<>(cal.getTime(), 0L));
				}
			}
			return "barchart";
		}
	}

}
