<#macro selectDictionary dictionaryName name="" value="" required=false disabled=false showHeader=true headerKey="" headerValue="" strict=true dynamicAttributes...>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
	<#if dynamicAttributes['dynamicAttributes']??>
		<#local dynamicAttributes+=dynamicAttributes['dynamicAttributes']/>
	</#if>
	<#if !value?has_content&&name?has_content>
	<#local value=stack.findValue(name)!/>
	</#if>
	<select<#if name?has_content><#if disabled> disabled</#if> name="${name}"</#if> class="<#if required && !(dynamicAttributes['class']!)?contains('required')>required </#if><#if !strict>combobox </#if>${dynamicAttributes['class']!}"<@dynAttrs value=dynamicAttributes exclude='class'/>>
		<#if showHeader><option value="${headerKey}">${headerValue}</option></#if>
		<#local exists=false>
		<#if dictionary?? && dictionary.items?? && dictionary.items?size gt 0>
			<#local items = dictionary.items/>
			<#if !dictionary.groupable>
				<#list items as lv>
				<option value="${lv.value}"<#if value=lv.value><#local exists=true> selected="selected"</#if>>${lv.label?has_content?string(lv.label!,lv.value!)}</option>
				</#list>
			<#else>
				<#local group = ""/>
				<#list items as lv>
					<#if !lv.value?has_content>
						<#local label = lv.label/>
						<#if (!label?has_content) && group?has_content>
							<#local group = ""/>
							</optgroup>
						<#else>
							<#if group?has_content>
								</optgroup>
							</#if>
							<#local group = label/>
							<#if group?has_content>
								<optgroup label="${group}">
							</#if>
						</#if>
					<#else>
						<option value="${lv.value}"<#if value=lv.value><#local exists=true> selected="selected"</#if>>${lv.label?has_content?string(lv.label!,lv.value!)}</option>
						<#if group?has_content && !lv?has_next>
						</optgroup>
						</#if>
					</#if>
				</#list>
			</#if>
		</#if>
		<#if !exists && value?has_content>
			<option value="${value}"selected="selected">${value}</option>
		</#if>
	</select>
</#macro>

<#function getDictionaryLabel dictionaryName value="">
	<#return beans['dictionaryControl'].getDictionaryLabel(dictionaryName,value)>
</#function>

<#macro displayDictionaryLabel dictionaryName value="">
${getDictionaryLabel(dictionaryName,value)}<#t>
</#macro>

<#macro checkDictionary dictionaryName name="" value=[] disabled=false dynamicAttributes...>
	<input type="hidden" id="__multiselect_${name!?html}" name="__multiselect_${name?html}" value=""<#rt/><#if disabled> disabled</#if>/>
	<#if !value?has_content&&name?has_content>
	<#local value=stack.findValue(name)!/>
	</#if>
	<#if value?? && !value?is_indexable>
	<#local value=[value]/>
	</#if>
	<#if dynamicAttributes['dynamicAttributes']??>
		<#local dynamicAttributes+=dynamicAttributes['dynamicAttributes']/>
	</#if>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
		<#local index = 0/>
		<#if dictionary?? && dictionary.items?? && dictionary.items?size gt 0>
			<#local items = dictionary.items/>
			<#if !dictionary.groupable>
				<#list items as lv>
				<label for="${name}-${index}" class="checkbox inline"><#t>
				<input id="${name}-${index}" type="checkbox"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> value="${lv.value}" class="custom ${dynamicAttributes['class']!}"<#if statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.value)||statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.label)> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
				</label><#t>
				<#local index++>
				</#list>
			<#else>
				<#local group = ""/>
				<#list items as lv>
					<#if !lv.value?has_content>
						<#local label = lv.label/>
						<#if (!label?has_content) && group?has_content>
							<#local group = ""/>
							</span><#lt>
						<#else>
							<#if group?has_content>
								</span><#lt>
							</#if>
							<#local group = label/>
							<#if group?has_content>
								<span class="checkboxgroup"><label for="${name}-${group}" class="group"><input id="${name}-${group}" type="checkbox" class="checkall custom"/>${group}</label><#t>
							</#if>
						</#if>
					<#else>
						<label for="${name}-${index}" class="checkbox inline"><#t>
						<input id="${name}-${index}" type="checkbox"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> value="${lv.value}" class="custom ${dynamicAttributes['class']!}"<#if statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.value)||statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.label)> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
						</label><#t>
						<#if group?has_content && index==items?size-1>
						</span><#lt>
						</#if>
					</#if>
					<#local index = index+1/>
				</#list>
			</#if>
		</#if>
		<#list value as v>
		<#if !dictionary?? || !(dictionary.itemsAsMap!)?keys?seq_contains(v)>
		<label for="${name}-${index}" class="checkbox inline"><#t>
		<input id="${name}-${index}" type="checkbox"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> class="custom ${dynamicAttributes['class']!}" value="${v}" checked<@dynAttrs value=dynamicAttributes exclude='class'/>/>${v}<#t>
		</label><#t>
		<#local index = index+1/>
		</#if>
		</#list>
</#macro>

<#macro radioDictionary dictionaryName name="" value="" disabled=false dynamicAttributes...>
	<#if dynamicAttributes['dynamicAttributes']??>
		<#local dynamicAttributes+=dynamicAttributes['dynamicAttributes']/>
	</#if>
	<#if !value?has_content&&name?has_content>
	<#local value=stack.findValue(name)!/>
	</#if>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
		<#local index = 0/>
		<#if dictionary?? && dictionary.items?? && dictionary.items?size gt 0>
			<#local items = dictionary.items/>
			<#if !dictionary.groupable>
				<#list items as lv>
				<label for="${name}-${index}" class="radio inline"><#t>
				<input id="${name}-${index}" type="radio"<#if name?has_content> name="${name}"</#if> value="${lv.value}"<#if disabled> disabled</#if> class="custom ${dynamicAttributes['class']!}"<#if value==lv.value> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
				</label><#t>
				<#local index++>
				</#list>
			<#else>
				<#local group = ""/>
				<#list items as lv>
					<#if !lv.value?has_content>
						<#local label = lv.label/>
						<#if (!label?has_content) && group?has_content>
							<#local group = ""/>
							</span><#lt>
						<#else>
							<#if group?has_content>
								</span><#lt>
							</#if>
							<#local group = label/>
							<#if group?has_content>
								<span class="checkgroup"><label for="${name}-${group}" class="group">${group}</label><#t>
							</#if>
						</#if>
					<#else>
						<label for="${name}-${index}" class="radio inline"><#t>
						<input id="${name}-${index}" type="radio"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> class="custom ${dynamicAttributes['class']!}" value="${lv.value}"<#if value==lv.value> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
						</label><#t>
						<#if group?has_content && index==items?size-1>
						</span><#lt>
						</#if>
					</#if>
					<#local index = index+1/>
				</#list>
			</#if>
		</#if>
		<#if !dictionary?? || value?has_content && !(dictionary.itemsAsMap!)?keys?seq_contains(value)>
		<label for="${name}-${index}" class="radio inline"><#t>
		<input id="${name}-${index}" type="radio"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> class="custom ${dynamicAttributes['class']!}" value="${value}"checked<@dynAttrs value=dynamicAttributes exclude='class'/>/>${value}<#t>
		</label><#t>
		<#local index = index+1/>
		</#if>
</#macro>