<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('pageView')}</title>
</head>
<body>
<h4 class="center"><#if date??>${date?string('yyyy-MM-dd')}<#else>${getText('total')}</#if></h4>
<ul class="unstyled">
	<#if dataMap??>
	<#list dataMap as key,value>
	<li>
	<span style="margin-right:10px;word-break:break-all;">${key}</span>
	<strong class="pull-right">${value?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html>