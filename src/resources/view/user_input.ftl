<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if user.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('user')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/save" method="post" cssClass="ajax form-horizontal">
	<#if !user.new>
		<@s.hidden name="user.id" />
		<@s.textfield label="%{getText('username')}" name="user.username" readonly="true"/>
		<@s.password label="%{getText('password')}" name="password"/>
		<@s.password label="%{getText('confirmPassword')}" name="confirmPassword"/>
	<#else>
		<@s.textfield label="%{getText('username')}" name="user.username" cssClass="required checkavailable regex" dynamicAttributes={"data-regex":"${statics['org.ironrhino.security.model.User'].USERNAME_REGEX}","data-checkurl":"${actionBaseUrl}/checkavailable"}/>
		<@s.password label="%{getText('password')}" name="password" cssClass="required"/>
		<@s.password label="%{getText('confirmPassword')}" name="confirmPassword" cssClass="required"/>
	</#if>
	<@s.textfield label="%{getText('name')}" name="user.name" cssClass="required"/>
	<@s.textfield label="%{getText('email')}" name="user.email" type="email" cssClass="email checkavailable" dynamicAttributes={"data-checkurl":"${actionBaseUrl}/checkavailable"}/>
	<@s.textfield label="%{getText('phone')}" name="user.phone"/>
	<@s.checkbox label="%{getText('enabled')}" name="user.enabled" cssClass="custom" />
	<@s.checkboxlist label="%{getText('role')}" name="user.roles" list="roles" listKey="value" listValue="label" cssClass="custom">
	<#if hiddenRoles??&&hiddenRoles?size gt 0>
	<@s.param name="after">
		<#list hiddenRoles as role>
			<input type="hidden" name="user.roles" value="${role}"/>
		</#list>
	</@s.param>
	</#if>
	</@s.checkboxlist>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html></#escape>


