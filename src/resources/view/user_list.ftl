<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('user')}${action.getText('list')}</title>
</head>
<body>
<#assign actionColumnButtons=r'
<button type="button" class="btn" data-view="view">${action.getText("view")}</button>
<button type="button" class="btn" data-view="input">${action.getText("edit")}</button>
'>
<#assign columns={"username":{"width":"120px"},"name":{"width":"120px"},"email":{"width":"180px"},"phone":{"width":"120px"},"roles":{"alias":"role","template":r"<#list value as r>${statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('userRoleManager').displayRole(r)}<#if r_has_next> </#if></#list>"},"enabled":{"width":"80px"}}>
<@richtable entityName="user" columns=columns actionColumnButtons=actionColumnButtons searchable=true celleditable=false enableable=true/>
</body>
</html></#escape>