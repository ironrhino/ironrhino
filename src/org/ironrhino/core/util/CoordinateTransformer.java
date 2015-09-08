package org.ironrhino.core.util;

import org.ironrhino.common.model.Coordinate;

public class CoordinateTransformer {

	//
	// Krasovsky 1940
	//
	// a = 6378245.0, 1/f = 298.3
	// b = a * (1 - f)
	// ee = (a^2 - b^2) / a^2;
	public static final double a = 6378245.0;
	public static final double ee = 0.00669342162296594323;

	// World Geodetic System (WGS-84) ==> Mars Geodetic System (GCJ-02)
	public static Coordinate transform(Coordinate wgs) {
		double wgLat = wgs.getLatitude();
		double wgLon = wgs.getLongitude();
		double mgLat;
		double mgLon;
		if (outOfChina(wgLat, wgLon)) {
			mgLat = wgLat;
			mgLon = wgLon;
		} else {
			double dLat = transformLat(wgLon - 105.0, wgLat - 35.0);
			double dLon = transformLon(wgLon - 105.0, wgLat - 35.0);
			double radLat = wgLat / 180.0 * Math.PI;
			double magic = Math.sin(radLat);
			magic = 1 - ee * magic * magic;
			double sqrtMagic = Math.sqrt(magic);
			dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
			dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
			mgLat = wgLat + dLat;
			mgLon = wgLon + dLon;
		}
		return new Coordinate(mgLat, mgLon);
	}

	private static boolean outOfChina(double lat, double lon) {
		if (lon < 72.004 || lon > 137.8347)
			return true;
		if (lat < 0.8293 || lat > 55.8271)
			return true;
		return false;
	}

	private static double transformLat(double x, double y) {
		double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
		ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
		return ret;
	}

	private static double transformLon(double x, double y) {
		double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
		ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
		return ret;
	}

}