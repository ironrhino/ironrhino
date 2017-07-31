package org.ironrhino.security.oauth.client.model;

import java.util.Collection;

import lombok.Data;

@Data
public class Profile {

	private String uid;

	private String email;

	private String country;

	private String name;

	private String displayName;

	private String locale;

	private String link;

	private String dob;

	private String gender;

	private String location;

	private String picture;

	private Collection<Contact> contacts;

	@Data
	public static class Contact {

		String email;

		String name;

		String otherEmails[];

	}

}
