<#assign view=Parameters.view!/>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if schema.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('schema')}</title>
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
	<#if view=='brief'><@s.hidden name="schema.name"/><h4>${schema.name!}<#if schema.strict> <span class="label">${action.getText('strict')}</span></#if></h4><#else><@s.textfield label="%{getText('name')}" name="schema.name" class="required checkavailable"/></#if>
	<#if view=='brief'><@s.hidden name="schema.strict"/><#else><@s.checkbox label="%{getText('strict')}" name="schema.strict" class="custom"/></#if>
	<#if view=='brief'><@s.hidden name="schema.description"/><p style="white-space:pre-wrap;word-break:break-all;">${schema.description!}</p><#else><@s.textarea label="%{getText('description')}" name="schema.description" class="input-xxlarge" style="height:50px;" maxlength="4000"/></#if>
	</#if>
	<table class="datagrid nullable table table-condensed">
	<@s.hidden name="__datagrid_schema.fields" />
		<style scoped>
		tr.linkage{
			background-color:#F5F5F5;
		}
		tr.GROUP{
			background-color:#D8D8D8;
		}
		</style>
		<thead>
			<tr>
				<th>${action.getText('name')}</th>
				<th style="width:36%;">${action.getText('value')}</th>
				<th>${action.getText('type')}</th>
				<th>${action.getText('required')}</th>
				<th>${action.getText('strict')}</th>
				<th class="manipulate"></th>
			</tr>
		</thead>
		<tbody>
			<#assign size = 0>
			<#if schema.fields?? && schema.fields?size gt 0>
				<#assign size = schema.fields?size-1>
			</#if>
			<#list 0..size as index>
			<#if schema.fields[index]?? && schema.fields[index].type??>
			</#if>
			<tr class="linkage">
				<td><@s.textfield theme="simple" name="schema.fields[${index}].name" style="width:100px;"/></td>
				<td>
					<table class="datagrid showonadd linkage_component SELECT CHECKBOX">
						<tbody>
							<#assign size = 0>
							<#if schema.fields[index]?? && schema.fields[index].values?? && schema.fields[index].values?size gt 0>
								<#assign size = schema.fields[index].values?size-1>
							</#if>
							<#list 0..size as index2>
							<tr>
								<td><@s.textfield theme="simple" name="schema.fields[${index}].values[${index2}]" class="required" style="width:95%;"/></td>
								<td class="manipulate"></td>
							</tr>
							</#list>
						</tbody>
					</table>
				</td>
				<td><@s.select theme="simple" name="schema.fields[${index}].type" class="linkage_switch required" style="width:80px;" list="@org.ironrhino.common.model.SchemaFieldType@values()" listKey="name" listValue="displayName"/></td>
				<td><span class="showonadd linkage_component SELECT INPUT"><@s.checkbox id="" theme="simple" name="schema.fields[${index}].required" class="custom"/></span></td>
				<td><span class="showonadd linkage_component SELECT"><@s.checkbox id="" theme="simple" name="schema.fields[${index}].strict" class="custom"/></span></td>
				<td class="manipulate"></td>
			</tr>
			</#list>
		</tbody>
	</table>
	<@s.submit value="%{getText('save')}" class="btn-primary"/>
</@s.form>
</body>
</html></#escape>


