<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('pageView')}</title>
</head>
<body>
<div style="padding:5px;">
	<span style="margin-right:10px;">${date?string('yyyy-MM-dd')}</span>
</div>
<ul class="unstyled flotbarchart" style="height:300px;">
	<#if dataList??>
	<#list dataList as var>
	<li style="float:left;width:200px;padding:10px;">
	<span>${var.a?string('HH')}</span>
	<strong class="pull-right" style="margin-right:10px;">${var.b?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html></#escape>
