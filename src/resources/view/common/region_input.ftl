<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if region.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('region')}</title>
</head>
<body>
<@s.form id="region_input" action="${actionBaseUrl}/save" method="post" class="ajax form-horizontal">
	<#if !region.new>
		<@s.hidden name="region.id" />
	</#if>
	<@s.hidden name="parent" />
	<@s.textfield label="%{getText('name')}" name="region.name" class="required"/>
	<@s.textfield label="%{getText('coordinate')}" name="region.coordinate" class="latlng" data\-address="${region.fullname!}"/>
	<@s.textfield label="%{getText('areacode')}" name="region.areacode" maxlength="6"/>
	<@s.textfield label="%{getText('postcode')}" name="region.postcode" maxlength="6"/>
	<@s.textfield label="%{getText('rank')}" name="region.rank" type="number" class="integer positive" min="1"/>
	<@s.textfield label="%{getText('displayOrder')}" name="region.displayOrder" type="number" class="integer"/>
	<@s.submit value="%{getText('save')}" class="btn-primary"/>
</@s.form>
</body>
</html></#escape>


