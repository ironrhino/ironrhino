<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('page')}${action.getText('list')}</title>
</head>
<body>
<#assign columns={"path":{"alias":"path","width":"200px","template":"<a href=\"${getUrl(cmsPath)}$"+"{value}\" target=\"_blank\">$"+"{value}</a>"},"title":{},"displayOrder":{"width":"100px"},"tag":{"template":r"<#list entity.tags as tag><a href='<@url value='/common/page?keyword=tags:${tag}'/>' class='ajax view'>${tag}</a>&nbsp;&nbsp;</#list>"},"createDate":{"width":"130px"},"modifyDate":{"width":"130px"}}>
<#assign actionColumnButtons='
<button type="button" class="btn" data-view="input" data-windowoptions="{\'iframe\':true,\'width\':\'80%\',\'includeParams\':true}">${action.getText("edit")}</button>
'>
<#assign bottomButtons='
<button type="button" class="btn" data-view="input" data-windowoptions="{\'iframe\':true,\'width\':\'80%\'}">${action.getText("create")}</button>
<button type="button" class="btn confirm" data-action="delete" data-shown="selected">${action.getText("delete")}</button>
<button type="button" class="btn reload">${action.getText("reload")}</button>
<button type="button" class="btn filter">${action.getText("filter")}</button>
'>
<@richtable entityName="page" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons celleditable=false searchable=true/>
</body>
</html></#escape>
