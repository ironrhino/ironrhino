<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if region.name??>${region.name}-</#if>${action.getText('region')}${action.getText('list')}</title>
</head>
<body>
<#assign columns={"name":{"cellEdit":"click"},"areacode":{"cellEdit":"click","width":"100px"},"postcode":{"cellEdit":"click","width":"100px"},"rank":{"cellEdit":"click","width":"100px"},"displayOrder":{"cellEdit":"click","width":"100px"}}>
<#assign actionColumnButtons=r'
<@btn view="input" label="edit"/>
<a class="btn ajax view" href="${actionBaseUrl+"?parent="+entity.id}<#if tree??>&tree=${tree}</#if>">${action.getText("enter")}</a>
'>
<#assign bottomButtons='
<@btn view="input" label="create"/>
<@btn action="save" confirm=true/>
<@btn action="delete" confirm=true/>
'+r'
<#if region?? && parent??>
<#if region.parent?? && (!tree??||parent!=tree)>
<a class="btn ajax view" href="${actionBaseUrl+"?parent="+region.parent.id}<#if tree??>&tree=${tree}</#if>">${action.getText("upward")}</a>
<#else>
<a class="btn ajax view" href="${actionBaseUrl}<#if tree??>?tree=${tree}</#if>">${action.getText("upward")}</a>
</#if>
</#if>
'+'
<button type="button" class="btn" onclick="$(\'#move\').toggle()">${action.getText("move")}</button>
<button type="button" class="btn" onclick="$(\'#merge\').toggle()">${action.getText("merge")}</button>
'>
<#if region?? && region.id?? && region.id gt 0>
<ul class="breadcrumb">
	<li>
    	<a href="${actionBaseUrl}<#if tree??>?tree=${tree}</#if>" class="ajax view">${action.getText('region')}</a> <span class="divider">/</span>
	</li>
	<#if region.level gt 1>
	<#assign renderItem=(!tree??||tree<1)/>
	<#list 1..region.level-1 as level>
	<#assign ancestor=region.getAncestor(level)>
	<#if !renderItem>
		<#assign renderItem=(ancestor.id==tree!)/>
	</#if>
	<#if renderItem>
	<li>
    	<a href="${actionBaseUrl}?parent=${ancestor.id?string}<#if tree??>&tree=${tree}</#if>" class="ajax view">${ancestor.name}</a> <span class="divider">/</span>
	</li>
	</#if>
	</#list>
	</#if>
	<li class="active">${region.name}</li>
</ul>
</#if>
<@richtable entityName="region" columns=columns actionColumnButtons=actionColumnButtons bottomButtons=bottomButtons/>
<form id="move" action="${actionBaseUrl}/move" method="post" class="ajax reset" style="display:none;" onprepare="return confirm('${action.getText('confirm')}?');" onsuccess="$('#region_form').submit()">
	<div style="padding-top:10px;text-align:center;">
	<input id="regionId1" type="hidden" name="id"/>
	<span class="treeselect" data-options="{'url':'<@url value="/region/children"/>','name':'this','id':'#regionId1','cache':false}"></span>
	--&gt;
	<input id="regionId2" type="hidden" name="id"/>
	<span class="treeselect" data-options="{'url':'<@url value="/region/children"/>','name':'this','id':'#regionId2','cache':false}"></span>
	<@s.submit theme="simple" value="%{getText('confirm')}" />
	</div>
</form>
<form id="merge" action="${actionBaseUrl}/merge" method="post" class="ajax reset" style="display:none;" onprepare="return confirm('${action.getText('confirm')}?');" onsuccess="$('#region_form').submit()">
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
