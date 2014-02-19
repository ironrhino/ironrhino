<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('client')}${action.getText('list')}</title>
</head>
<body>
<#assign columns={"name":{},"id":{},"secret":{},"enabled":{}}>
<#assign actionColumnButtons='<button type="button" class="btn" data-view="show">${action.getText("show")}</button><button type="button" class="btn" data-action="disable">${action.getText("disable")}</button>'>
<#assign bottomButtons='<button type="button" class="btn reload">${action.getText("reload")}</button>'>
<@richtable entityName="client" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons celleditable=false/>
</body>
</html></#escape>