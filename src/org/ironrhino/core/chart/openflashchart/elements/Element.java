package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Element implements Serializable {

	public static final String ON_CLICK_TOGGLE_VISIBILITY = "toggle-visibility";

	private static final long serialVersionUID = 3975314200083173622L;
	private final String type;
	private Float alpha;
	private String text;
	@JsonProperty("font-size")
	private Integer fontSize;
	private String tip;
	@JsonProperty("gradient-fill")
	private Boolean gradientFill;
	@JsonProperty("key-on-click")
	private String key_on_click;
	private List<Object> values = new ArrayList<>();

	protected Element(String type) {
		this.type = type;
	}

	public void setValues(List<Object> values) {
		this.values = values;
	}

	public String getType() {
		return type;
	}

	public Float getAlpha() {
		return alpha;
	}

	public void setAlpha(Float alpha) {
		this.alpha = alpha;

	}

	public String getText() {
		return text;
	}

	/**
	 * The Text is used to represent the Element in the legend. If text is null
	 * the element will not appear in the legend
	 */
	public void setText(String text) {
		this.text = text;

	}

	public Integer getFontSize() {
		return fontSize;
	}

	public void setFontSize(Integer fontSize) {
		this.fontSize = fontSize;

	}

	public List<Object> getValues() {
		return values;
	}

	public String getTip() {
		return tip;
	}

	public void setTip(String tip) {
		this.tip = tip;
	}

	public Boolean getGradientFill() {
		return gradientFill;
	}

	public void setGradientFill(Boolean gradientFill) {
		this.gradientFill = gradientFill;
	}

	public String getKey_on_click() {
		return key_on_click;
	}

	/**
	 * Set the Key on Click Funktion. e.g. "toggle-visibility". For a Link just
	 * pass the URL toggle-visibility will enable you to click on your legend an
	 * switch on and off individual elements
	 */
	public void setKey_on_click(String key_on_click) {
		this.key_on_click = key_on_click;
	}

	/**
	 * Shortcut for setKey_on_click(ON_CLICK_TOGGLE_VISIBILITY)
	 */
	public void setToggleVisibility() {
		this.key_on_click = ON_CLICK_TOGGLE_VISIBILITY;
	}
}
