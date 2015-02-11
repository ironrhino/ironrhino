<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('install')}</title>
</head>
<body>
<#assign columns={"id":{"width":"250px"},"vendor":{"width":"150px"},"version":{"width":"100px"},"dependence":{}}>
<#assign actionColumnButtons=r'
<@btn action="uninstall" confirm=true/>
<#if entity.rollbackable>
<@btn action="rollback" confirm=true/>
</#if>
'>
<#assign bottomButtons='
<button type="button" class="btn" onclick="$(\'#install\').toggle()">${action.getText("install")}</button>
'>
<@richtable entityName="install" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons showCheckColumn=false/>
<form id="install" action="${actionBaseUrl}/install" method="post" enctype="multipart/form-data" style="display:none;text-align:center;padding-top:20px;" class="form-inline">
<input type="file" name="file"/> <@s.submit theme="simple" value="${action.getText('upload')}"/>
</form>
</body>
</html></#escape>