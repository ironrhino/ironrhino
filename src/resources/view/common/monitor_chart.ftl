<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('monitor')}</title>
<script src="<@url value="/assets/components/flot/jquery.flot.js"/>" type="text/javascript"></script>
<script src="<@url value="/assets/components/flot/jquery.flot.time.js"/>" type="text/javascript"></script>
<script src="<@url value="/assets/components/flot/ironrhino.flot.js"/>" type="text/javascript"></script>
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
<ul class="unstyled flotbarchart" style="height:600px;"<#if Parameters.live??> data-interval="${Parameters.interval!'60000'}" data-quiet="true"</#if>>
	<#if dataList??>
	<#list dataList as var>
	<li style="float:left;width:200px;padding:10px;">
	<span>${var.key?string('HH')}</span>
	<strong class="pull-right" style="margin-right:10px;">${var.value?string}</strong>
	</li>
	</#list>
	</#if>
</ul>
</body>
</html>
