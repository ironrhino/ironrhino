<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('login')}</title>
<meta name="body_class" content="welcome" />
<@authorize ifAnyGranted="ROLE_BUILTIN_USER">
<meta name="decorator" content="simple" />
<meta http-equiv="refresh" content="0; url=<@url value="/"/>" />
</@authorize>
</head>
<body>
<@authorize ifNotGranted="ROLE_BUILTIN_USER">
<div class="row">
	<div class="span6 offset3">
	<div class="hero-unit">
	<h2 class="caption">${action.getText('login')}</h2>
	<@s.form id="login" action="login" method="post" cssClass="ajax focus form-horizontal well">
		<@s.hidden id="targetUrl" name="targetUrl" />
		<@s.textfield label="%{getText('username')}" name="username" cssClass="required span2"/>
		<@s.password label="%{getText('password')}" name="password" cssClass="required span2 input-pattern submit"/>
		<@s.checkbox label="%{getText('rememberme')}" name="rememberme" cssClass="custom"/>
		<@captcha/>
		<@s.submit value="%{getText('login')}" cssClass="btn-primary">
		<#if getSetting??&&'true'==getSetting('signup.enabled')>
		<@s.param name="after"> <a class="btn" href="${getUrl('/signup')}">${action.getText('signup')}</a></@s.param>
		</#if>
		</@s.submit>
	</@s.form>
	</div>
	</div>
</div>
<#if getSetting??&&'true'==getSetting('signup.enabled')&&'true'==getSetting('oauth.enabled')>
<div class="ajaxpanel" data-url="<@url value="/oauth/connect"/>">
</div>
</#if>
</@authorize>
<@authorize ifAnyGranted="ROLE_BUILTIN_USER">
<div class="modal">
	<div class="modal-body">
		<div class="progress progress-striped active">
			<div class="bar" style="width: 50%;"></div>
		</div>
	</div>
</div>
</@authorize>
</body>
</html></#escape>
