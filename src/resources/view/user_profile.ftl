<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('profile')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/profile" method="post" cssClass="form-horizontal ajax">
	<@s.textfield label="%{getText('name')}" name="user.name" cssClass="required" readonly=userProfileReadonly/>
	<@s.textfield label="%{getText('email')}" name="user.email" type="email" cssClass="email" readonly=userProfileReadonly/>
	<@s.textfield label="%{getText('phone')}" name="user.phone" readonly=userProfileReadonly/>
	<@s.submit value="%{getText('save')}" disabled=userProfileReadonly />
</@s.form>
</body>
</html></#escape>


