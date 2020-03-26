<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('create')}${getText('webAuthnCredential')}</title>
</head>
<body>
<@s.form id="create-webauthn-credential" action=actionBaseUrl+'/create' method="post" class="form-horizontal focus">
	<@s.hidden name="credential"/>
	<@s.textfield name="username" class="required"/>
	<@s.textfield name="expiryTime" class="datetime"/>
	<div class="form-actions"><button type="button" class="btn btn-primary">${getText('create')}</button></div>
</@s.form>
</body>
</html>
