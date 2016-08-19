<#assign view=Parameters.view!/>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if setting.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('setting')}</title>
</head>
<body>
<@s.form id="setting_input" action="${actionBaseUrl}/save" method="post" class="ajax form-horizontal${view?has_content?then('',' importable')}">
	<#if !setting.new>
		<@s.hidden name="setting.id"/>
	</#if>
	<@s.hidden name="setting.version" class="version"/>
	<#if view=='embedded'>
		<@s.hidden name="setting.key"/>
		<@s.hidden name="setting.description"/>
	<#elseif view=='brief'>
		<@s.hidden name="setting.key"/>
		<@s.textarea label="%{getText('description')}" name="setting.description" tabindex="-1" readonly=true class="input-xxlarge"/>
	<#else>
		<@s.textfield label="%{getText('key')}" name="setting.key" class="required checkavailable input-xxlarge"/>
	</#if>
	<#if view=='embedded'>
	<@s.textarea label="%{getText('value')}" theme="simple" name="setting.value" style="width:95%;" class="${Parameters.class!Parameters.cssClass!}" maxlength="4000"/>
	<#else>
	<@s.textarea label="%{getText('value')}" name="setting.value" class="input-xxlarge" maxlength="4000"/>
	</#if>
	<#if !(view=='embedded'||view=='brief')>
		<@s.textarea label="%{getText('description')}" name="setting.description" class="input-xxlarge" maxlength="4000"/>
	</#if>
	<@s.submit value="%{getText('save')}" class="btn-primary"/>
</@s.form>
</body>
</html></#escape>


