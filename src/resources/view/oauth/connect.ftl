<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('login')}</title>
</head>
<body>
<#if providers??>
<div class="row<#if fluidLayout>-fluid</#if>">
<div class="span4 offset4">
<div class="row<#if fluidLayout>-fluid</#if>">
<#list providers as var>
<div class="span2" style="height:100px;">
<a href="${request.requestURL}?id=${var.name}<#if targetUrl??>&targetUrl=${targetUrl?url}</#if>">
	<img src="${var.logo}" alt="${getText(var.name)}" title="${getText(var.name)}"/>
</a>
</div>
</#list>
</div>
</div>
</div>
</#if>
</body>
</html>
