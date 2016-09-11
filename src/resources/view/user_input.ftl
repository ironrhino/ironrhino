<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title><#if user.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('user')}</title>
</head>
<body>
<@s.form id="user_input" action="${actionBaseUrl}/save" method="post" class="ajax form-horizontal sequential_create">
	<#if !user.new>
		<@s.hidden name="user.id" />
		<@s.textfield label="%{getText('username')}" name="user.username" readonly="true"/>
		<@s.password label="%{getText('password')}" name="password"/>
		<@s.password label="%{getText('confirmPassword')}" name="confirmPassword"/>
	<#else>
		<@s.textfield label="%{getText('username')}" name="user.username" class="required checkavailable regex conjunct" data\-replacement="controls-user-roles" data\-regex="${statics['org.ironrhino.security.model.User'].USERNAME_REGEX}" data\-checkurl="${actionBaseUrl}/checkavailable"/>
		<@s.password label="%{getText('password')}" name="password" class="required"/>
		<@s.password label="%{getText('confirmPassword')}" name="confirmPassword" class="required"/>
	</#if>
	<@s.textfield label="%{getText('name')}" name="user.name" class="required"/>
	<@s.textfield label="%{getText('email')}" name="user.email" type="email" class="email checkavailable" data\-checkurl="${actionBaseUrl}/checkavailable"/>
	<@s.textfield label="%{getText('phone')}" name="user.phone"/>
	<@s.checkbox label="%{getText('enabled')}" name="user.enabled" class="custom" />
	<@s.checkboxlist label="%{getText('role')}" name="user.roles" list="roles" listKey="value" listValue="label" class="custom">
	<#if hiddenRoles??&&hiddenRoles?size gt 0>
	<@s.param name="after">
		<#list hiddenRoles as role>
			<input type="hidden" name="user.roles" value="${role}"/>
		</#list>
	</@s.param>
	</#if>
	</@s.checkboxlist>
	<@s.submit value="%{getText('save')}" class="btn-primary"/>
</@s.form>
</body>
</html>


