package org.ironrhino.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.ironrhino.common.model.Region;

public class RegionParser {

	public static List<Region> parse() throws IOException {
		return parse(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("resources/data/region.txt"));
	}

	public static List<Region> parse(InputStream inputStream)
			throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				inputStream, "utf-8"));
		List<String> segments = new ArrayList<String>();
		String line;
		boolean end = false;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if ("".equals(line)) {
				if (!end)
					end = true;
				else
					continue;
			} else {
				if (line.charAt(0) == (char) 65279)
					line = line.substring(1);
				sb.append(line);
				sb.append('\n');
			}
			if (end) {
				String s = sb.toString().trim();
				if (!"".equals(s))
					segments.add(s);
				sb.delete(0, sb.length());
				end = false;
			}
		}
		if (sb.length() > 0)
			segments.add(sb.toString().trim());

		br.close();
		List<Region> regions = new ArrayList<Region>();
		for (int i = 0; i < segments.size(); i++) {
			Region child = parseSegment(segments.get(i));
			child.setDisplayOrder(i);
			regions.add(child);
		}
		return regions;
	}

	public static Region parseSegment(String segment) {
		String[] array = segment.split("\n");
		if (array[0].trim().contains(":")) {
			return parseLine(array[0]);
		}
		Region region = new Region(array[0].trim());
		for (int i = 1; i < array.length; i++) {
			Region child = parseLine(array[i]);
			child.setDisplayOrder(i - 1);
			child.setParent(region);
			region.getChildren().add(child);
		}
		return region;
	}

	public static Region parseLine(String line) {
		line = line.trim();
		String city = null;
		String counties = null;
		String[] array = line.split(":");
		city = array[0];
		if (array.length == 2)
			counties = array[1];
		Region region = new Region(city.trim());
		if (counties != null) {
			String[] ss = counties.split("\\s");
			List<String> list = new ArrayList<String>();
			for (int i = 0; i < ss.length; i++) {
				String s = ss[i].trim();
				if (!"".equals(s))
					list.add(s);
			}
			for (int i = 0; i < list.size(); i++) {
				Region child = new Region(list.get(i), i);
				child.setParent(region);
				region.getChildren().add(child);
			}
		}
		return region;
	}

}
