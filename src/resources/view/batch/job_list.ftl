<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('job')}${action.getText('list')}</title>
</head>
<body>
<#assign actionColumnButtons='<@btn view="instances" windowoptions="{\'width\':\'95%\',\'reloadonclose\':false}"/> <#if entity.launchable><@btn view="launch"/></#if>'>
<#assign bottomButtons='<@btn class="reload"/>'>
<#assign columns={"name":{"width":"200px","template":r"${action.getText(value)}","dynamicAttributes" : r"<#if entity.description?has_content>{'data-tooltip':'${entity.description}'}</#if>"},
"lastExecution.jobParameters":{"alias":"lastJobParameters","template":r"<#if value?has_content>${statics['org.ironrhino.batch.job.JobParameterHelper'].toString(value)}</#if>"},
"lastExecution.status":{"alias":"lastStatus","width":"120px","template":r"<#if value?has_content>${action.getText(value)}</#if>","dynamicAttributes":r"<#if (entity.lastExecution.exitStatus)?has_content>{'data-tooltip':'${action.getText('exitStatus')+': '+action.getText(entity.lastExecution.exitStatus.exitCode)}'}</#if>"},
"lastExecution.startTime":{"alias":"lastStartTime","width":"130px"},
"lastExecution.endTime":{"alias":"lastEndTime","width":"130px"},
"lastExecution.duration":{"alias":"duration","width":"80px","template":r"<#if (entity.lastExecution.endTime)?has_content>${statics['org.ironrhino.core.util.DateUtils'].duration(entity.lastExecution.startTime,entity.lastExecution.endTime)}</#if>"}}>
<#assign rowDynamicAttributes=r'{"class":"${(entity.lastExecution.status.name())!?switch("COMPLETED","success","FAILED","error","STARTING","info","STARTED","info","warning")}"}'>
<@richtable entityName="job" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons rowDynamicAttributes=rowDynamicAttributes/>
</body>
</html>