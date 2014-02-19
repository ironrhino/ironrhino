<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('user')}${action.getText('list')}</title>
</head>
<body>
<#assign columns={"username":{"width":"120px"},"name":{"width":"120px"},"email":{"width":"180px"},"phone":{"width":"120px"},"roles":{"alias":"role","template":r"<#list value as r>${statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('userRoleManager').displayRole(r)}<#if r_has_next> </#if></#list>"},"enabled":{"width":"80px"}}>
<@richtable entityName="user" columns=columns searchable=true celleditable=false enableable=true/>
</body>
</html></#escape>