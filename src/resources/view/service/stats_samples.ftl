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
			<th style="width:120px;">${action.getText('start')}</th>
			<th style="width:120px;">${action.getText('end')}</th>
		</tr>
		</thead>
		<tbody>
		<#assign count=0>
		<#assign totalTime=0>
		<#list samples as var>
		<#assign count+=var.count>
		<#assign totalTime+=var.totalTime>
		<#if !start??||start.after(var.start)>
		<#assign start=var.start>
		</#if>
		<#if !end??||end.before(var.end)>
		<#assign end=var.end>
		</#if>
		<tr>
			<td>${var.host!}</td>
			<td>${var.count?string}</td>
			<td>${var.meanTime?string}</td>
			<td>${var.start?datetime}</td>
			<td>${var.end?datetime}</td>
		</tr>
		</#list>
		</tbody>
		<#if samples?size gt 1>
		<tfoot>
		<tr>
			<td></td>
			<td>${count}</td>
			<td>${(totalTime/count)?string}</td>
			<td>${start?datetime}</td>
			<td>${end?datetime}</td>
		</tr>
		</tfoot>
		</#if>
	</table>
</#if>
</body>
</html></#escape>
