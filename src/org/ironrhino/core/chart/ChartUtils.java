package org.ironrhino.core.chart;

import java.util.Date;

public class ChartUtils {
	public static int caculateSteps(Number max) {
		double d = max.doubleValue() / 10;
		if (d < 1)
			return 1;
		int i = (int) d;
		if (d > i)
			i++;
		int digit = String.valueOf(i).length();

		int first = i % ((int) Math.pow(10, digit - 1));
		if (first < 5)
			first = 5;
		else
			first = 10;
		return first * ((int) Math.pow(10, digit - 1));
	}

	private static final String[] colors = new String[] { "#ee4400", "#94ee00", "#00eee6", "#ee00c7", "#9800ee",
			"#524141", "#173652", "#36520d", "#d1d900", "#00d96d" };

	public static String caculateColor(int seed) {
		if (seed < colors.length)
			return colors[seed];
		boolean odd = seed % 2 != 0;
		seed = odd ? seed * 2 : 9 - seed;
		StringBuilder sb = new StringBuilder();
		sb.append('#');
		sb.append(seed);
		sb.append(9 - seed);
		sb.append(seed);
		sb.append(9 - seed);
		sb.append(seed);
		sb.append(9 - seed);
		return sb.toString();
	}

	private static final String[] stepColors = new String[] { "#ffeeee", "#ffaaaa", "#ff8888", "#ff5555", "#ff0000" };

	public static String caculateStepColor(Number max, Number value) {
		int steps = stepColors.length;
		double single = max.doubleValue() / steps;
		for (int i = 0; i < steps; i++) {
			if (value.doubleValue() >= single * i && value.doubleValue() < single * (i + 1))
				return stepColors[i];
		}
		return stepColors[stepColors.length - 1];
	}

	public static int caculateXAxisLabelsSteps(Date from, Date to) {
		long range = (to.getTime() - from.getTime()) / 1000;
		return (int) (range / 24);
	}

	public static int caculateXAxisSteps(Date from, Date to) {
		long range = (to.getTime() - from.getTime()) / 1000;
		return (int) (range / 144);
	}

}
