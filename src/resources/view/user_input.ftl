<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title><#if user.new>${getText('create')}<#else>${getText('edit')}</#if>${getText('user')}</title>
</head>
<body>
<@s.form id="user_input" action="${actionBaseUrl}/save" method="post" class="ajax form-horizontal sequential_create">
	<#if !user.new>
		<@s.hidden name="user.id" />
		<@s.hidden name="user.version" class="version" />
		<@s.textfield name="user.username" readonly="true"/>
		<@s.password name="password" class="sha"/>
		<@s.password name="confirmPassword" class="sha"/>
	<#else>
		<@s.textfield name="user.username" class="required checkavailable regex conjunct" data\-replacement="controls-user-roles" data\-regex="${statics['org.ironrhino.security.model.User'].USERNAME_REGEX}" data\-checkurl="${actionBaseUrl}/checkavailable"/>
		<@s.password name="password" class="required sha"/>
		<@s.password name="confirmPassword" class="required sha"/>
	</#if>
	<@s.textfield name="user.name" class="required"/>
	<@s.textfield name="user.email" type="email" class="email checkavailable" data\-checkurl="${actionBaseUrl}/checkavailable"/>
	<@s.textfield name="user.phone"/>
	<@s.checkbox name="user.enabled" class="switch" />
	<@s.checkboxlist name="user.roles" list="roles" listKey="value" listValue="label">
	<#if hiddenRoles??&&hiddenRoles?size gt 0>
	<@s.param name="after">
		<#list hiddenRoles as role>
			<input type="hidden" name="user.roles" value="${role}"/>
		</#list>
	</@s.param>
	</#if>
	</@s.checkboxlist>
	<@s.submit label=getText('save') class="btn-primary"/>
</@s.form>
</body>
</html>


