package org.ironrhino.core.security.otp;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.security.VerificationCodeChecker;
import org.ironrhino.core.spring.security.WrongVerificationCodeException;
import org.ironrhino.core.throttle.ThrottleService;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "totp.enabled", value = "true")
@Component
public class TotpVerificationCodeChecker implements VerificationCodeChecker {

	@Value("${totp.key:}")
	private String key;

	@Value("${verification.code.length:6}")
	private int digits = 6;

	@Value("${verification.code.resend.interval:60}")
	private int period = 60;

	@Value("${authenticationFailureHandler.delayInterval:5}")
	private int delayInterval;

	@Autowired
	private LockService lockService;

	@Autowired
	private ThrottleService throttleService;

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
	public void verify(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication,
			String verificationCode) {
		String username = userDetails.getUsername();
		String lockName = "totp:" + username;
		String throttleKey = "username:" + username;
		if (lockService.tryLock(lockName)) {
			try {
				if (!of(userDetails).verify(verificationCode)) {
					throttleService.delay(throttleKey, delayInterval, TimeUnit.SECONDS, delayInterval / 2);
					throw new WrongVerificationCodeException("Wrong verification code: " + verificationCode);
				}
			} finally {
				lockService.unlock(lockName);
			}
		} else {
			throttleService.delay(throttleKey, delayInterval, TimeUnit.SECONDS, delayInterval / 2);
		}
	}

}
