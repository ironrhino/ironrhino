package org.ironrhino.common.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import javax.xml.namespace.NamespaceContext;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegionSetup {

	@Autowired
	private EntityManager<Region> entityManager;

	private AtomicInteger count = new AtomicInteger();

	@Setup
	@Async
	@Transactional
	public void setup() throws Exception {
		entityManager.setEntityClass(Region.class);
		if (entityManager.countAll() > 0)
			return;
		count.set(0);
		long time = System.currentTimeMillis();
		log.info("Inserting started");
		List<Region> regions = parseRegions();
		int displayOrder = 0;
		for (Region region : regions) {
			region.setDisplayOrder(++displayOrder);
			save(region);
		}
		log.info("Inserted {} in {}ms", count.get(), System.currentTimeMillis() - time);
		_regionCoordinateMap = null;
		_regionAreacodeMap = null;
		_rgnMap = null;
	}

	private void save(Region region) {
		walk(region);
		AopContext.setBypass(PublishAspect.class);
		entityManager.save(region);
		if (count.incrementAndGet() % 5000 == 0)
			log.info("Inserting {} ...", count.get());
		for (Region child : region.getChildren())
			save(child);
	}

	private static List<Region> parseRegions() throws IOException {
		return RegionParser.parse();
	}

	private static void walk(Region region) {
		String shortName = LocationUtils.shortenName(region.getName());
		if (StringUtils.isBlank(region.getAreacode()) || "0".equals(region.getAreacode())) {
			String areacode = null;
			if (region.getParent() != null)
				areacode = getRegionAreacodeMap().get(region.getParent().getName() + region.getName());
			if (areacode == null)
				areacode = getRegionAreacodeMap().get(region.getName());
			region.setAreacode(areacode);
		}
		if (region.getParent() != null) {
			if (region.getFullname().matches("^(台湾|香港|澳门).*$")) {
				region.setAreacode(null);
				region.setPostcode(null);
			} else {
				Rgn rgn = findMatched(region);
				if (rgn != null) {
					if (region.getAreacode() == null && StringUtils.isNotBlank(rgn.getI()))
						region.setAreacode(rgn.getI());
					if (StringUtils.isNotBlank(rgn.getZ()))
						region.setPostcode(rgn.getZ());
				}
			}
		}
		if (getRegionCoordinateMap() != null) {
			List<String> coordinateAndParentName = getRegionCoordinateMap().get(region.getName());
			if (coordinateAndParentName == null)
				coordinateAndParentName = getRegionCoordinateMap().get(shortName);
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

		List<Region> list = new ArrayList<>();
		int displayOrder = 0;
		for (Region child : region.getChildren()) {
			child.setParent(region);
			child.setDisplayOrder(++displayOrder);
			list.add(child);
		}
		list.sort(null);
		region.setChildren(list);
	}

	private static Map<String, String> _regionAreacodeMap;

	private static Map<String, List<String>> _regionCoordinateMap;

	private static Map<String, List<Rgn>> _rgnMap;

	private static Map<String, String> getRegionAreacodeMap() {
		if (_regionAreacodeMap == null)
			_regionAreacodeMap = regionAreacodeMap();
		return _regionAreacodeMap;
	}

	private static Map<String, List<String>> getRegionCoordinateMap() {
		if (_regionCoordinateMap == null)
			_regionCoordinateMap = regionCoordinateMap();
		return _regionCoordinateMap;
	}

	private static Map<String, List<Rgn>> getRgnMap() {
		if (_rgnMap == null)
			_rgnMap = rgnMap();
		return _rgnMap;
	}

	private static Rgn findMatched(Region region) {
		if (region.getAreacode() != null) {
			List<Rgn> rgns = getRgnMap().get(region.getAreacode().substring(0, 2) + "0000");
			return findMatched(region, rgns);
		} else {
			for (List<Rgn> rgns : getRgnMap().values()) {
				Rgn rgn = findMatched(region, rgns);
				if (rgn != null)
					return rgn;
			}
		}
		return null;
	}

	private static Rgn findMatched(Region region, List<Rgn> rgns) {
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
				StreamUtils.copy(is, os);
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
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("resources/data/region_code.txt"),
				StandardCharsets.UTF_8))) {
			lines = br.lines().collect(Collectors.toList());
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
		map.put("上海市宝山区", "310113");
		map.put("双鸭山市宝山区", "230506");
		map.put("重庆市江北区", "500105");
		map.put("宁波市江北区", "330205");
		map.put("邢台市桥东区", "130502");
		map.put("张家口市桥东区", "130702");
		map.put("石家庄市桥西区", "130104");
		map.put("邢台市桥西区", "130503");
		map.put("张家口市桥西区", "130703");
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
				map.computeIfAbsent(name, key -> new ArrayList<>()).add(coordinate + ' ' + parentName);
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

}
