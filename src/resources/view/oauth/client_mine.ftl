<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('client')}${getText('list')}</title>
</head>
<body>
<#assign columns={"name":{},"id":{"alias":"client_id","width":"200px"},"secret":{"alias":"client_secret","width":"200px"},"enabled":{"width":"100px"}}>
<#assign actionColumnButtons='<button type="button" class="btn" data-view="show">${getText("view")}</button>'>
<#assign bottomButtons='<@btn view="apply" class="btn-primary"/> <@btn action="disable" confirm=true/> <@btn class="reload"/>'>
<@richtable entityName="client" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons celleditable=false enableable=true/>
</body>
</html>