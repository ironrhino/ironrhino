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
<#if warnings?size gt 0>
	<table class="table table-hover table-bordered sortable resizable" style="white-space: nowrap;">
		<caption><h4>${getText('warnings')}</h4></caption>
		<thead>
		<tr>
			<th style="width:120px;">${getText('source')}</th>
			<th style="width:120px;">${getText('target')}</th>
			<th>${getText('service')}</th>
			<th style="width:50px;">${getText('time')} (ms)</th>
			<th style="width:120px;">${getText('date')}</th>
		</tr>
		</thead>
		<tbody>
		<#list warnings as var>
		<tr<#if var.failed> class="error"<#else> class="warning"</#if>>
			<td>${var.source!}</td>
			<td>${var.target!}</td>
			<td><a href="<@url value="${baseurl+baseurl?contains('?')?then('&','?')+'service='+(var.service)?url}"/>" class="ajax view" data-replacement="count">${var.service!}</a></td>
			<td>${var.time?string}</td>
			<td>${var.date?datetime}</td>
		</tr>
		</#list>
		</tbody>
	</table>
</#if>
</body>
</html>
