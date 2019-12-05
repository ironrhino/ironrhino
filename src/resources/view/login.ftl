<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('login')}</title>
<meta name="body_class" content="welcome" />
<#assign anonymous = false>
<@authorize ifAllGranted="ROLE_BUILTIN_ANONYMOUS">
<#assign anonymous = true>
</@authorize>
<#if !anonymous>
<meta name="decorator" content="simple" />
<meta http-equiv="refresh" content="0; url=<@url value=targetUrl!(properties['login.defaultTargetUrl']!'/')/>" />
</#if>
</head>
<body>
<#if anonymous>
<div class="row">
	<div class="span6 offset3">
	<h2 class="caption">${getText('login')}</h2>
	<div class="hero-unit">
	<#assign verificationCodeRequirement = beans['verificationCodeRequirementService'].getVerificationRequirement(username)!>
	<@s.form id="login" action=request.requestURI+request.queryString???then('?'+request.queryString,'') method="post" class="ajax focus form-horizontal well">
		<#assign dynamicAttributes={}>
		<#if (verificationCodeRequirement.required)!false>
		<#assign dynamicAttributes+={'data-replacement':'verification'}>
		</#if>
		<@s.textfield name="username" class="required span2${(verificationCodeRequirement?has_content)?then(' conjunct','')}" dynamicAttributes=dynamicAttributes/>
		<#if verificationCodeRequirement?has_content><div id="verification"></#if>
		<#if !(((verificationCodeRequirement.required)!false) && verificationCodeRequirement.passwordHidden)>
		<@s.password name="password" class="required span2 input-pattern submit sha"/>
		</#if>
		<#if (verificationCodeRequirement.required)!false>
		<@s.textfield name="verificationCode" class="required input-small" maxlength="${verificationCodeRequirement.length}">
			<#if verificationCodeRequirement.sendingRequired>
			<@s.param name="after"> <button type="button" class="btn input-mini sendVerificationCode" data-interval="${verificationCodeRequirement.resendInterval}">${getText('send')}</button></@s.param>
			</#if>
		</@s.textfield>
		<#else>
		<#if (properties['rememberMe.disabled']!)!='true'><@s.checkbox name="rememberme" class="switch span2"/></#if>
		<@captcha/>
		</#if>
		<#if verificationCodeRequirement?has_content></div></#if>
		<@s.submit label=getText('login') class="btn-primary"/>
	</@s.form>
	</div>
	</div>
</div>
<#else>
<div class="modal">
	<div class="modal-body">
		<div class="progress progress-striped active">
			<div class="bar" style="width: 50%;"></div>
		</div>
	</div>
</div>
</#if>
</body>
</html>