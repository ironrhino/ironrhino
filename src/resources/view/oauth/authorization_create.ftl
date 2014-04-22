<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('create')}${action.getText('authorization')}</title>
</head>
<body>
<@s.form action="create" method="post" cssClass="ajax reset form-horizontal">
	<div class="control-group listpick" data-options="{'url':'<@url value="/oauth/client/pick?columns=name"/>'}">
	<@s.hidden id="clientId" name="authorization.client.id" cssClass="listpick-id"/>
	<label class="control-label" for="client">${action.getText('client')}</label>
	<div class="controls">
	<span class="listpick-name"><#if authorization.client??>${authorization.client.name}</#if></span>
	</div>
	</div>
	<div class="control-group listpick" data-options="{'url':'<@url value="/user/pick?columns=username,name&enabled=true"/>'}">
	<@s.hidden id="grantorId" name="authorization.grantor.id" cssClass="required listpick-id"/>
	<label class="control-label" for="grantor">${action.getText('grantor')}</label>
	<div class="controls">
	<span class="listpick-name"><#if authorization.grantor??>${authorization.grantor.username}</#if></span>
	</div>
	</div>
	<@s.textfield label="%{getText('lifetime')}" name="authorization.lifetime" cssClass="required span1"/>
	<@s.textfield label="%{getText('scope')}" name="authorization.scope" cssClass="span4"/>
	<@s.submit value="%{getText('create')}" />
</@s.form>
</body>
</html></#escape>