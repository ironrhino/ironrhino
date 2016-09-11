<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('monitor')}</title>
<script src="<@url value="/assets/components/flot/jquery.flot.js"/>" type="text/javascript"></script>
<script src="<@url value="/assets/components/flot/jquery.flot.pie.js"/>" type="text/javascript"></script>
<script src="<@url value="/assets/components/flot/ironrhino.flot.js"/>" type="text/javascript"></script>
</head>
<body>
<ul class="unstyled flotpiechart" style="height:300px;">
	<#if treeNode??>
	<#list treeNode.children as t>
	<li style="float:left;width:50%;">
	<span style="margin-right:10px;">${t.name}</span>
	<strong class="pull-right" style="margin-right:20px;">${t.value[(vtype=='d')?string('doubleValue','longValue')]?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html>
