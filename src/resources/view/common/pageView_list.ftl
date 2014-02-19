<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('pageView')}</title>
</head>
<body>
<h3 style="text-align:center;"><#if date??>${date?string('yyyy-MM-dd')}<#else>${action.getText('total')}</#if></h3>
<ul class="unstyled">
	<#if dataMap??>
	<#list dataMap?keys as key>
	<li>
	<span style="margin-right:10px;word-break:break-all;">${key}</span>
	<strong class="pull-right">${dataMap[key]?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html></#escape>
