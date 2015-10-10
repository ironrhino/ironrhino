package org.ironrhino.core.session.impl;

import java.security.cert.X509Certificate;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.session.SessionCompressor;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.CodecUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class SecurityContextSessionCompressor implements SessionCompressor<SecurityContext> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserDetailsService userDetailsService;

	@Override
	public boolean supportsKey(String key) {
		return HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY.equals(key);
	}

	@Override
	public String compress(SecurityContext sc) {
		if (sc != null) {
			Authentication auth = sc.getAuthentication();
			if (auth != null) {
				if (auth.getCredentials() instanceof X509Certificate)
					return null;
				if (auth.isAuthenticated()) {
					Object principal = auth.getPrincipal();
					if (principal instanceof UserDetails) {
						UserDetails ud = (UserDetails) principal;
						String username = ud.getUsername();
						String password = ud.getPassword();
						return password != null ? new StringBuilder(CodecUtils.md5Hex(ud.getPassword())).append(",")
								.append(username).toString() : username;
					}
				}
			}
		}
		return null;
	}

	@Override
	public SecurityContext uncompress(String string) {
		SecurityContext sc = SecurityContextHolder.getContext();
		if (StringUtils.isNotBlank(string))
			try {
				String[] arr = string.split(",", 2);
				String username, password;
				if (arr.length == 2) {
					username = arr[1];
					password = arr[0];
				} else {
					username = arr[0];
					password = null;
				}
				UserDetails ud = userDetailsService.loadUserByUsername(username);
				if (ud.getPassword() == null && password == null
						|| ud.getPassword() != null && CodecUtils.md5Hex(ud.getPassword()).equals(password))
					sc.setAuthentication(
							new UsernamePasswordAuthenticationToken(ud, ud.getPassword(), ud.getAuthorities()));
				else
					logger.info("invalidate SecurityContext of \"{}\" because password changed", username);
			} catch (UsernameNotFoundException e) {
				logger.warn(e.getMessage());
			}
		return sc;
	}
}
