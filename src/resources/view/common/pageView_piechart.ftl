<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('pageView')}</title>
</head>
<body>
<h3 style="text-align:center;"><#if date??>${date?string('yyyy-MM-dd')}<#else>${action.getText('total')}</#if></h3>
<ul class="unstyled flotpiechart" style="height:300px;">
	<#if dataMap??>
	<#list dataMap?keys as key>
	<li style="float:left;width:50%;">
	<span style="margin-right:10px;">${key}</span>
	<strong class="pull-right" style="margin-right:20px;">${dataMap[key]?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html></#escape>
