package org.ironrhino.common.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.xml.namespace.NamespaceContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.Coordinate;
import org.ironrhino.common.model.Region;
import org.ironrhino.common.util.LocationUtils;
import org.ironrhino.common.util.RegionParser;
import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.aop.PublishAspect;
import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.NumberUtils;
import org.ironrhino.core.util.XmlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.Setter;

@Component
public class RegionSetup {

	private Map<String, String> regionAreacodeMap;

	private Map<String, List<String>> regionCoordinateMap;

	private Map<String, List<Rgn>> rgnMap;

	@Autowired
	private EntityManager<Region> entityManager;

	@Setup
	@Async
	public void setup() throws Exception {
		entityManager.setEntityClass(Region.class);
		if (entityManager.countAll() > 0)
			return;
		regionAreacodeMap = regionAreacodeMap();
		regionCoordinateMap = regionCoordinateMap();
		rgnMap = rgnMap();
		List<Region> regions = RegionParser.parse();
		for (Region region : regions)
			save(region);
		regionCoordinateMap = null;
		regionAreacodeMap = null;
		rgnMap = null;
	}

	private void save(Region region) {
		String shortName = LocationUtils.shortenName(region.getName());
		String areacode = null;
		if (region.getParent() != null)
			areacode = regionAreacodeMap.get(region.getParent().getName() + region.getName());
		if (areacode == null)
			areacode = regionAreacodeMap.get(region.getName());
		region.setAreacode(areacode);
		if (region.getParent() != null) {
			Rgn rgn = findMatched(region);
			if (rgn != null) {
				if (region.getAreacode() == null && StringUtils.isNotBlank(rgn.getI()))
					region.setAreacode(rgn.getI());
				if (StringUtils.isNotBlank(rgn.getZ()))
					region.setPostcode(rgn.getZ());
			}
		}
		if (regionCoordinateMap != null) {
			List<String> coordinateAndParentName = regionCoordinateMap.get(region.getName());
			if (coordinateAndParentName == null)
				coordinateAndParentName = regionCoordinateMap.get(shortName);
			if (coordinateAndParentName != null)
				for (String s : coordinateAndParentName) {
					String[] arr = s.split("\\s");
					String coordinate = arr[0];
					String parentName = arr[1];
					if (region.getParent() != null) {
						if (parentName.length() >= 2 && region.getParent().getName().length() > 2
								&& parentName.contains(region.getParent().getName().substring(0, 2))) {
							String[] arr2 = coordinate.split("\\s*,\\s*");
							Coordinate c = new Coordinate();
							c.setLatitude(NumberUtils.round(Double.valueOf(arr2[1]), 6));
							c.setLongitude(NumberUtils.round(Double.valueOf(arr2[0]), 6));
							region.setCoordinate(c);
							break;
						}
					} else {
						String[] arr2 = coordinate.split("\\s*,\\s*");
						Coordinate c = new Coordinate();
						c.setLatitude(NumberUtils.round(Double.valueOf(arr2[1]), 6));
						c.setLongitude(NumberUtils.round(Double.valueOf(arr2[0]), 6));
						region.setCoordinate(c);
					}
				}

		}
		if (rank1cities.contains(shortName)) {
			region.setRank(1);
		} else if (rank2cities.contains(shortName)) {
			region.setRank(2);
		} else if (!region.isRoot() && !region.isLeaf()) {
			if (region.getDisplayOrder() == 0)
				region.setRank(3);
			else
				region.setRank(4);
		}
		AopContext.setBypass(PublishAspect.class);
		entityManager.save(region);
		List<Region> list = new ArrayList<>();
		for (Region child : region.getChildren())
			list.add(child);
		list.sort(null);
		for (Region child : list)
			save(child);
	}

	private Rgn findMatched(Region region) {
		if (region.getAreacode() != null) {
			List<Rgn> rgns = rgnMap.get(region.getAreacode().substring(0, 2) + "0000");
			return findMatched(region, rgns);
		} else {
			for (List<Rgn> rgns : rgnMap.values()) {
				Rgn rgn = findMatched(region, rgns);
				if (rgn != null)
					return rgn;
			}
		}
		return null;
	}

	private Rgn findMatched(Region region, List<Rgn> rgns) {
		if (rgns == null || rgns.isEmpty())
			return null;
		for (Rgn rgn : rgns) {
			if (matchs(region, rgn))
				return rgn;
			Rgn r = findMatched(region, rgn.getC());
			if (r != null)
				return r;
		}
		return null;
	}

	private static boolean matchs(Region region, Rgn rgn) {
		if (region.getAreacode() != null && region.getAreacode().equals(rgn.getI()))
			return true;
		String name1 = region.getName();
		String name2 = LocationUtils.shortenName(name1);
		String name3 = rgn.getN();
		String name4 = rgn.getA();
		if (name1.equals(name3))
			return true;
		if (name2.equals(name4) && name1.endsWith("区") && (name3.endsWith("县") || name3.endsWith("市")))
			return true;
		return false;
	}

	@Getter
	@Setter
	private static class Rgn {
		private String i;
		private String n;
		private String a;
		private String y;
		private String b;
		private String z;
		private List<Rgn> c;
	}

	private static Map<String, List<Rgn>> rgnMap() {
		Map<String, List<Rgn>> map = new LinkedHashMap<>();
		try (InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("resources/data/region.zip")) {
			// https://github.com/xixilive/chinese_regions_db
			File tempZipFile = File.createTempFile("region", "zip");
			try (OutputStream os = new FileOutputStream(tempZipFile)) {
				IOUtils.copy(is, os);
			}
			try (ZipFile zf = new ZipFile(tempZipFile)) {
				zf.stream().filter(ze -> ze.getName().endsWith(".json")).forEach(ze -> {
					try (InputStream zeis = zf.getInputStream(ze)) {
						List<Rgn> rgns = JsonUtils.getObjectMapper().readValue(zeis, new TypeReference<List<Rgn>>() {
						});
						map.put(ze.getName().substring(0, ze.getName().indexOf('.')), rgns);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
			tempZipFile.delete();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return map;
	}

	private static Map<String, String> regionAreacodeMap() {
		List<String> lines = new ArrayList<>();
		try (InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("resources/data/region_code.txt")) {
			lines = IOUtils.readLines(is, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<String, String> map = new HashMap<>();
		for (String line : lines) {
			if (StringUtils.isBlank(line))
				continue;
			String arr[] = line.split("\\s+", 2);
			String name = arr[1];
			String areacode = arr[0];
			if (map.putIfAbsent(name, areacode) != null) {
				String parentAreacode = areacode.substring(0, 4) + "00";
				String parentName = null;
				for (Map.Entry<String, String> entry : map.entrySet()) {
					if (entry.getValue().equals(parentAreacode)) {
						parentName = entry.getKey();
						break;
					}
				}
				map.put(parentName + name, areacode);
			}
		}
		return map;
	}

	private static Map<String, List<String>> regionCoordinateMap() {
		// http://www.williamlong.info/google/archives/27.html
		NodeList nodeList = null;
		NamespaceContext nsContext = new NamespaceContext() {

			@Override
			public String getNamespaceURI(String prefix) {
				String uri;
				if (prefix.equals("kml")) {
					uri = "http://earth.google.com/kml/2.0";
				} else {
					uri = null;
				}
				return uri;
			}

			@Override
			public String getPrefix(String namespaceURI) {
				String prefix;
				if (namespaceURI.equals("http://earth.google.com/kml/2.0")) {
					prefix = "kml";
				} else {
					prefix = null;
				}
				return prefix;
			}

			@Override
			public Iterator<String> getPrefixes(String namespaceURI) {
				List<String> prefix = new ArrayList<>();
				prefix.add("kml");
				return prefix.iterator();
			}
		};
		nodeList = XmlUtils.evalNodeList("//kml:Placemark",
				new InputStreamReader(
						Thread.currentThread().getContextClassLoader().getResourceAsStream("resources/data/region.kml"),
						StandardCharsets.UTF_8),
				nsContext);
		Map<String, List<String>> map = new HashMap<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element element = (Element) nodeList.item(i);

			String name = null;
			String coordinate = null;
			String parentName = "";
			Element folder = (Element) element.getParentNode();
			int level = 3;
			while (level > 0) {
				parentName = getName(folder) + parentName;
				folder = (Element) folder.getParentNode();
				level--;
			}
			NodeList nl = element.getChildNodes();
			for (int j = 0; j < nl.getLength(); j++) {
				Node node = nl.item(j);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element ele = (Element) node;
					if (ele.getTagName().equals("name")) {
						name = ele.getTextContent();
					} else if (ele.getTagName().equals("Point")) {
						NodeList nl2 = ele.getChildNodes();
						for (int k = 0; k < nl2.getLength(); k++) {
							Node node2 = nl2.item(k);
							if (node2.getNodeType() == Node.ELEMENT_NODE) {
								Element ele2 = (Element) node2;
								if (ele2.getTagName().equals("coordinates")) {
									coordinate = ele2.getTextContent();
								}
							}
						}
					}
				}
			}
			if (name != null) {
				List<String> list = map.get(name);
				if (list == null) {
					list = new ArrayList<>();
					map.put(name, list);
				}
				list.add(coordinate + ' ' + parentName);
			}
		}
		return map;
	}

	private static String getName(Element element) {
		NodeList children = element.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node node = children.item(j);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element ele = (Element) node;
				if (ele.getTagName().equals("name")) {
					return ele.getTextContent();
				}
			}
		}
		return "";
	}

	private static List<String> rank1cities = Arrays.asList("北京,上海,广州,深圳,香港,澳门,台北".split(","));
	private static List<String> rank2cities = Arrays.asList("天津,重庆,杭州,南京,成都,武汉,西安,沈阳,大连,青岛,厦门".split(","));

}
