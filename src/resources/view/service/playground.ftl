<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('playground')}</title>
</head>
<body>
<#assign service=uid!>
<h3>${service}</h3>
<div class="accordion" id="methods-accordion">
<#list servicePlayground.getMethods(service!) as m>
	<div class="accordion-group">
	<div class="accordion-heading">
		<a class="accordion-toggle" data-toggle="collapse" data-parent="#methods-accordion" href="#method-${m?index}">
		<h4>${m.signature}</h4>
		</a>
	</div>
	<div id="method-${m?index}" class="accordion-body collapse">
	<div class="accordion-inner">
		<#if m.concrete>
		<@s.form id="method-form-${m?index}" action="${actionBaseUrl}/${service}" method="post" class="ajax form-horizontal">
		<@s.hidden name="method" value=m.signature/>
		<#list m.parameters as p>
			<#if p.multiline>
			<@s.textarea label=p.name name="params['"+p.name+"']" value=p.sample!/>
			<#else>
			<@s.textfield label=p.name name="params['"+p.name+"']" value=p.sample!/>
			</#if>
		</#list>
		<@s.submit label=getText('invoke') class="btn-primary"/>
		</@s.form>
		<#else>
		Not concrete!
		</#if>
	</div>
	</div>
	</div>
</#list>
</div>
</body>
</html>
