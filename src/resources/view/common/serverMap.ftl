<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('serverMap')}</title>
</head>
<body>
<#assign serverMap = beans['applicationContextInspector'].serverMap>
<table class="table">
	<caption><h3>${serverMap.name}</h3></caption>
	<thead>
		<tr>
			<th style="width:200px;">Service</th>
			<th style="width:200px;">Version</th>
			<th>Address</th>
		</tr>
	</thead>
	<tbody>
	<#list serverMap.services as service>
		<tr>
			<td>${service.type}</td>
			<td>${service.version!}</td>
			<td>${service.address!}</td>
		</tr>
	</#list>
	</tbody>
</table>
</body>
</html>