package org.ironrhino.common.util;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.Region;

public class RegionUtils {

	public static Region parse(String value, Region regionTree) {
		Location loc = LocationUtils.parse(value);
		if (loc == null)
			return null;
		Region firstRegion = getChildOrSelfByName(regionTree,
				loc.getFirstArea());
		if (firstRegion == null)
			return null;
		Region secondRegion = getChildOrSelfByName(firstRegion,
				loc.getSecondArea());
		if (secondRegion == null)
			return firstRegion;
		Region thirdRegion = getChildOrSelfByName(secondRegion,
				loc.getThirdArea());
		return thirdRegion != null ? thirdRegion : secondRegion;
	}

	public static Region getChildOrSelfByName(Region parent, String name) {
		if (StringUtils.isBlank(name))
			return null;
		if (parent.getName().startsWith(name))
			return parent;
		for (Region r : parent.getChildren())
			if (r.getName().startsWith(name))
				return r;
		return null;
	}

	public static Region parseByAddress(String address, Region regionTree) {
		// 广东省深圳市福田区
		if (StringUtils.isBlank(address) || regionTree == null)
			return null;
		Region region = null;
		for (Region r : regionTree.getChildren())
			if (address.startsWith(r.getName())) {
				address = address.substring(r.getName().length());
				if (address.length() == 0)
					return r;
				region = r;
				break;
			}
		if (region == null || region.getId() < 1)
			return null;
		if (region.getChildren().size() > 0)
			return parseByAddress(address, region);
		return region;
	}

	public static boolean isAutonomousRegion(Region region) {
		if (region == null || StringUtils.isBlank(region.getName()))
			return false;
		return LocationUtils.autonomousRegions.contains(LocationUtils
				.shortenName(region.getName()));
	}

	public static boolean isSpecialAdministrativeRegion(Region region) {
		if (region == null || StringUtils.isBlank(region.getName()))
			return false;
		return LocationUtils.specialAdministrativeRegions
				.contains(LocationUtils.shortenName(region.getName()));
	}

	public static boolean isMunicipalities(Region region) {
		if (region == null || StringUtils.isBlank(region.getName()))
			return false;
		return LocationUtils.municipalities.contains(LocationUtils
				.shortenName(region.getName()));
	}

	public static Region getCity(Region region) {
		if (isMunicipalities(region) || isSpecialAdministrativeRegion(region))
			return region;
		if (region.getLevel() >= 2) {
			Region top = region.getAncestor(1);
			if (isMunicipalities(top) || isSpecialAdministrativeRegion(top))
				return top;
			else
				return region.getAncestor(2);
		}
		return region;
	}

}
