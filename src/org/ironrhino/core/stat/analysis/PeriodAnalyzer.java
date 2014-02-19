package org.ironrhino.core.stat.analysis;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.KeyValuePair;
import org.ironrhino.core.stat.Value;

public class PeriodAnalyzer extends AbstractAnalyzer<List<Value>> {

	private List<Value> result = new ArrayList<Value>(24);

	private Map<String, List<Value>> perHostResult = new TreeMap<String, List<Value>>();

	private Key key;

	private boolean cumulative;

	private boolean perHostEnabled;

	Calendar calendar = Calendar.getInstance();

	public PeriodAnalyzer(Key key, boolean localhost)
			throws FileNotFoundException {
		super(localhost);
		this.key = key;
	}

	public PeriodAnalyzer(Key key, Date date, boolean localhost)
			throws FileNotFoundException {
		super(date, localhost);
		this.key = key;
	}

	public PeriodAnalyzer(Key key, Date[] dates, boolean localhost)
			throws FileNotFoundException {
		super(dates, localhost);
		this.key = key;
	}

	public PeriodAnalyzer(Key key, Date start, Date end, boolean localhost)
			throws FileNotFoundException {
		super(start, end, localhost);
		this.key = key;
	}

	public PeriodAnalyzer(Key key, Iterator<? extends KeyValuePair> iterator) {
		super(iterator);
		this.key = key;
	}

	public boolean isPerHostEnabled() {
		return perHostEnabled;
	}

	public void setPerHostEnabled(boolean perHostEnabled) {
		this.perHostEnabled = perHostEnabled;
	}

	public boolean isCumulative() {
		return cumulative;
	}

	public void setCumulative(boolean cumulative) {
		this.cumulative = cumulative;
	}

	@Override
	public List<Value> getResult() {
		return result;
	}

	public Map<String, List<Value>> getPerHostResult() {
		return perHostResult;
	}

	@Override
	protected void preAnalyze() {
		if (key == null)
			throw new IllegalArgumentException("key is null");
		for (int i = 0; i < 24; i++) {
			result.add(new Value());
		}
	}

	@Override
	protected void process(KeyValuePair pair) {
		if (!key.isAncestorOf(pair.getKey()))
			return;
		calendar.setTime(pair.getDate());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		result.get(hour).cumulate(pair.getValue());
		if (cumulative)
			for (int i = hour + 1; i < 24; i++)
				result.get(i).cumulate(pair.getValue());
		if (perHostEnabled) {
			List<Value> list = perHostResult.get(pair.getHost());
			if (list == null) {
				list = new ArrayList<Value>(24);
				for (int i = 0; i < 24; i++)
					list.add(new Value());
				perHostResult.put(pair.getHost(), list);
			}
			list.get(hour).cumulate(pair.getValue());
			if (cumulative)
				for (int i = hour + 1; i < 24; i++)
					list.get(i).cumulate(pair.getValue());
		}
	}

}