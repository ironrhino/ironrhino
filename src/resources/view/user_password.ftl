<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('change')}${action.getText('password')}</title>
</head>
<body>
<#if !(authentication("principal").credentialsNonExpired!true)>
<div class="action-error alert alert-error">
<a class="close" data-dismiss="alert">&times;</a>
${action.getText('org.springframework.security.authentication.CredentialsExpiredException')}
</div>
</#if>
<@s.form action="${actionBaseUrl}/password" method="post" class="form-horizontal ajax focus reset">
	<@s.password label="%{getText('currentPassword')}" name="currentPassword" class="required input-pattern" readonly=userProfileReadonly/>
	<@s.password label="%{getText('password')}" name="password" class="required input-pattern" readonly=userProfileReadonly/>
	<@s.password label="%{getText('confirmPassword')}" name="confirmPassword" class="required repeat input-pattern submit" data\-repeatwith="password" readonly=userProfileReadonly/>
	<@s.submit value="%{getText('save')}" class="btn-primary" disabled=userProfileReadonly/>
</@s.form>
</body>
</html>