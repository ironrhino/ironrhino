<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('jobExecution')}${getText('steps')} - ${getText(jobInstance.jobName)!}</title>
</head>
<body>
<div class="accordion" id="steps">
<#list jobExecution.stepExecutions as step>
<div class="accordion-group" style="border-width:2px;border-color:#${step.status.name()?switch('COMPLETED','468847','FAILED','b94a48','STARTING','3a87ad','STARTED','3a87ad','fcf8e3')};">
	<div class="accordion-heading">
	<a class="accordion-toggle" data-toggle="collapse" data-parent="#steps" href="#step${step?index}">
		<h4>${getText(step.stepName)}</h4>
	</a>
	</div>
	<div id="step${step?index}" class="accordion-body collapse<#if !step?has_next> in</#if>">
		<div class="accordion-inner form-horizontal">
		<div class="row-fluid">
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('status')}</label><div class="controls">${getText(step.status)}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('exitStatus')}</label><div class="controls">${(getText(step.exitStatus.exitCode))!}</div></div>
			</div>
			<#if step.endTime??>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('duration')}</label><div class="controls">${statics['org.ironrhino.core.util.DateUtils'].duration(step.startTime,step.endTime)}</div></div>
			</div>
			</#if>
		</div>
		<div class="row-fluid">
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('startTime')}</label><div class="controls">${(step.startTime?datetime)!}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('endTime')}</label><div class="controls">${(step.endTime?datetime)!}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('lastUpdated')}</label><div class="controls">${(step.lastUpdated?datetime)!}</div></div>
			</div>
		</div>
		<div class="row-fluid">
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('readCount')}</label><div class="controls">${(step.readCount)!}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('filterCount')}</label><div class="controls">${(step.filterCount)!}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('writeCount')}</label><div class="controls">${(step.writeCount)!}</div></div>
			</div>
		</div>
		<div class="row-fluid">
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('readSkipCount')}</label><div class="controls">${(step.readSkipCount)!}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('processSkipCount')}</label><div class="controls">${(step.processSkipCount)!}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('writeSkipCount')}</label><div class="controls">${(step.writeSkipCount)!}</div></div>
			</div>
		</div>
		<div class="row-fluid">
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('commitCount')}</label><div class="controls">${(step.commitCount)!}</div></div>
			</div>
			<div class="span4">
				<div class="control-group"><label class="control-label">${getText('rollbackCount')}</label><div class="controls">${(step.rollbackCount)!}</div></div>
			</div>
		</div>
		<#if (step.exitStatus.exitDescription)?has_content>
		<div class="row-fluid">
			<div class="span12">
				<div class="control-group"><label class="control-label">${getText('exitDescription')}</label><div class="controls"><p>${step.exitStatus.exitDescription}</p></div></div>
			</div>
		</div>
		</#if>
		</div>
	</div>
</div>
</#list>
</div>
</body>
</html>