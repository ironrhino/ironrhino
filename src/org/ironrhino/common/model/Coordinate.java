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

	private static double EARTH_RADIUS = 6371393;

	@Min(-90)
	@Max(90)
	@Column(precision = 8, scale = 6)
	private Double latitude;

	@Min(-180)
	@Max(180)
	@Column(precision = 9, scale = 6)
	private Double longitude;

	public Coordinate(String latLng) {
		if (latLng == null || latLng.trim().length() == 0) {
			this.latitude = null;
			this.longitude = null;
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
		double latitude = Math.abs((c1.getLatitude() - c2.getLatitude())) * Math.PI / 180;
		double longitude = Math.abs((c1.getLongitude() - c2.getLongitude())) * Math.PI / 180;
		double value = Math.sin(latitude / 2) * Math.sin(latitude / 2) + Math.cos(c1.getLatitude() * Math.PI / 180)
				* Math.cos(c2.getLatitude() * Math.PI / 180) * Math.sin(longitude / 2) * Math.sin(longitude / 2);
		double distance = 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
		return (int) Math.round((EARTH_RADIUS * distance) * 1000) / 1000;
	}

	public Coordinate moveVertical(int distance) {
		double delta = (distance / (2 * Math.PI * EARTH_RADIUS)) * 360;
		double newLatitude = this.latitude + delta;
		while (newLatitude > 90)
			newLatitude = 180 - newLatitude;
		while (newLatitude < -90)
			newLatitude = 180 + newLatitude;
		return new Coordinate(newLatitude, this.longitude);
	}

	public Coordinate moveHorizontal(int distance) {
		if (Math.abs(this.latitude) == 90)
			return new Coordinate(this.latitude, this.longitude);
		double radius = EARTH_RADIUS * Math.sin((90 - Math.abs(this.latitude)) / 180 * Math.PI);
		double delta = (distance / (2 * Math.PI * radius)) * 360;
		double newLongitude = this.longitude + delta;
		while (newLongitude > 180)
			newLongitude = newLongitude - 360;
		while (newLongitude < -180)
			newLongitude = newLongitude + 360;
		return new Coordinate(this.latitude, newLongitude);
	}

}
