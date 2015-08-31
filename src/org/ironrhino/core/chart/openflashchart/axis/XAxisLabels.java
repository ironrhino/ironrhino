package org.ironrhino.core.chart.openflashchart.axis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XAxisLabels extends Label {

	private static final long serialVersionUID = -6134375829177947590L;
	private Integer steps;
	@JsonProperty("visible-steps")
	private Integer visible_steps;
	private List<Object> labels;

	public XAxisLabels() {
	}

	public XAxisLabels(String... labels) {
		addLabels(labels);
	}

	public XAxisLabels(List<String> labels) {
		addLabels(labels.toArray(new String[0]));
	}

	public void setLabels(List<Object> labels) {
		this.labels = labels;
	}

	public List<Object> getLabels() {
		return labels;
	}

	public void addLabels(String... labels) {
		checkLabels();
		this.labels.addAll(Arrays.asList(labels));

	}

	public void addLabels(Label... labels) {
		checkLabels();
		this.labels.addAll(Arrays.asList(labels));

	}

	public void addLabels(List<Label> labels) {
		checkLabels();
		this.labels.addAll(labels);

	}

	public void setSteps(Integer steps) {
		this.steps = steps;

	}

	public Integer getSteps() {
		return steps;
	}

	public Integer getVisible_steps() {
		return visible_steps;
	}

	public void setVisible_steps(Integer visibleSteps) {
		visible_steps = visibleSteps;
	}

	private synchronized void checkLabels() {
		if (labels == null)
			labels = new ArrayList<>();
	}
}
