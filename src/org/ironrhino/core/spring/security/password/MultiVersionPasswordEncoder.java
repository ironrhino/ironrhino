package org.ironrhino.core.spring.security.password;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiVersionPasswordEncoder implements PasswordEncoder {

	private SortedMap<Integer, VersionedPasswordEncoder> map;

	@Getter
	@Setter
	private List<VersionedPasswordEncoder> versions;

	@PostConstruct
	public void afterPropertiesSet() {
		if (versions == null || versions.size() == 0) {
			log.error("no PasswordDigester found");
			return;
		}
		map = new TreeMap<>();
		for (VersionedPasswordEncoder pd : versions) {
			map.put(pd.getVersion(), pd);
		}
	}

	@Override
	public String encode(CharSequence rawPassword) {
		if (rawPassword == null)
			return null;
		VersionedPasswordEncoder pd = map.get(map.lastKey());
		return new StringBuilder(pd.encode(rawPassword)).append('#').append(pd.getVersion()).toString();
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (encodedPassword == null)
			encodedPassword = "";
		int i = encodedPassword.indexOf('#');
		int version = 1;
		if (i > 0) {
			version = Integer.valueOf(encodedPassword.substring(i + 1));
			encodedPassword = encodedPassword.substring(0, i);
		}
		VersionedPasswordEncoder pd = map.get(version);
		if (pd == null) {
			pd = map.get(map.lastKey());
			log.warn("no PasswordDigester of version {}, use version {} instead ", version, map.lastKey());
		}
		return pd.matches(rawPassword, encodedPassword);
	}

	public boolean isLastVersion(String encPass) {
		int lastVersion = map.lastKey();
		int version = 1;
		int i = encPass.indexOf('#');
		if (i > 0)
			version = Integer.valueOf(encPass.substring(i + 1));
		return version == lastVersion;
	}

}