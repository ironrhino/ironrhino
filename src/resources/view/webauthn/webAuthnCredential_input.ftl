<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('create')}${getText('webAuthnCredential')}</title>
</head>
<body>
<@s.form theme="simple" id="create-webauthn-credential" action=actionBaseUrl+'/create' method="post" class="form-inline focus">
	<@s.hidden name="credential"/>
	<@s.textfield theme="simple" name="username" class="required"/>
	<button type="button" class="btn btn-primary">${getText('create')}</button>
</div>
</@s.form>
</body>
</html>
