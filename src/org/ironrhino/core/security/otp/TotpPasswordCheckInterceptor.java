package org.ironrhino.core.security.otp;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.security.verfication.WrongVerificationCodeException;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.security.DefaultWebAuthenticationDetails;
import org.ironrhino.core.spring.security.password.PasswordCheckInterceptor;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "totp.enabled", value = "true")
@Component
public class TotpPasswordCheckInterceptor implements PasswordCheckInterceptor {

	@Value("${totp.key:}")
	private String key;

	@Value("${verification.code.length:6}")
	private int digits = 6;

	@Value("${verification.code.resend.interval:60}")
	private int period = 60;

	@PostConstruct
	private void init() {
		if (StringUtils.isBlank(key))
			key = AppInfo.getAppName();
	}

	public Totp of(UserDetails userDetails) {
		String identity = userDetails.getUsername() + '@' + key;
		String secret = Totp.BASE32.encodeToString(CodecUtils.sha256(identity));
		return new Totp(secret, digits, period);
	}

	@Override
	public void prePasswordCheck(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
		DefaultWebAuthenticationDetails wad = (DefaultWebAuthenticationDetails) authentication.getDetails();
		if (!shouldSkip(authentication)) {
			String verificationCode = wad.getVerificationCode();
			boolean verified = of(userDetails).verify(verificationCode);
			if (!verified)
				throw new WrongVerificationCodeException("Wrong verification code: " + verificationCode);
		}
	}

	protected boolean shouldSkip(UsernamePasswordAuthenticationToken authentication) {
		DefaultWebAuthenticationDetails wad = (DefaultWebAuthenticationDetails) authentication.getDetails();
		String remoteAddress = wad.getRemoteAddress();
		return "admin".equals(authentication.getName())
				&& ("127.0.0.1".equals(remoteAddress) || "0:0:0:0:0:0:0:1".equals(remoteAddress));
	}

}
