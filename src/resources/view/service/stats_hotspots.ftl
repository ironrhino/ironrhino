<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('service')}${getText('stats')}</title>
</head>
<body>
<#assign baseurl=actionBaseUrl>
<#if request.queryString?has_content>
<#list request.queryString?split('&') as pair>
	<#assign name=pair?keep_before('=')>
	<#if name!='_'&&name!='service'>
		<#assign baseurl+=baseurl?contains('?')?then('&','?')+pair>
	</#if>
</#list>
</#if>
<#if hotspots?size gt 0>
	<table class="table table-hover table-striped table-bordered sortable resizable" style="white-space: nowrap;">
		<caption><h4>${getText('hotspots')}</h4></caption>
		<thead>
		<tr>
			<th>${getText('service')}</th>
			<th style="width:120px;">${getText('times')}</th>
		</tr>
		</thead>
		<tbody>
		<#list hotspots as key,value>
		<tr class="warning">
			<td><a href="<@url value="${baseurl+baseurl?contains('?')?then('&','?')+'service='+(key)?url}"/>" class="ajax view" data-replacement="count">${key}</a></td>
			<td>${value?string}</td>
		</tr>
		</#list>
		</tbody>
	</table>
</#if>
</body>
</html>
