<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('view')}${action.getText('client')}</title>
</head>
<body>
<div class="form-horizontal">
	<div class="control-group">
		<label class="control-label">client_id</label>
		<div class="controls">${client.id}</div>
	</div>
	<div class="control-group">
		<label class="control-label">client_secret</label>
		<div class="controls">${client.secret}</div>
	</div>
	<div class="control-group">
		<label class="control-label">${action.getText('enabled')}</label>
		<div class="controls">${action.getText(client.enabled?string)}</div>
	</div>
	<div class="control-group">
		<label class="control-label">${action.getText('description')}</label>
		<div class="controls">${client.description!}</div>
	</div>
</div>	
</body>
</html>