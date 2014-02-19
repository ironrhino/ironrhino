package org.ironrhino.core.chart.openflashchart.elements;

import java.io.Serializable;

public class Legend implements Serializable {

	private static final long serialVersionUID = -4840382172090912482L;
	private String position;
	private boolean visible;
	private String bg_colour;
	private String border_color;
	private boolean shadow;
	private Integer margin;
	private Integer alpha;
	private Integer padding;
	private boolean border;
	private Integer stroke;

	public void setAlpha(Integer alpha) {
		this.alpha = alpha;
	}

	public void setPadding(Integer padding) {
		this.padding = padding;
	}

	public void setStroke(Integer stroke) {
		this.stroke = stroke;
	}

	public Legend() {
		super();
		setVisible(true);
		setPosition("right");
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getBg_colour() {
		return bg_colour;
	}

	public void setBg_colour(String bgColour) {
		bg_colour = bgColour;
	}

	public String getBorder_color() {
		return border_color;
	}

	public void setBorder_color(String borderColor) {
		border_color = borderColor;
	}

	public boolean isShadow() {
		return shadow;
	}

	public void setShadow(boolean shadow) {
		this.shadow = shadow;
	}

	public Integer getMargin() {
		return margin;
	}

	public void setMargin(Integer margin) {
		this.margin = margin;
	}

	public boolean isBorder() {
		return border;
	}

	public void setBorder(boolean border) {
		this.border = border;
	}

	public Integer getAlpha() {
		return alpha;
	}

	public Integer getPadding() {
		return padding;
	}

	public Integer getStroke() {
		return stroke;
	}

}
