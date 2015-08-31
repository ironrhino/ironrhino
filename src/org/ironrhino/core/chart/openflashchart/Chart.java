/*
This file is part of JOFC2.

JOFC2 is free software: you can redistribute it and/or modify
it under the terms of the Lesser GNU General Public License as
published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

JOFC2 is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

See <http://www.gnu.org/licenses/lgpl-3.0.txt>.
 */
package org.ironrhino.core.chart.openflashchart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.ironrhino.core.chart.openflashchart.axis.XAxis;
import org.ironrhino.core.chart.openflashchart.axis.YAxis;
import org.ironrhino.core.chart.openflashchart.elements.Element;
import org.ironrhino.core.chart.openflashchart.elements.Legend;
import org.ironrhino.core.chart.openflashchart.elements.Tooltip;
import org.ironrhino.core.util.JsonUtils;

public class Chart implements Serializable {

	private static final long serialVersionUID = -1868082240169089976L;
	private Text title;
	private XAxis x_axis;
	private YAxis y_axis;
	private YAxis y_axis_right;
	private Text y_legend;
	private Text x_legend;
	private String bg_colour = "#ffffff";

	private int num_decimals = 2;
	private Collection<Element> elements = new ArrayList<>();
	private Legend legend;
	private Tooltip tooltip;

	public Chart() {
	}

	public Chart(String titleText) {
		this(titleText, null);
	}

	public Chart(String titleText, String style) {
		this.setTitle(new Text(titleText, style));
	}

	public Text getTitle() {
		return title;
	}

	public void setTitle(Text title) {
		this.title = title;
	}

	public XAxis getX_axis() {
		return x_axis;
	}

	public void setX_axis(XAxis xAxis) {
		x_axis = xAxis;
	}

	public YAxis getY_axis() {
		return y_axis;
	}

	public void setY_axis(YAxis yAxis) {
		y_axis = yAxis;
	}

	public YAxis getY_axis_right() {
		return y_axis_right;
	}

	public void setY_axis_right(YAxis yAxisRight) {
		y_axis_right = yAxisRight;
	}

	public Text getY_legend() {
		return y_legend;
	}

	public void setY_legend(Text yLegend) {
		y_legend = yLegend;
	}

	public Text getX_legend() {
		return x_legend;
	}

	public void setX_legend(Text xLegend) {
		x_legend = xLegend;
	}

	public String getBg_colour() {
		return bg_colour;
	}

	public void setBg_colour(String bgColour) {
		bg_colour = bgColour;
	}

	public int getNum_decimals() {
		return num_decimals;
	}

	public void setNum_decimals(int numDecimals) {
		num_decimals = numDecimals;
	}

	public Legend getLegend() {
		return legend;
	}

	public void setLegend(Legend legend) {
		this.legend = legend;
	}

	public Tooltip getTooltip() {
		return tooltip;
	}

	public void setTooltip(Tooltip tooltip) {
		this.tooltip = tooltip;
	}

	public Collection<Element> getElements() {
		return elements;
	}

	public void setElements(Collection<Element> elements) {
		this.elements.clear();
		this.elements.addAll(elements);

	}

	public void addElements(Element... e) {
		elements.addAll(Arrays.asList(e));

	}

	public void addElements(Collection<Element> coll) {
		elements.addAll(coll);

	}

	public boolean removeElement(Element e) {
		return elements.remove(e);
	}

	public Element getElementByText(String text) {
		for (Element e : getElements()) {
			if (text.equals(e.getText()))
				return e;
		}
		return null;
	}

	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}

}
