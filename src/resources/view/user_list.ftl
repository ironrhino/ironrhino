<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('user')}${getText('list')}</title>
</head>
<body>
<#assign actionColumnButtons=r'<@btn view="view"/> <@btn view="input" label="edit"/>'>
<#assign columns={"username":{"width":"120px"},"name":{"width":"120px"},"email":{"width":"180px"},"phone":{"width":"120px"},"roles":{"template":r'<#list value as r><span class="label">${beans["userRoleManager"].displayRole(r)}</span><#sep> </#list>'},"enabled":{"width":"80px"}}>
<@richtable columns=columns actionColumnButtons=actionColumnButtons searchable=true celleditable=false enableable=true/>
</body>
</html>