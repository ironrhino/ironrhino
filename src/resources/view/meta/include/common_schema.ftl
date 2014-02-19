<#macro editAttributes schemaName merge=false attributes=[] parameterNamePrefix=""  headerKey="" headerValue="" ignoreIfNotFound=true dynamicAttributesMapping={}>
	<#local schemaManager=statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('schemaManager')/>
	<#if schemaName?is_string>
		<#local schema=schemaManager.findOne(true,[schemaName])!>
	<#elseif schemaName?is_sequence && schemaName?size gt 0>
		<#if schemaName?size == 1>
			<#local schema=schemaManager.findOne(true,schemaName)!>
		<#else>
			<#if merge>
			<#list schemaName as name>
				<#local temp=schemaManager.findOne(true,[name])!>
				<#local tempisnotnull=schemaManager.findOne(true,[name])??>
				<#if tempisnotnull>
					<#if !schema?? && temp??>
						<#local schema=temp>
					<#elseif schema.name?? && temp??>
						<#local schema=schema.merge(temp)>
					</#if>
				</#if>
			</#list>
			<#else>
			<#list schemaName as name>
				<#local temp=schemaManager.findOne(true,[name])!>
				<#local tempisnotnull=schemaManager.findOne(true,[name])??>
				<#if tempisnotnull>
					<#if !schema?? && temp??>
						<#local schema=temp>
						<#break>
					</#if>
				</#if>
			</#list>
			</#if>
		</#if>
	</#if>
	<#if !ignoreIfNotFound || schema??&&schema.fields??&&schema.fields?size gt 0>
	<input type="hidden" name="__datagrid_${parameterNamePrefix}attributes"/>
	<table class="datagrid table table-condensed nullable">
		<thead>
			<tr>
				<td>${action.getText('name')}</td>
				<td>${action.getText('value')}</td>
				<#if !schema.strict?? || !schema.strict>
				<td class="manipulate"></td>
				</#if>
			</tr>
		</thead>
		<tbody>
			<#if schema?? && schema.name??>
				<#local index = 0>
				<#list schema.fields as field>
				<#if field.type??>
					<#local type=field.type.name()/>
				<#else>
					<#local type='SELECT'/>
				</#if>
				<#if type=='GROUP'>
				<tr class="nontemplate" style="background-color:#F0F0F0;height:1em;">
					<td colspan="2">${field.name?html}<@s.hidden theme="simple" name="${parameterNamePrefix}attributes[${index}].name" value="${field.name?html}"/></td>
					<#if !schema.strict>
					<td class="manipulate"></td>
					</#if>
				</tr>
				<#else>
				<#local persistValueExists=false>
				<#list attributes as attr>
					<#if attr.name==field.name && attr.value??>
						<#local persistValue=attr.value>
						<#local persistValueExists=true>
						<#break/>
					</#if>
				</#list>
				<tr<#if field.required> class="required"</#if>>
					<#local dynamicAttributes = dynamicAttributesMapping[field.name]!{}>
					<td><@s.textfield theme="simple" id="${parameterNamePrefix?replace('.','_')}attributes__${index}__name" name="${parameterNamePrefix}attributes[${index}].name" value="${field.name?html}" readonly=(schema.strict || field.strict || field.required)?string/></td>
					<td>
						<#if type=='SELECT'>
							<select id="${parameterNamePrefix?replace('.','_')}attributes__${index}__value" name="${parameterNamePrefix}attributes[${index}].value" class="textonadd<#if field.required> required</#if><#if !field.strict> combobox</#if> ${dynamicAttributes['class']!}"<#list dynamicAttributes?keys as attr><#if attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>>
								<option value="${headerKey?html}">${headerValue?html}</option>
								<#list field.values as value>
								<option value="${value}"<#if persistValueExists && persistValue=value> selected="selected"</#if>>${value?html}</option>
								</#list>
								<#if !field.strict && persistValueExists && persistValue!='' && !field.values?seq_contains(persistValue)>
								<option value="${persistValue?html}" selected="selected">${persistValue?html}</option>
								</#if>
							</select>
						<#elseif type=='CHECKBOX'>
							<#if persistValueExists>
								<#local persistValueArray=persistValue?split(',')/>
							<#else>
								<#local persistValueArray=[]/>
							</#if>
							<#list field.values as value>
								<label for="${parameterNamePrefix?replace('.','_')}attributes_${index}_value_${value_index}" class="checkbox inline removeonadd"><input id="${parameterNamePrefix?replace('.','_')}attributes_${index}_value_${value_index}" type="checkbox" name="${parameterNamePrefix}attributes[${index}].value" value="${value!?html}" class="textonadd custom ${dynamicAttributes['class']!}"<#list persistValueArray as tempValue><#if tempValue=value> checked="checked"<#break/></#if></#list><#list dynamicAttributes?keys as attr><#if attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>>${value!?html}</label>
							</#list>
						<#elseif type=='INPUT'>
							<input id="${parameterNamePrefix?replace('.','_')}attributes__${index}__value" type="text" name="${parameterNamePrefix}attributes[${index}].value"<#if persistValueExists> value="${persistValue?html}"</#if> class="<#if field.required>required</#if> ${dynamicAttributes['class']!}"<#list dynamicAttributes?keys as attr><#if attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>/>
						</#if>
					</td>
					<#if !schema.strict>
					<td class="manipulate"></td>
					</#if>
				</tr>
				</#if>
				<#local index = index+1>
				</#list>
				<#if !schema.strict>
				<#list attributes as attr>
					<#local inschema=false>
					<#list schema.fields as field>
						<#if attr.name==field.name>
							<#local inschema=true>
							<#break/>
						</#if>
					</#list>
					<#if !inschema && attr.value?? && attr.value?has_content>
					<tr>
						<td><input type="text" name="${parameterNamePrefix}attributes[${index}].name" value="${attr.name!?html}"/></td>
						<td><input type="text" name="${parameterNamePrefix}attributes[${index}].value" value="${attr.value!?html}"/></td>
						<td class="manipulate"></td>
					</tr>
					<#local index = index+1>
					</#if>
				</#list>
				</#if>
				<#else>
					<#local size = 0>
					<#local isnew=true>
					<#if attributes?? && attributes?size gt 0>
						<#local size = attributes?size-1>
						<#local isnew=false>
					</#if>
					<#local index = 0>
					<#list 0..size as var>
						<#if isnew || attributes[var].value?? && attributes[var].value?has_content>
							<tr>
								<td><input type="text" name="${parameterNamePrefix}attributes[${index}].name"<#if !isnew> value="${attributes[var].name!?html}</#if>"/></td>
								<td><input type="text" name="${parameterNamePrefix}attributes[${index}].value"<#if !isnew> value="${attributes[var].value!?html}</#if>"/></td>
								<td class="manipulate"></td>
							</tr>
							<#local index = index+1>
						</#if>
					</#list>
				</#if>
		</tbody>
	</table>
	</#if>
</#macro>

<#macro printAttributes attributes grouping=true excludes=[]>
	<#if attributes?? && attributes?size gt 0>
		<#if !grouping>
			<ul class="attributes">
			<#list attributes as attr>
				<#if attr.value?? && attr.value?has_content && !excludes?seq_contains(attr.name)>
					<li><span class="name">${attr.name?html}:<span><span class="value">${attr.value?html}</span></li>
				</#if>
			</#list>
			</ul>
		<#else>
			<ul class="attributes">
			<#local group = ""/>
			<#list attributes as attr>
				<#if !attr.value?? || !attr.value?has_content>
					<#local name = attr.name/>
					<#if (!name?has_content)>
						<#local group = ""/>
						</ul></li>
					<#else>
						<#if group?has_content>
							</ul></li>
						</#if>
						<#local group = name/>
						<#if group?has_content>
							<li class="group"><span class="group">${group?html}</span>
							<ul>
						</#if>
					</#if>
				<#else>
					<#if !excludes?seq_contains(attr.name)>
					<li><span class="name">${attr.name?html}:<span><span class="value">${attr.value?html}</span></li>
					</#if>
					<#if group?has_content && !attr_has_next>
						</ul></li>
					</#if>
				</#if>
			</#list>
			</ul>
		</#if>
	</#if>
</#macro>
<#function getAttributeOptions schemaName name>
	<#local schemaManager=statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('schemaManager')/>
	<#if schemaName?is_string>
		<#local schema=schemaManager.findOne(true,[schemaName])!>
	<#elseif schemaName?is_sequence && schemaName?size gt 0>
		<#if schemaName?size == 1>
			<#local schema=schemaManager.findOne(true,schemaName)!>
		<#else>
			<#if merge>
			<#list schemaName as name>
				<#local temp=schemaManager.findOne(true,[name])!>
				<#local tempisnotnull=schemaManager.findOne(true,[name])??>
				<#if tempisnotnull>
					<#if !schema.name?? && temp??>
						<#local schema=temp>
					<#elseif schema.name?? && temp??>
						<#local schema=schema.merge(temp)>
					</#if>
				</#if>
			</#list>
			<#else>
			<#list schemaName as name>
				<#local temp=schemaManager.findOne(true,[name])!>
				<#local tempisnotnull=schemaManager.findOne(true,[name])??>
				<#if tempisnotnull>
					<#if !schema.name?? && temp??>
						<#local schema=temp>
						<#break>
					</#if>
				</#if>
			</#list>
			</#if>
		</#if>
	</#if>
	<#if schema?? && schema.fields??>
	<#list schema.fields as field>
		<#if field.name?? && field.name==name && field.type?? && ('SELECT'==field.type.name() || 'CHECKBOX'==field.type.name())>
			<#return field.values>
		</#if>
	</#list>
	</#if>
</#function>