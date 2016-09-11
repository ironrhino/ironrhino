<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('move')}${action.getText(entityName)}</title>
</head>
<body>
	<@s.form action="${actionBaseUrl}/move" method="post" class="ajax form-horizontal">
	<@s.hidden name="id" />
	<div class="control-group treeselect" data-options="{'url':'${actionBaseUrl}/children','cache':false,'type':'treeview','separator':'/','name':'#parent-control','id':'#parent'}">
		<@s.hidden id="parent" name="parent"/>
		<label class="control-label" for="parent-control">${action.getText('parentNode')}</label>
		<div class="controls">
		<span id="parent-control"></span>
		</div>
	</div>
	<@s.submit value="%{getText('save')}" class="btn-primary"/>
	</@s.form>
</body>
</html>