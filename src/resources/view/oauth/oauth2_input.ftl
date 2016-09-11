<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title><#if displayForNative && granted>Success code=${authorization.code}<#elseif displayForNative && denied>denied<#else>${action.getText('grant')}</#if></title>
</head>
<body>
	<#if displayForNative && granted>
		<textarea cols="23">${authorization.code}</textarea>
	<#elseif displayForNative && denied>
	<div class="alert alert-warn">
		you denied this request
	</div>
	<#else>
		<@s.form id="grant_form" action="${actionBaseUrl}/grant" method="post" class="form-horizontal">
			<#if client??>
			<legend>
			<div>grant access of ${authorization.scope!} to <strong title="${client.description!}" class="tiped">${client.name}</strong></div>
			</legend>
			</#if>
			<#if id??><@s.hidden name="id" /></#if>
			<#if client_id??><@s.hidden name="client_id" /></#if>
			<#if redirect_uri??><@s.hidden name="redirect_uri" /></#if>
			<#if scope??><@s.hidden name="scope" /></#if>
			<#if response_type??><@s.hidden name="response_type" /></#if>
			<#if state??><@s.hidden name="state" /></#if>
			<#if Parameters.login??>
				<input type="hidden" name="login" value="${Parameters.login!}"/>
				<@s.textfield label="%{getText('username')}" name="username"/>
				<@s.password label="%{getText('password')}" name="password"/>
				<@captcha/>
			<#else>
			<@authorize ifNotGranted="ROLE_BUILTIN_USER">
				<@s.textfield label="%{getText('username')}" name="username"/>
				<@s.password label="%{getText('password')}" name="password"/>
				<@captcha/>
			</@authorize>
			<@authorize ifAnyGranted="ROLE_BUILTIN_USER">
				<div>login as ${authentication('principal.username')},or <a href="<@url value="${ssoServerBase!}/logout?referer=1"/>">${action.getText('logout')}</a></div>
			</@authorize>
			</#if>
			<div class="form-actions">
			<#if Parameters.login??>
				<@s.submit value="%{getText('login')}" theme="simple" class="btn-primary"/>
			<#else>
				<@s.submit value="%{getText('grant')}" theme="simple" class="btn-primary"/> <@s.submit value="%{getText('deny')}" theme="simple" onclick="document.getElementById('grant_form').action='deny';"/>
			</#if>
			</div>
		</@s.form>
	</#if>
</body>
</html>
