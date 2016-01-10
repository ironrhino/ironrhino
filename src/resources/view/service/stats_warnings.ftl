<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('service')}${action.getText('stats')}</title>
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
		<caption><h3>${action.getText('warnings')}</h3></caption>
		<thead>
		<tr>
			<th style="width:120px;">${action.getText('source')}</th>
			<th style="width:120px;">${action.getText('target')}</th>
			<th>${action.getText('service')}</th>
			<th style="width:50px;">${action.getText('time')}</th>
			<th style="width:120px;">${action.getText('date')}</th>
		</tr>
		</thead>
		<tbody>
		<#list warnings as var>
		<tr<#if var.failed> class="error"<#else> class="warning"</#if>>
			<td>${var.source!}</td>
			<td>${var.target!}</td>
			<td><a href="<@url value="${baseurl+baseurl?contains('?')?then('&','?')+'service='+(var.service)?url}"/>" class="ajax view" data-replacement="count">${var.service!}</a></td>
			<td>${var.time?string}ms</td>
			<td>${var.date?datetime}</td>
		</tr>
		</#list>
		</tbody>
	</table>
</#if>
</body>
</html></#escape>
