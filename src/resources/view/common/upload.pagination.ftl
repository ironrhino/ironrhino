<#ftl output_format='HTML'>
<#if pagedFiles?? && (marker?has_content || pagedFiles.nextMarker??)>
<#assign view=.main_template_name?keep_after_last('_')?keep_before('.')/>
<#assign baseurl= actionBaseUrl+'/'+view+folderEncoded/>
<#if limit??><#assign baseurl+=('?limit='+limit)/></#if>
<tr>
	<td colspan="4" class="center">
	<#if marker?has_content>
	<#assign _url=baseurl>
	<#if previousMarker?has_content>
		<#assign _url+=_url?contains('?')?then('&','?')/>
		<#if previousMarker?contains(',')>
			<#assign _url+='m='+previousMarker?keep_after_last(',')?url+'&pm='+previousMarker?keep_before_last(',')?url/>
		<#else>
			<#assign _url+='m='+previousMarker?url/>
		</#if>
	</#if>
	<a class="ajax view" data-replacement="files" href="${_url}">${getText(previousMarker?has_content?then('previouspage','firstpage'))}</a>
	<#else>
	<span>${getText('firstpage')}</span>
	</#if>
	<#if pagedFiles.nextMarker?has_content>
	<#assign _url=baseurl+baseurl?contains('?')?then('&','?')+'m='+pagedFiles.nextMarker?url/>
	<#if pagedFiles.marker?has_content>
		<#assign history=previousMarker?has_content?then(previousMarker+','+pagedFiles.marker,pagedFiles.marker)?split(',')/>
		<#assign maxlength=5>
		<#if history?size gt maxlength><#assign history=history[history?size-maxlength..]></#if>
		<#assign _url+=('&pm='+history?join(',')?url)/>
	</#if>
	<a class="ajax view" data-replacement="files" href="${_url}">${getText('nextpage')}</a>
	<#else>
	<span>${getText('nextpage')}</span>
	</#if>
	</td>
</tr>
</#if>