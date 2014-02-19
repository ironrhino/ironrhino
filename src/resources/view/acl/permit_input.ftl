<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('permit')}</title>
</head>
<body>
<div id="save">
<@s.form id="save_form" action="${actionBaseUrl}/save" method="post" cssClass="ajax">
	<@s.hidden name="role" />
	<#list resources?keys as group>
	<div class="checkboxgroup">
		<div style="padding-bottom:10px;"><#if group!=''><input type="checkbox" style="margin-right:5px;"/>${action.getText(group)}</#if></div>
		<div style="padding:0 0 30px 30px;">
		<#list resources[group] as resource>
			<input type="checkbox" style="margin:0 5px 0 10px;" name="id" value="${resource}"<#if id?? && id?seq_contains(resource)> checked="checked"</#if>/>${action.getText(resource)}
		</#list>
		</div>
	</div>
	</#list>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</div>
</body>
</html></#escape>