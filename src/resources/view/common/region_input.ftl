<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if region.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('region')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/save" method="post" cssClass="ajax form-horizontal">
	<#if !region.new>
		<@s.hidden name="region.id" />
	</#if>
	<@s.hidden name="parent" />
	<@s.textfield label="%{getText('name')}" name="region.name" cssClass="required"/>
	<@s.textfield label="%{getText('coordinate')}" name="region.coordinate.latLngAsString" cssClass="latlng" dynamicAttributes={"data-address":"${region.fullname!}"}/>
	<@s.textfield label="%{getText('areacode')}" name="region.areacode" maxlength="6"/>
	<@s.textfield label="%{getText('postcode')}" name="region.postcode" maxlength="6"/>
	<@s.textfield label="%{getText('rank')}" name="region.rank" type="number" cssClass="integer positive" min="1"/>
	<@s.textfield label="%{getText('displayOrder')}" name="region.displayOrder" type="number" cssClass="integer"/>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html></#escape>


