<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('service')}${getText('stats')}</title>
</head>
<body>
<#if samples?has_content>
	<table class="table table-hover table-bordered sortable resizable" style="white-space: nowrap;">
		<caption><h4>${getText('samples')}</h4></caption>
		<thead>
		<tr>
			<th>${getText('host')}</th>
			<th style="width:120px;">${getText('count')}</th>
			<th style="width:120px;">${getText('meanTime')} (ms)</th>
			<th style="width:120px;">${getText('start')}</th>
			<th style="width:120px;">${getText('end')}</th>
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
			<td>${(totalTime/count)?string('0')}</td>
			<td>${start?datetime}</td>
			<td>${end?datetime}</td>
		</tr>
		</tfoot>
		</#if>
	</table>
</#if>
</body>
</html>
