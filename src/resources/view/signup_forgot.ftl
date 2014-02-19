<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('signup.forgot')}</title>
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
	<h2 class="caption">${action.getText('signup.forgot')}</h2>
	<@s.form method="post" action="forgot" cssClass="ajax reset form-horizontal well">
		<@s.textfield label="%{getText('email')}" name="email" type="email" cssClass="required email"/>
		<@captcha/>
		<@s.submit value="%{getText('confirm')}"  cssClass="btn-primary">
		<@s.param name="after"> <a class="btn" href="${getUrl('/signup')}">${action.getText('signup')}</a> <a class="btn" href="${getUrl('/login')}">${action.getText('login')}</a></@s.param>
		</@s.submit>
	</@s.form>
	</div>
	</div>
</div>
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