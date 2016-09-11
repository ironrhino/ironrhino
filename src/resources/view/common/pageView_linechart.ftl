<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('pageView')}</title>
</head>
<body>
<div style="padding:5px;">
	<span style="margin-right:10px;">${from?string('yyyy-MM-dd')} -> ${to?string('yyyy-MM-dd')}</span>
	<#if max??>
	<span class="pull-right" style="margin:0 10px;">${action.getText('max')}:
	<strong>${max.value?string}</strong>
	${max.key?string('yyyy-MM-dd')}
	</span>
	</#if>
	<#if total??>
	<span class="pull-right" style="margin:0 10px;">${action.getText('total')}:<strong>${total?string}</strong></span>
	</#if>
</div>
<ul class="unstyled flotlinechart" style="height:300px;" data-format="<#if date??>%H(%m-%d)<#else>%m-%d</#if>">
	<#if dataList??>
	<#list dataList as var>
	<li style="float:left;width:200px;padding:10px;">
	<span data-time="${var.key.time}">${var.key?string('yyyy-MM-dd')}</span>
	<strong class="pull-right" style="margin-right:10px;">${var.value?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html>
