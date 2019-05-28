package org.ironrhino.common.model;

import java.io.Serializable;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coordinate implements Serializable {

	private static final long serialVersionUID = 5828814302557010566L;

	private static final int SCALE = 6;

	public static final double EARTH_RADIUS = 6371393;

	public static final double EARTH_PERIMETER = EARTH_RADIUS * Math.PI * 2;

	@Min(-90)
	@Max(90)
	@Column(precision = 8, scale = SCALE)
	private Double latitude;

	@Min(-180)
	@Max(180)
	@Column(precision = 9, scale = SCALE)
	private Double longitude;

	public Coordinate(String latLng) {
		latLng = latLng.trim();
		if (latLng.toLowerCase(Locale.ROOT).startsWith("point")) {
			// WKT https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry
			latLng = latLng.substring(latLng.indexOf('(') + 1, latLng.indexOf(')')).trim();
			String[] arr = latLng.split("\\s+");
			this.latitude = parseLatOrLong(arr[1]);
			this.longitude = parseLatOrLong(arr[0]);
		} else {
			String[] arr = latLng.split("\\s*,\\s*");
			this.latitude = parseLatOrLong(arr[0]);
			this.longitude = parseLatOrLong(arr[1]);
		}
	}

	public Coordinate(String latitude, String longitude) {
		this.latitude = parseLatOrLong(latitude);
		this.longitude = parseLatOrLong(longitude);
	}

	@Override
	public String toString() {
		if (latitude != null && longitude != null)
			return latitude + "," + longitude;
		else
			return "";
	}

	public String toWktString() {
		return String.format("POINT (%." + SCALE + "f %." + SCALE + "f)", longitude, latitude);
	}

	public static Double parseLatOrLong(String input) {
		try {
			return Double.valueOf(input);
		} catch (Exception e) {
			int i = input.indexOf('°');
			double d = Double.valueOf(input.substring(0, i));
			input = input.substring(i + 1);
			i = input.indexOf('\'');
			if (i > 0) {
				d += Double.valueOf(input.substring(0, i)) / 60;
				input = input.substring(i + 1);
				i = input.indexOf('"');
				if (i > 0) {
					d += Double.valueOf(input.substring(0, i)) / (60 * 60);
					input = input.substring(i + 1).trim().toUpperCase(Locale.ROOT);
					if (input.equals("S") || input.equals("W"))
						d = 0 - d;
				}
			}
			return d;
		}
	}

	public int distanceFrom(Coordinate c2) {
		Coordinate c1 = this;
		double x1 = radian(c1.latitude), y1 = radian(c1.longitude);
		double x2 = radian(c2.latitude), y2 = radian(c2.longitude);
		double radian = Math.acos(Math.cos(x1) * Math.cos(x2) * Math.cos(y2 - y1) + Math.sin(x1) * Math.sin(x2));
		return (int) (EARTH_RADIUS * radian);
	}

	public Coordinate moveVertical(int distance) {
		double delta = (distance / EARTH_PERIMETER) % 1 * 360; // (-360, 360)
		double newLatitude = this.latitude + delta; // (-450, 450)
		double absoluteNewLatitude = Math.abs(newLatitude);
		boolean needInverse = false;
		if (absoluteNewLatitude <= 90) {
		} else if (absoluteNewLatitude <= 270) {
			needInverse = true;
			newLatitude = (newLatitude > 0 ? 180 : -180) - newLatitude;
		} else {
			newLatitude = (newLatitude < 0 ? 360 : -360) + newLatitude;
		}
		return new Coordinate(newLatitude,
				needInverse ? (this.longitude > 0 ? -180 : 180) + this.longitude : this.longitude);
	}

	public Coordinate moveHorizontal(int distance) {
		if (Math.abs(this.latitude) == 90)
			return new Coordinate(this.latitude, this.longitude);
		double delta = (distance / (EARTH_PERIMETER * Math.cos(radian(this.latitude)))) % 1 * 360; // (-360, 360)
		double newLongitude = this.longitude + delta; // (-540, 540)
		if (newLongitude > 180) {
			newLongitude -= 360;
		} else if (newLongitude < -180) {
			newLongitude += 360;
		}
		return new Coordinate(this.latitude, newLongitude);
	}

	public static double radian(double degree) {
		return degree * Math.PI / 180;
	}

}
