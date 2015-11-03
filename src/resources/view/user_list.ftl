<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('user')}${action.getText('list')}</title>
</head>
<body>
<#assign actionColumnButtons=r'<@btn view="view"/> <@btn view="input" label="edit"/>'>
<#assign columns={"username":{"width":"120px"},"name":{"width":"120px"},"email":{"width":"180px"},"phone":{"width":"120px"},"roles":{"alias":"role","template":r'<#list value as r><span class="label">${beans["userRoleManager"].displayRole(r)}</span><#sep> </#list>'},"enabled":{"width":"80px"}}>
<@richtable entityName="user" columns=columns actionColumnButtons=actionColumnButtons searchable=true celleditable=false enableable=true/>
</body>
</html></#escape>