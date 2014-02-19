<#assign view=Parameters.view!/>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if schema.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('schema')}</title>
</head>
<body>
<@s.form id="schema_input" action="${actionBaseUrl}/save" method="post" cssClass="ajax${view?has_content?string('',' importable')}" cssStyle="text-align:center;">
	<#if !schema.new>
		<@s.hidden name="schema.id" />
	</#if>
	<@s.hidden name="schema.version" cssClass="version" />
	<#if view=='embedded'>
	<@s.hidden name="schema.name"/>
	<@s.hidden name="schema.description"/>
	<@s.hidden name="schema.strict"/>
	<#else>
	<div class="row-fluid">
		<div class="span4"><span>${action.getText('name')}: </span><#if view=='brief'><@s.hidden name="schema.name"/>${schema.name!}<#else><@s.textfield theme="simple" name="schema.name" cssClass="required checkavailable input-medium"/></#if></div>
		<div class="span5"><span>${action.getText('description')}: </span><#if view=='brief'><@s.hidden name="schema.description" />${schema.description!}<#else><@s.textfield theme="simple" name="schema.description"/></#if></div>
		<div class="span3"><span>${action.getText('strict')}: </span><#if view=='brief'><@s.hidden name="schema.strict" />${action.getText(schema.strict?string)}<#else><@s.checkbox theme="simple" name="schema.strict" cssClass="custom"/></#if></div>
	</div>
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
				<td><@s.textfield theme="simple" name="schema.fields[${index}].name" cssStyle="width:100px;"/></td>
				<td>
					<table class="datagrid showonadd linkage_component SELECT CHECKBOX">
						<tbody>
							<#assign size = 0>
							<#if schema.fields[index]?? && schema.fields[index].values?? && schema.fields[index].values?size gt 0>
								<#assign size = schema.fields[index].values?size-1>
							</#if>
							<#list 0..size as index2>
							<tr>
								<td><@s.textfield theme="simple" name="schema.fields[${index}].values[${index2}]" cssClass="required" cssStyle="width:95%;"/></td>
								<td class="manipulate"></td>
							</tr>
							</#list>
						</tbody>
					</table>
				</td>
				<td><@s.select theme="simple" name="schema.fields[${index}].type" cssClass="linkage_switch" cssStyle="width:80px;" list="@org.ironrhino.common.model.SchemaFieldType@values()" listKey="name" listValue="displayName"/></td>
				<td><span class="showonadd linkage_component SELECT INPUT"><@s.checkbox theme="simple" name="schema.fields[${index}].required"/></span></td>
				<td><span class="showonadd linkage_component SELECT"><@s.checkbox theme="simple" name="schema.fields[${index}].strict"/></span></td>
				<td class="manipulate"></td>
			</tr>
			</#list>
		</tbody>
	</table>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html></#escape>


