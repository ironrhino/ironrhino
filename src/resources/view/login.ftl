<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('login')}</title>
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
<#if fluidLayout><div class="container"></#if>
<div class="row">
	<div class="span6 offset3">
	<h2 class="caption">${action.getText('login')}</h2>
	<div class="hero-unit">
	<@s.form id="login" action="${actionBaseUrl}" method="post" class="ajax focus form-horizontal well">
		<#if targetUrl?has_content><@s.hidden name="targetUrl" /></#if>
		<@s.textfield label="%{getText('username')}" name="username" class="required span2"/>
		<@s.password label="%{getText('password')}" name="password" class="required span2 input-pattern submit"/>
		<@s.checkbox label="%{getText('rememberme')}" name="rememberme" class="custom"/>
		<@captcha/>
		<@s.submit value="%{getText('login')}" class="btn-primary">
		<#if getSetting??&&'true'==getSetting('signup.enabled')>
		<@resourcePresentConditional value="resources/view/signup.ftl">
		<@s.param name="after"> <a class="btn" href="${getUrl('/signup')}">${action.getText('signup')}</a></@s.param>
		</@resourcePresentConditional>
		</#if>
		</@s.submit>
	</@s.form>
	</div>
	</div>
</div>
<#if fluidLayout></div></#if>
<#if getSetting??&&'true'==getSetting('signup.enabled')&&'true'==getSetting('oauth.enabled')>
<@resourcePresentConditional value="resources/view/oauth/connect.ftl">
<div class="ajaxpanel" data-url="<@url value="/oauth/connect"/>">
</div>
</@resourcePresentConditional>
</#if>

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
</html></#escape>
