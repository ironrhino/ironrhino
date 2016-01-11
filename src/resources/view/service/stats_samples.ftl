<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('service')}${action.getText('stats')}</title>
</head>
<body>
<#if samples?has_content>
	<table class="table table-hover table-bordered sortable resizable" style="white-space: nowrap;">
		<caption><strong>${action.getText('samples')}</strong></caption>
		<thead>
		<tr>
			<th>${action.getText('host')}</th>
			<th style="width:120px;">${action.getText('count')}</th>
			<th style="width:120px;">${action.getText('meanTime')} (ms)</th>
			<th style="width:150px;">${action.getText('start')}</th>
			<th style="width:150px;">${action.getText('end')}</th>
		</tr>
		</thead>
		<tbody>
		<#list samples as var>
		<tr>
			<td>${var.host!}</td>
			<td>${var.count?string}</td>
			<td>${var.meanTime?string}</td>
			<td>${var.start?datetime}</td>
			<td>${var.end?datetime}</td>
		</tr>
		</#list>
		</tbody>
	</table>
</#if>
</body>
</html></#escape>
