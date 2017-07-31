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

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class StatsAction extends BaseAction {

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
	@Setter
	private int limit = 10;

	@Getter
	private List<Tuple<Date, Long>> dataList;

	@Getter
	private Tuple<Date, Long> max;

	@Getter
	private Long total;

	@Getter
	@Setter
	private String service;

	@Getter
	@Setter
	private StatsType type = StatsType.SERVER_SIDE;

	@Getter
	private Map<String, Set<String>> services;

	@Getter
	private Map<String, Long> hotspots;

	@Getter
	private List<InvocationWarning> warnings;

	@Getter
	private List<InvocationSample> samples;

	@Autowired(required = false)
	private ServiceStats serviceStats;

	@Override
	public String execute() {
		if (serviceStats == null) {
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
