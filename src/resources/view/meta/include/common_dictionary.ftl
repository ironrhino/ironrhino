<#macro selectDictionary dictionaryName name="" value="" required=false disabled=false headerKey="" headerValue="" strict=true dynamicAttributes...>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
	<#if dynamicAttributes['dynamicAttributes']??>
		<#local dynamicAttributes=dynamicAttributes+dynamicAttributes['dynamicAttributes']/>
	</#if>
	<#if !value?has_content&&name?has_content>
	<#local value=stack.findValue(name)!/>
	</#if>
	<select<#if name?has_content><#if disabled> disabled</#if> name="${name}"</#if> class="<#if required && !(dynamicAttributes['class']!)?contains('required')>required </#if><#if !strict>combobox </#if>${dynamicAttributes['class']!}"<#list dynamicAttributes?keys as attr><#if attr!='class' && attr!='dynamicAttributes'&&attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>>
		<option value="${headerKey}">${headerValue}</option>
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
						<#if group?has_content && !lv_has_next>
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

<#macro checkDictionary dictionaryName value=[] dynamicAttributes...>
	<#if value?? && !value?is_sequence>
	<#local value=[value]/>
	</#if>
	<#if dynamicAttributes['dynamicAttributes']??>
		<#local dynamicAttributes=dynamicAttributes+dynamicAttributes['dynamicAttributes']/>
	</#if>
	<#local dictionary=beans['dictionaryControl'].getDictionary(dictionaryName)!>
		<#local index = 0/>
		<#if dictionary?? && dictionary.items?? && dictionary.items?size gt 0>
			<#local items = dictionary.items/>
			<#if !dictionary.groupable>
				<#list items as lv>
				<label for="${dictionaryName}-${index}" class="checkbox inline"><#t>
				<input id="${dictionaryName}-${index}" type="checkbox" value="${lv.value}" class="custom ${dynamicAttributes['class']!}"<#if value?seq_contains(lv.value)> checked="checked"</#if><#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'&&attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
				</label><#t>
				<#local index=index+1>
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
								<span class="checkboxgroup"><label for="${dictionaryName}-${group}" class="group"><input id="${dictionaryName}-${group}" type="checkbox" class="custom"/>${group}</label><#t>
							</#if>
						</#if>
					<#else>
						<label for="${dictionaryName}-${index}" class="checkbox inline"><#t>
						<input id="${dictionaryName}-${index}" type="checkbox" class="custom ${dynamicAttributes['class']!}" value="${lv.value}"<#if value?seq_contains(lv.value)> checked="checked"</#if><#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'&&attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
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
		<label for="${dictionaryName}-${index}" class="checkbox inline"><#t>
		<input id="${dictionaryName}-${index}" type="checkbox" class="custom ${dynamicAttributes['class']!}" value="${v}"checked="checked"<#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'&&attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>/>${v}<#t>
		</label><#t>
		<#local index = index+1/>
		</#if>
		</#list>
</#macro>

<#macro radioDictionary dictionaryName name="" value="" dynamicAttributes...>
	<#if dynamicAttributes['dynamicAttributes']??>
		<#local dynamicAttributes=dynamicAttributes+dynamicAttributes['dynamicAttributes']/>
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
				<label for="${dictionaryName}-${index}" class="radio inline"><#t>
				<input id="${dictionaryName}-${index}" type="radio"<#if name?has_content> name="${name}"</#if> value="${lv.value}" class="custom ${dynamicAttributes['class']!}"<#if value==lv.value> checked="checked"</#if><#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'&&attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
				</label><#t>
				<#local index=index+1>
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
								<span class="checkgroup"><label for="${dictionaryName}-${group}" class="group">${group}</label><#t>
							</#if>
						</#if>
					<#else>
						<label for="${dictionaryName}-${index}" class="radio inline"><#t>
						<input id="${dictionaryName}-${index}" type="radio"<#if name?has_content> name="${name}"</#if> class="custom ${dynamicAttributes['class']!}" value="${lv.value}"<#if value==lv.value> checked="checked"</#if><#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'&&attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>/>${lv.label?has_content?string(lv.label!,lv.value!)}<#t>
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
		<label for="${dictionaryName}-${index}" class="radio inline"><#t>
		<input id="${dictionaryName}-${index}" type="radio"<#if name?has_content> name="${name}"</#if> class="custom ${dynamicAttributes['class']!}" value="${value}"checked="checked"<#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'&&attr!='class'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>/>${value}<#t>
		</label><#t>
		<#local index = index+1/>
		</#if>
</#macro>