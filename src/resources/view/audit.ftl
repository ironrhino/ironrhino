<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('auditEvent')}</title>
</head>
<body>
	<div id="events" class="scroll-list" data-scrollurl="${actionBaseUrl}" data-pagesize="${pageSize}">
		<#list events as ev>
		<div id="${ev.id}" class="scroll-item row<#if fluidLayout>-fluid</#if>" data-position="${ev.date.time}" style="padding-top:20px;">
			<div class="span2"><em class="time" title="${ev.date?datetime}">${statics['org.ironrhino.core.util.DateUtils'].humanRead(ev.date)}</em></div>
			<div class="span8"><strong>${ev.displayEvent}</strong></div>
			<div class="span2"><span class="label">${ev.address!}</span></div>
		</div>
		</#list>
		<button style="margin-top:10px;" class="btn btn-block load-more"<#if !since?? && events?size==0 || events?size lt pageSize> disabled</#if>>${action.getText('more')}</button>
	</div>
</body>
</html>