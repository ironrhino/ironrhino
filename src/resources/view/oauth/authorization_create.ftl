<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('create')}${action.getText('authorization')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/create" method="post" class="ajax reset form-horizontal">
	<div class="control-group listpick" data-options="{'url':'<@url value="/oauth/client/pick?columns=name"/>'}">
	<@s.hidden name="authorization.client" class="listpick-id"/>
	<label class="control-label" for="client">${action.getText('client')}</label>
	<div class="controls">
	<span class="listpick-name"></span>
	</div>
	</div>
	<div class="control-group listpick" data-options="{'url':'<@url value="/user/pick?columns=username,name&enabled=true"/>','idindex':1}">
	<@s.hidden name="authorization.grantor" class="required listpick-id"/>
	<label class="control-label" for="grantor">${action.getText('grantor')}</label>
	<div class="controls">
	<span class="listpick-name"></span>
	</div>
	</div>
	<@s.textfield label="%{getText('lifetime')}" name="authorization.lifetime" value="0" class="required span1"/>
	<@s.textfield label="%{getText('scope')}" name="authorization.scope" class="span4"/>
	<@s.submit value="%{getText('create')}" class="btn-primary"/>
</@s.form>
</body>
</html>