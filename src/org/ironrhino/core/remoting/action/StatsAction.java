package org.ironrhino.core.remoting.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.common.model.tuples.Pair;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
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

	private List<Pair<Date, Long>> dataList;

	private Pair<Date, Long> max;

	private Long total;

	private String serviceName;

	private String method;

	private StatsType type = StatsType.SERVER_SIDE;

	private Map<String, Set<String>> services;

	@Autowired
	private transient ServiceStats serviceStats;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
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

	@Override
	public String execute() {
		services = serviceStats.getServices();
		return SUCCESS;
	}

	public String count() {
		if (from != null && to != null && from.before(to)) {
			dataList = new ArrayList<>();
			Date date = from;
			while (!date.after(to)) {
				String key = DateUtils.formatDate8(date);
				Long value = serviceStats.getCount(serviceName, method, key, type);
				dataList.add(new Pair<>(date, value));
				date = DateUtils.addDays(date, 1);
			}
			Pair<String, Long> p = serviceStats.getMaxCount(serviceName, method, type);
			if (p != null)
				max = new Pair<>(DateUtils.parseDate8(p.getA()), p.getB());
			long value = serviceStats.getCount(serviceName, method, null, type);
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
					Long value = serviceStats.getCount(serviceName, method, key, type);
					Calendar c = Calendar.getInstance();
					c.setTime(d);
					c.set(Calendar.MINUTE, 30);
					c.set(Calendar.SECOND, 30);
					dataList.add(new Pair<>(c.getTime(), value));
				} else {
					dataList.add(new Pair<>(cal.getTime(), 0L));
				}
			}
			return "barchart";
		}
	}

}
