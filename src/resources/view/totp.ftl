<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>TOTP</title>
</head>
<body>
<@s.form action=actionBaseUrl method="post" class="form-horizontal ajax view focus">
	<@s.textfield name="username" class="required"/>
	<@s.submit label=getText('view') class="btn-primary"/>
	<#if totpUri?has_content>
	<h1 class="center">${username}</h1>
	<div class="encodeqrcode" style="width: 200px; height: 200px;margin: 50px auto;">${totpUri}</div>
	</#if>
</@s.form>
</body>
</html>