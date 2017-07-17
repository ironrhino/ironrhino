<#if actionErrors?has_content>
	<#list actionErrors as error>
		<#if error?has_content>
            <div class="action-error alert alert-error"><a class="close" data-dismiss="alert">&times;</a>${error!}</div>
        </#if>
	</#list>
</#if>