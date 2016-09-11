<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('apply')}${action.getText('client')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/apply" method="post" class="ajax reset form-horizontal">
	<@s.textfield label="%{getText('name')}" name="client.name" class="required checkavailable input-xxlarge"/>
	<@s.textfield label="%{getText('redirectUri')}" name="client.redirectUri" class="required input-xxlarge"/>
	<@s.textarea label="%{getText('description')}" name="client.description" class=" input-xxlarge"/>
	<@s.submit value="%{getText('apply')}" class="btn-primary"/>
</@s.form>
</body>
</html>