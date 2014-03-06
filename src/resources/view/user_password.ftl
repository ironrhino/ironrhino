<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('change')}${action.getText('password')}</title>
</head>
<body>
<#if !(authentication("principal").credentialsNonExpired!true)>
<div class="alert alert-warn">
${action.getText('org.springframework.security.authentication.CredentialsExpiredException')}
</div>
</#if>
<@s.form action="${actionBaseUrl}/password" method="post" cssClass="form-horizontal ajax focus reset">
	<@s.password label="%{getText('currentPassword')}" name="currentPassword" cssClass="required input-pattern" readonly=userProfileReadonly/>
	<@s.password label="%{getText('password')}" name="password" cssClass="required input-pattern" readonly=userProfileReadonly/>
	<@s.password label="%{getText('confirmPassword')}" name="confirmPassword" cssClass="required repeat input-pattern submit" dynamicAttributes={"data-repeatwith":"password"} readonly=userProfileReadonly/>
	<@s.submit value="%{getText('save')}" disabled=userProfileReadonly/>
</@s.form>
</body>
</html></#escape>