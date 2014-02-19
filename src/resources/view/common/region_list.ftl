<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if region.name??>${region.name}-</#if>${action.getText('region')}${action.getText('list')}</title>
</head>
<body>
<#assign columns={"name":{"cellEdit":"click"},"areacode":{"cellEdit":"click","width":"100px"},"postcode":{"cellEdit":"click","width":"100px"},"rank":{"cellEdit":"click","width":"100px"},"displayOrder":{"cellEdit":"click","width":"100px"}}>
<#assign actionColumnButtons=r'
<button type="button" class="btn" data-view="input">${action.getText("edit")}</button>
<a class="btn ajax view" href="${actionBaseUrl+"?parent="+entity.id}">${action.getText("enter")}</a>
'>
<#assign bottomButtons='
<button type="button" class="btn" data-view="input">${action.getText("create")}</button>
<button type="button" class="btn confirm" data-action="save">${action.getText("save")}</button>
<button type="button" class="btn" data-action="delete" data-shown="selected">${action.getText("delete")}</button>
'+r'
<#if region?? && parent??>
<#if region.parent??>
<a class="btn ajax view" href="${actionBaseUrl+"?parent="+region.parent.id}">${action.getText("upward")}</a>
<#else>
<a class="btn ajax view" href="${actionBaseUrl}">${action.getText("upward")}</a>
</#if>
</#if>
'+'
<button type="button" class="btn" onclick="$(\'#move\').toggle()">${action.getText("move")}</button>
<button type="button" class="btn" onclick="$(\'#merge\').toggle()">${action.getText("merge")}</button>
'>
<#if region?? && region.id?? && region.id gt 0>
<ul class="breadcrumb">
	<li>
    	<a href="${actionBaseUrl}" class="ajax view">${action.getText('region')}</a> <span class="divider">/</span>
	</li>
	<#if region.level gt 1>
	<#list 1..region.level-1 as level>
	<#assign ancestor=region.getAncestor(level)>
	<li>
    	<a href="${actionBaseUrl}?parent=${ancestor.id?string}" class="ajax view">${ancestor.name}</a> <span class="divider">/</span>
	</li>
	</#list>
	</#if>
	<li class="active">${region.name}</li>
</ul>
</#if>
<@richtable entityName="region" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons/>
<form id="move" action="region/move" method="post" class="ajax reset" style="display:none;" onprepare="return confirm('${action.getText('confirm')}?');" onsuccess="$('#region_form').submit()">
	<div style="padding-top:10px;text-align:center;">
	<input id="regionId1" type="hidden" name="id"/>
	<span class="treeselect" data-options="{'url':'<@url value="/region/children"/>','name':'this','id':'#regionId1','cache':false}"></span>
	--&gt;
	<input id="regionId2" type="hidden" name="id"/>
	<span class="treeselect" data-options="{'url':'<@url value="/region/children"/>','name':'this','id':'#regionId2','cache':false}"></span>
	<@s.submit theme="simple" value="%{getText('confirm')}" />
	</div>
</form>
<form id="merge" action="region/merge" method="post" class="ajax reset" style="display:none;" onprepare="return confirm('${action.getText('confirm')}?');" onsuccess="$('#region_form').submit()">
	<div style="padding-top:10px;text-align:center;">
	<input id="regionId3" type="hidden" name="id"/>
	<span class="treeselect" data-options="{'url':'<@url value="/region/children"/>','name':'this','id':'#regionId3','cache':false}"></span>
	--&gt;
	<input id="regionId4" type="hidden" name="id"/>
	<span class="treeselect" data-options="{'url':'<@url value="/region/children"/>','name':'this','id':'#regionId4','cache':false}"></span>
	<@s.submit theme="simple" value="%{getText('confirm')}" />
	</div>
</form>
</body>
</html></#escape>
