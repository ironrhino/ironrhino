<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('change')}${getText('password')}</title>
</head>
<body>
<#if !(authentication("principal").credentialsNonExpired!true)>
<div class="action-error alert alert-error">
<a class="close" data-dismiss="alert"></a>
${getText('org.springframework.security.authentication.CredentialsExpiredException')}
</div>
</#if>
<#assign verificationCodeEnabled = (properties['verification.code.enabled']!)=='true'>
<#assign verificationCodeRequired = false>
<#if verificationCodeEnabled>
	<#assign verificationCodeRequired = verificationManager.isVerificationRequired(authentication('name'))>
</#if>
<#assign totpEnabled = (properties['totp.enabled']!)=='true'>
<@s.form action=request.requestURI+request.queryString???then('?'+request.queryString,'') method="post" class="form-horizontal ajax focus reset">
	<#if userCurrentPasswordNeeded!true>
	<#assign userProfileReadonly=userProfileReadonly!false>
	<@s.password name="currentPassword" class="required input-pattern sha" readonly=userProfileReadonly/>
	</#if>
	<@s.password name="password" class="required input-pattern" readonly=userProfileReadonly/>
	<@s.password name="confirmPassword" class="required repeat input-pattern submit" data\-repeatwith="password" readonly=userProfileReadonly/>
	<#if verificationCodeRequired || totpEnabled>
	<@s.textfield name="verificationCode" class="required input-small" maxlength="${properties['verification.code.length']!'6'}">
		<#if verificationCodeRequired>
		<@s.param name="after"> <button type="button" class="btn input-mini sendVerificationCode" data-url="<@url value='/login/sendVerificationCode'/>" data-interval="${properties['verification.code.resend.interval']!'60'}">${getText('send')}</button></@s.param>
		</#if>
	</@s.textfield>
	</#if>
	<@s.submit label=getText('save') class="btn-primary" disabled=userProfileReadonly>
	<@s.param name="after">
	<button type="button" class="btn dialog-close">${getText('cancel')}</button>
	</@s.param>
	</@s.submit>
</@s.form>
</body>
</html>