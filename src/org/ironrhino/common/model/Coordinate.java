package org.ironrhino.common.model;

import java.io.Serializable;
import java.util.Locale;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coordinate implements Serializable {

	private static final long serialVersionUID = 5828814302557010566L;

	private static double EARTH_RADIUS = 6371000;

	private Double latitude;

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
		Double latitude = (c1.getLatitude() - c2.getLatitude()) * Math.PI / 180;
		Double longitude = (c1.getLongitude() - c2.getLongitude()) * Math.PI / 180;
		Double aDouble = Math.sin(latitude / 2) * Math.sin(latitude / 2) + Math.cos(c1.getLatitude() * Math.PI / 180)
				* Math.cos(c2.getLatitude() * Math.PI / 180) * Math.sin(longitude / 2) * Math.sin(longitude / 2);
		Double distance = 2 * Math.atan2(Math.sqrt(aDouble), Math.sqrt(1 - aDouble));
		return (int) Math.round((EARTH_RADIUS * distance) * 1000) / 1000;
	}

}
