package org.ironrhino.core.session.impl;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.session.SessionCompressor;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.CodecUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
@Slf4j
public class SecurityContextSessionCompressor implements SessionCompressor<SecurityContext> {

	@Autowired
	private UserDetailsService userDetailsService;

	@Value("${httpSessionManager.checkDirtyPassword:true}")
	private boolean checkDirtyPassword;

	@Override
	public boolean supportsKey(String key) {
		return HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY.equals(key);
	}

	@Override
	public String compress(SecurityContext sc) {
		if (sc == null || sc.getAuthentication() == null)
			return null;
		Authentication auth = sc.getAuthentication();
		if (!auth.isAuthenticated() || auth.getCredentials() instanceof X509Certificate)
			return null;
		StringBuilder sb = new StringBuilder();
		Object principal = auth.getPrincipal();
		if (principal instanceof UserDetails) {
			UserDetails ud = (UserDetails) principal;
			String username = ud.getUsername();
			String password = ud.getPassword();
			if (password != null)
				sb.append(CodecUtils.md5Hex(ud.getPassword())).append(",");
			sb.append(username);
		}
		for (GrantedAuthority ga : auth.getAuthorities()) {
			if (ga instanceof SwitchUserGrantedAuthority) {
				SwitchUserGrantedAuthority suga = (SwitchUserGrantedAuthority) ga;
				String role = suga.getAuthority();
				String source = suga.getSource().getName();
				sb.append("@suga(").append(role).append(",").append(source).append(")");
				break;
			}
		}
		return sb.toString();
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
				String extraAuthorities = null;
				int index = username.indexOf('@');
				if (index > 0) {
					extraAuthorities = username.substring(index + 1);
					username = username.substring(0, index);
				}
				UserDetails ud = userDetailsService.loadUserByUsername(username);
				if (!checkDirtyPassword || (ud.getPassword() == null && password == null
						|| ud.getPassword() != null && CodecUtils.md5Hex(ud.getPassword()).equals(password))) {
					List<GrantedAuthority> authorities = new ArrayList<>(ud.getAuthorities());
					if (extraAuthorities != null) {
						for (String s : extraAuthorities.split("@")) {
							if (s.startsWith("suga(") && s.endsWith(")")) {
								String[] arr2 = s.substring(5, s.length() - 1).split(",", 2);
								try {
									UserDetails source = userDetailsService.loadUserByUsername(arr2[1]);
									SwitchUserGrantedAuthority ga = new SwitchUserGrantedAuthority(arr2[0],
											new UsernamePasswordAuthenticationToken(source, source.getPassword(),
													source.getAuthorities()));
									authorities.add(ga);
								} catch (UsernameNotFoundException e) {
									log.warn(e.getMessage());
								}
							}
						}
					}
					Authentication auth = new UsernamePasswordAuthenticationToken(ud, ud.getPassword(), authorities);
					sc.setAuthentication(auth);
					MDC.put("username", auth.getName());
				} else {
					log.info("invalidate SecurityContext of \"{}\" because password changed", username);
				}
			} catch (UsernameNotFoundException e) {
				log.warn(e.getMessage());
			}
		return sc;
	}
}
