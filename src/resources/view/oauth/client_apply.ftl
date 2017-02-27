<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('apply')}${getText('client')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/apply" method="post" class="ajax reset form-horizontal">
	<@s.textfield name="client.name" class="required checkavailable input-xxlarge"/>
	<@s.textfield name="client.redirectUri" class="required input-xxlarge"/>
	<@s.textarea name="client.description" class=" input-xxlarge"/>
	<@s.submit value=getText('apply') class="btn-primary"/>
</@s.form>
</body>
</html>