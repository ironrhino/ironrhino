<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('move')}${getText(entityName)}</title>
</head>
<body>
	<@s.form action="${actionBaseUrl}/move" method="post" class="ajax form-horizontal">
	<@s.hidden name="id" />
	<div class="control-group">
		<label class="control-label" for="parent-control">${getText('parentNode')}</label>
		<div class="controls">
		<input class="treeselect-inline" name="parent" data-url="${actionBaseUrl}/children" data-separator="/">
		</div>
	</div>
	<@s.submit label=getText('save') class="btn-primary"/>
	</@s.form>
</body>
</html>