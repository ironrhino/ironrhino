<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('pageView')}</title>
</head>
<body>
<h4 class="center"><#if date??>${date?string('yyyy-MM-dd')}<#else>${getText('total')}</#if></h4>
<ul class="unstyled flotpiechart" style="height:300px;">
	<#if dataMap??>
	<#list dataMap as key,value>
	<li style="float:left;width:50%;">
	<span style="margin-right:10px;">${key}</span>
	<strong class="pull-right" style="margin-right:20px;">${value?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html>
