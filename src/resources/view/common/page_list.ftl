<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('page')}${action.getText('list')}</title>
</head>
<body>
<#assign columns={"path":{"alias":"path","width":"300px","template":"<a href=\"${getUrl(cmsPath)}$"+"{value}\" target=\"_blank\">$"+"{value}</a>"},"title":{},"tag":{"template":r"<#list entity.tags as tag><a href='<@url value='/common/page?keyword=tags:${tag}'/>' class='ajax view'>${tag}</a>&nbsp;&nbsp;</#list>"},"createDate":{"width":"130px"},"modifyDate":{"width":"130px"},"displayOrder":{"width":"60px"}}>
<#assign actionColumnButtons='<@btn view="input" label="edit" windowoptions="{\'iframe\':true,\'width\':\'80%\',\'includeParams\':true}"/>'>
<#assign bottomButtons='
<@btn view="input" label="create" windowoptions="{\'iframe\':true,\'width\':\'80%\'}"/>
<@btn action="delete" confirm=true/>
<@btn class="reload"/>
<@btn class="filter"/>
'>
<@richtable entityName="page" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons celleditable=false searchable=true/>
</body>
</html></#escape>
