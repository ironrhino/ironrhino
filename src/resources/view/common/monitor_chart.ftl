<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('monitor')}</title>
</head>
<body>
<div class="row<#if fluidLayout>-fluid</#if>">
<div class="span6">
<form action="${actionBaseUrl}/chart/${uid}" class="ajax view form-inline" data-replacement="chart">
<@s.hidden name="vtype"/>
<@s.hidden name="ctype"/>
<span>${getText('date')}</span>
<@s.textfield theme="simple" name="date" class="date"/>
<@s.submit label=getText('query') theme="simple"/>
</form>
</div>
<div class="span6">
<form action="${actionBaseUrl}/chart/${uid}" class="ajax view form-inline" data-replacement="chart">
<@s.hidden name="vtype"/>
<@s.hidden name="ctype"/>
<span>${getText('date.range')}</span>
<@s.textfield theme="simple" name="from" class="date"/>
<i class="glyphicon glyphicon-arrow-right"></i>
<@s.textfield theme="simple" name="to" class="date"/>
<@s.submit label=getText('query') theme="simple"/>
</form>
</div>
</div>
<#assign dataurl='${actionBaseUrl}/data'/>
<#if uid??>
<#assign dataurl+='/'+uid>
</#if>
<#if request.queryString??>
<#assign dataurl+='?'+request.queryString>
</#if>
<div id="chart" class="chart" data-url="${dataurl}" style="height:600px;"<#if Parameters.live??> data-interval="${Parameters.interval!'60000'}" data-quiet="true"</#if>>
</div>
</body>
</html>
