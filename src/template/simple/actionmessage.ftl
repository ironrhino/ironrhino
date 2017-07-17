<#if (actionMessages?has_content && !parameters.isEmptyList)>
	<#list actionMessages as message>
        <#if message?has_content>
            <div class="action-message alert alert-info"><a class="close" data-dismiss="alert">&times;</a>${message!}</div>
        </#if>
	</#list>
</#if>
<#if actionSuccessMessage?has_content>
     <div class="action-message alert alert-success"><a class="close" data-dismiss="alert">&times;</a>${actionSuccessMessage}</div>
</#if>
<#if actionWarning?has_content>
     <div class="action-message alert alert-warning"><a class="close" data-dismiss="alert">&times;</a>${actionWarning}</div>
</#if>