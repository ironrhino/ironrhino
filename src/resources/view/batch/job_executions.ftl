<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('jobExecution')}${action.getText('list')} - ${action.getText(jobInstance.jobName)!}</title>
</head>
<body>
<#assign actionColumnButtons='<@btn view="steps" windowoptions="{\'width\':\'80%\',\'reloadonclose\':false}"/><#if entity.status.name()=="STARTED"> <@btn action="stop" confirm=true/></#if><#if entity.status.name()=="STOPPED"> <@btn action="restart" confirm=true/> <@btn action="abandon" confirm=true/></#if>'>
<#assign bottomButtons='<@btn class="reload"/>'>
<#assign columns={
"jobParameters":{"template":r"${statics['org.ironrhino.batch.job.JobParameterHelper'].toString(value)}"},
"status":{"width":"100px","template":r"${action.getText(value)}"},
"exitStatus.exitCode":{"alias":"exitStatus","width":"100px","template":r"<#if value?has_content>${action.getText(value)}</#if>"},
"startTime":{"width":"130px"},
"endTime":{"width":"130px"},
"lastExecution.duration":{"alias":"duration","width":"80px","template":r"<#if (entity.endTime)?has_content>${statics['org.ironrhino.core.util.DateUtils'].duration(entity.startTime,entity.endTime)}</#if>"},
"lastUpdated":{"width":"130px"}
}>
<#assign rowDynamicAttributes=r'{"class":"${entity.status.name()?switch("COMPLETED","success","FAILED","error","STARTING","info","STARTED","info","warning")}"}'>
<@richtable entityName="jobExecution" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons rowDynamicAttributes=rowDynamicAttributes/>
</body>
</html>