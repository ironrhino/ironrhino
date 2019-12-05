package org.ironrhino.core.spring.security;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
public class VerificationCodeRequirementService {

	@Autowired(required = false)
	private List<VerificationCodeChecker> verificationCodeCheckers = Collections.emptyList();

	@Value("${verification.code.length:6}")
	@Getter
	private int length = 6;

	@Value("${verification.code.resend.interval:60}")
	@Getter
	private int resendInterval = 60;

	public VerificationCodeRequirement getVerificationRequirement(String username) {
		if (verificationCodeCheckers.isEmpty())
			return null;
		if (StringUtils.isBlank(username) || !verificationCodeCheckers.stream().anyMatch(c -> !c.skip(username)))
			return new VerificationCodeRequirement(false, null, null, null, null);
		boolean passwordHidden = verificationCodeCheckers.stream().noneMatch(c -> !c.skipPasswordCheck(username));
		boolean sendingRequired = verificationCodeCheckers.stream().anyMatch(c -> !c.skipSend());
		return new VerificationCodeRequirement(true, length, passwordHidden, sendingRequired,
				sendingRequired ? resendInterval : null);
	}

}
