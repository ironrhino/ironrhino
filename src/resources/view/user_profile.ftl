<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('profile')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/profile" method="post" class="form-horizontal ajax">
	<@s.textfield name="user.name" class="required" readonly=userProfileReadonly/>
	<@s.textfield name="user.email" type="email" class="email" readonly=userProfileReadonly/>
	<@s.textfield name="user.phone" readonly=userProfileReadonly/>
	<@s.submit label=getText('save') class="btn-primary" disabled=userProfileReadonly>
	<@s.param name="after">
	<button type="button" class="btn dialog-close">${getText('cancel')}</button>
	</@s.param>
	</@s.submit>
</@s.form>
</body>
</html>


