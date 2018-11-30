<#ftl output_format='HTML'>
<#assign service=uid!>
<!DOCTYPE html>
<html>
<head>
<title>${service}</title>
</head>
<body>
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
		<@s.form id="method-form-${m?index}" action="${actionBaseUrl}/${service}" method="post" class="ajax form-horizontal">
		<@s.hidden name="method" value=m.signature/>
		<#list m.parameters as p>
			<#if p.integralNumeric>
				<@s.textfield type="number" label=p.name name="params['"+p.name+"']" value=p.sample! class="integer${p.required?then(' required','')}"/>
			<#elseif p.integralNumeric>
				<@s.textfield type="number" label=p.name name="params['"+p.name+"']" value=p.sample! class="double${p.required?then(' required','')}"/>
			<#elseif p.bool>
				<#if p.required>
				<@s.checkbox label=p.name name="params['"+p.name+"']" value=p.sample!false/>
				<#else>
				<@s.select label=p.name name="params['"+p.name+"']" value=p.sample! list={'true':getText('true'),'false':getText('false')} headerKey="" headerValue=""/>
				</#if>
			<#elseif p.enum>
				<@s.select label=p.name name="params['"+p.name+"']" value=p.sample! list="@${p.type.name}@values()" listKey="name" listValue="name" headerKey="" headerValue="" class="${p.required?then('required','')}"/>
			<#elseif p.temporal>
				<@s.textfield label=p.name name="params['"+p.name+"']" value=(p.sample?substring(1,p.sample?length-1))! class="datetime${p.required?then(' required','')}"/>
			<#else>
				<#if p.multiline>
				<@s.textarea label=p.name name="params['"+p.name+"']" value=p.sample! class="input-xxlarge${p.required?then(' required','')}"/>
				<#else>
				<@s.textfield label=p.name name="params['"+p.name+"']" value=p.sample! class="input-xxlarge${p.required?then(' required','')}"/>
				</#if>
			</#if>
		</#list>
		<@s.submit label=getText('invoke') class="btn-primary"/>
		</@s.form>
	</div>
	</div>
	</div>
</#list>
</div>
</body>
</html>
