<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('profile')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/profile" method="post" class="form-horizontal ajax">
	<@s.textfield label="%{getText('name')}" name="user.name" class="required" readonly=userProfileReadonly/>
	<@s.textfield label="%{getText('email')}" name="user.email" type="email" class="email" readonly=userProfileReadonly/>
	<@s.textfield label="%{getText('phone')}" name="user.phone" readonly=userProfileReadonly/>
	<@s.submit value="%{getText('save')}" class="btn-primary" disabled=userProfileReadonly />
</@s.form>
</body>
</html>


