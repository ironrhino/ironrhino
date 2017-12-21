<#ftl output_format='HTML'>
<#assign view=Parameters.view!/>
<!DOCTYPE html>
<html>
<head>
<title><#if schema.new>${getText('create')}<#else>${getText('edit')}</#if>${getText('schema')}</title>
</head>
<body>
<@s.form id="schema_input" action="${actionBaseUrl}/save" method="post" class="form-horizontal ajax${view?has_content?then('',' importable')}">
	<#if !schema.new>
		<@s.hidden name="schema.id" />
	</#if>
	<@s.hidden name="schema.version" class="version" />
	<#if view=='embedded'>
	<@s.hidden name="schema.name"/>
	<@s.hidden name="schema.description"/>
	<@s.hidden name="schema.strict"/>
	<#else>
	<#if view=='brief'><@s.hidden name="schema.name"/><h4>${schema.name!}<#if schema.strict> <span class="label">${getText('strict')}</span></#if></h4><#else><@s.textfield name="schema.name" class="required checkavailable"/></#if>
	<#if view=='brief'><@s.hidden name="schema.strict"/><#else><@s.checkbox name="schema.strict" class="switch"/></#if>
	<#if view=='brief'><@s.hidden name="schema.description"/><p>${schema.description!}</p><#else><@s.textarea name="schema.description" class="input-xxlarge" style="height:50px;" maxlength="4000"/></#if>
	</#if>
	<@s.hidden name="__datagrid_schema.fields"/>
	<table class="datagrid adaptive nullable table table-condensed">
		<style>
		tr.linkage{
			background-color:#F5F5F5;
		}
		tr.GROUP{
			background-color:#D8D8D8;
		}
		</style>
		<thead>
			<tr>
				<th>${getText('name')}</th>
				<th >${getText('value')}</th>
				<th style="width:60px;">${getText('type')}</th>
				<th>${getText('required')}</th>
				<th>${getText('strict')}</th>
				<th class="manipulate"></th>
			</tr>
		</thead>
		<tbody>
			<#assign size = 0>
			<#if schema.fields?? && schema.fields?size gt 0>
				<#assign size = schema.fields?size-1>
			</#if>
			<#list 0..size as index>
			<tr class="linkage">
				<td><@s.textfield theme="simple" name="schema.fields[${index}].name"/></td>
				<td>
					<table class="datagrid adaptive showonadd linkage_component SELECT CHECKBOX">
						<tbody>
							<#assign size = 0>
							<#if schema.fields[index]?? && schema.fields[index].values?? && schema.fields[index].values?size gt 0>
								<#assign size = schema.fields[index].values?size-1>
							</#if>
							<#list 0..size as index2>
							<tr>
								<td><@s.textfield theme="simple" name="schema.fields[${index}].values[${index2}]" class="required"/></td>
								<td class="manipulate"></td>
							</tr>
							</#list>
						</tbody>
					</table>
				</td>
				<td><@s.select theme="simple" name="schema.fields[${index}].type" class="linkage_switch required" list="@org.ironrhino.common.model.SchemaFieldType@values()" listKey="name" listValue="displayName"/></td>
				<td><span class="showonadd linkage_component SELECT INPUT"><@s.checkbox id="" theme="simple" name="schema.fields[${index}].required"/></span></td>
				<td><span class="showonadd linkage_component SELECT"><@s.checkbox id="" theme="simple" name="schema.fields[${index}].strict"/></span></td>
				<td class="manipulate"></td>
			</tr>
			</#list>
		</tbody>
	</table>
	<@s.submit label=getText('save') class="btn-primary"/>
</@s.form>
</body>
</html>


