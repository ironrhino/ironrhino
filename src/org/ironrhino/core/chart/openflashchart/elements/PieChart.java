package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PieChart extends Element {

	private static final long serialVersionUID = 8853434988212173862L;
	@JsonProperty("start-angle")
	private Integer startAngle;
	private Collection<String> colours;
	private Boolean animate;
	private Integer border;
	private Integer radius;
	@JsonProperty("no-labels")
	private Boolean noLabels;

	public PieChart() {
		super("pie");
	}

	public void setAnimate(Boolean animate) {
		this.animate = animate;
	}

	public Boolean getAnimate() {
		return animate;
	}

	public Integer getStartAngle() {
		return startAngle;
	}

	public void setStartAngle(Integer startAngle) {
		this.startAngle = startAngle;

	}

	public Collection<String> getColours() {
		return colours;
	}

	public void setColours(Collection<String> colours) {
		checkColours();
		this.colours = colours;

	}

	public void setColours(String... colours) {
		checkColours();
		this.colours.clear();
		this.colours.addAll(Arrays.asList(colours));

	}

	public void setColours(List<String> colours) {
		checkColours();
		this.colours.clear();
		this.colours.addAll(colours);

	}

	public Integer getBorder() {
		return border;
	}

	public void setBorder(Integer border) {
		this.border = border;

	}

	public void addValues(Number... values) {
		getValues().addAll(Arrays.asList(values));

	}

	public void addValues(List<Number> values) {
		for (Number number : values) {
			// Ignore null values cause they dont make sense in pie Charts
			if (number != null) {
				getValues().add(number);
			}
		}

	}

	public void addSlice(Number value, String text) {
		addSlices(new Slice(value, text));
	}

	public void addSlices(Slice... s) {
		getValues().addAll(Arrays.asList(s));

	}

	public void addSlices(List<Slice> values) {
		getValues().addAll(values);

	}

	public static class Slice implements Serializable {

		private static final long serialVersionUID = 6961394996186973937L;
		private final String label;
		private String tip;
		private String highlight = "alpha";
		private String text;
		private final Number value;

		public void setHighlight(String highlight) {
			this.highlight = highlight;
		}

		public Slice(Number value, String label) {
			this.label = label;
			this.value = value;
		}

		public void setOnMouseOverAlpha() {
			this.highlight = "alpha";
		}

		public void setOnMouseOverBreakout() {
			this.highlight = null;
		}

		public Number getValue() {
			return value;
		}

		public String getLabel() {
			return label;
		}

		public String getText() {
			return text;
		}

		public String getTip() {
			return tip;
		}

		public void setTip(String tip) {
			this.tip = tip;
		}

		public String getHighlight() {
			return highlight;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	private synchronized void checkColours() {
		if (colours == null)
			colours = new ArrayList<>();
	}

	public Boolean getNoLabels() {
		return noLabels;
	}

	public void setNoLabels(Boolean noLabels) {
		this.noLabels = noLabels;
	}

	public Integer getRadius() {
		return radius;
	}

	public void setRadius(Integer radius) {
		this.radius = radius;
	}
}
