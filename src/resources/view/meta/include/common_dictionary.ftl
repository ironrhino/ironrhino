<#macro selectDictionary dictionaryName name="" value="" required=false disabled=false multiple=false showHeader=true headerKey="" headerValue="" strict=true dynamicAttributes...>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
	<#if !value?has_content&&name?has_content>
	<#local value=stack.findValue(name)!/>
	</#if>
	<select<#if name?has_content><#if disabled> disabled</#if> name="${name}"</#if> class="<#if required && !(dynamicAttributes['class']!)?contains('required')>required </#if><#if !strict>combobox </#if>${dynamicAttributes['class']!}"<#if multiple> multiple</#if><@dynAttrs value=dynamicAttributes exclude='class'/>><#lt>
		<#if showHeader><option value="${headerKey}">${headerValue}</option><#lt></#if>
		<#local exists=true>
		<#if (dictionary.items)?has_content>
			<#local items = dictionary.items/>
			<#if !dictionary.groupable>
				<#list items as lv>
				<option value="${lv.value}"<#if value?has_content&&(!value?is_sequence&&value?string==lv.value||value?is_sequence&&value?seq_contains(lv.value))><#local exists=true> selected</#if>>${lv.label?has_content?then(lv.label,lv.value!)}</option><#lt>
				</#list>
			<#else>
				<#local group = ""/>
				<#list items as lv>
					<#if !lv.value?has_content>
						<#local label = lv.label/>
						<#if (!label?has_content) && group?has_content>
							<#local group = ""/>
							</optgroup><#lt>
						<#else>
							<#if group?has_content>
								</optgroup><#lt>
							</#if>
							<#local group = label/>
							<#if group?has_content>
								<optgroup label="${group}"><#lt>
							</#if>
						</#if>
					<#else>
						<option value="${lv.value}"<#if value?has_content&&(!value?is_sequence&&value?string==lv.value||value?is_sequence&&value?seq_contains(lv.value))><#local exists=true> selected</#if>>${lv.label?has_content?then(lv.label,lv.value!)}</option><#lt>
						<#if group?has_content && !lv?has_next>
						</optgroup><#lt>
						</#if>
					</#if>
				</#list>
			</#if>
		</#if>
		<#if !exists&&value?has_content>
		<#if value?is_sequence>
			<#list value as v>
			<option value="${v}" selected>${v}</option><#lt>
			</#list>
		<#else>
			<option value="${value}" selected>${value}</option><#lt>
		</#if>
		</#if>
	</select><#lt>
	<#if !disabled && name?has_content && multiple><input type="hidden" name="__multiselect_${name?html}"></#if>
</#macro>

<#function getDictionaryLabel dictionaryName value="">
	<#return beans['dictionaryControl'].getDictionaryLabel(dictionaryName,value?string)>
</#function>

<#macro displayDictionaryLabel dictionaryName value="">
${getDictionaryLabel(dictionaryName,value?string)}<#t>
</#macro>

<#macro checkDictionary dictionaryName name="" value=[] disabled=false dynamicAttributes...>
	<#if !value?has_content&&name?has_content>
	<#local value=stack.findValue(name)!/>
	</#if>
	<#if value?has_content && !value?is_indexable>
	<#local value=[value]/>
	</#if>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
		<#local index = 0/>
		<#if (dictionary.items)?has_content>
			<#local items = dictionary.items/>
			<#if !dictionary.groupable>
				<#list items as lv>
				<label for="${name}-${index}" class="checkbox inline"><#t>
				<input id="${name}-${index}" type="checkbox"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> value="${lv.value}" class="${dynamicAttributes['class']!}"<#if statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.value)||statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.label)> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?then(lv.label,lv.value!)}<#t>
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
								<span class="checkboxgroup"><label for="${name}-${group}" class="group"><input id="${name}-${group}" type="checkbox" class="checkall"/>${group}</label><#t>
							</#if>
						</#if>
					<#else>
						<label for="${name}-${index}" class="checkbox inline"><#t>
						<input id="${name}-${index}" type="checkbox"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> value="${lv.value}" class="${dynamicAttributes['class']!}"<#if statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.value)||statics['org.apache.struts2.util.ContainUtil'].contains(value,lv.label)> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?then(lv.label,lv.value!)}<#t>
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
		<#if !dictionary?? || !(dictionary.itemsAsMap!)?keys?seq_contains(v?string)>
		<label for="${name}-${index}" class="checkbox inline"><#t>
		<input id="${name}-${index}" type="checkbox"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> class="${dynamicAttributes['class']!}" value="${v}" checked<@dynAttrs value=dynamicAttributes exclude='class'/>/>${v}<#t>
		</label><#t>
		<#local index = index+1/>
		</#if>
		</#list>
		<#if !disabled && name?has_content><input type="hidden" name="__multiselect_${name?html}"/></#if>
</#macro>

<#macro radioDictionary dictionaryName name="" value="" disabled=false dynamicAttributes...>
	<#if !value?has_content&&name?has_content>
	<#local value=stack.findValue(name)!/>
	</#if>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
		<#local index = 0/>
		<#if (dictionary.items)?has_content>
			<#local items = dictionary.items/>
			<#if !dictionary.groupable>
				<#list items as lv>
				<label for="${name}-${index}" class="radio inline"><#t>
				<input id="${name}-${index}" type="radio"<#if name?has_content> name="${name}"</#if> value="${lv.value}"<#if disabled> disabled</#if> class="${dynamicAttributes['class']!}"<#if value?string==lv.value> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?then(lv.label,lv.value!)}<#t>
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
						<input id="${name}-${index}" type="radio"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> class="${dynamicAttributes['class']!}" value="${lv.value}"<#if value?string==lv.value> checked</#if><@dynAttrs value=dynamicAttributes exclude='class'/>/>${lv.label?has_content?then(lv.label,lv.value!)}<#t>
						</label><#t>
						<#if group?has_content && index==items?size-1>
						</span><#lt>
						</#if>
					</#if>
					<#local index = index+1/>
				</#list>
			</#if>
		</#if>
		<#if !dictionary?? || value?has_content && !(dictionary.itemsAsMap!)?keys?seq_contains(value?string)>
		<label for="${name}-${index}" class="radio inline"><#t>
		<input id="${name}-${index}" type="radio"<#if name?has_content> name="${name}"</#if><#if disabled> disabled</#if> class="${dynamicAttributes['class']!}" value="${value}"checked<@dynAttrs value=dynamicAttributes exclude='class'/>/>${value}<#t>
		</label><#t>
		<#local index = index+1/>
		</#if>
		<#if !disabled && name?has_content><input type="hidden" name="__checkbox_${name?html}"></#if>
</#macro>