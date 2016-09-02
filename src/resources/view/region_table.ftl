<#macro renderTR region>
<tr id="node-${region.id}"<#if region.parent??&&region.parent.id gt 0> class="child-of-node-${region.parent.id}"</#if>>
        <td><input type="checkbox" class="custom" name="id" value="${region.id}"/></td>
        <td>${region.name}</td>
        <td>${region.fullname}</td>
</tr>
<#if region.leaf>
	<#return>
<#else>
<#list region.children as var>
	<@renderTR var/>
</#list>
</#if>
</#macro>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>region table</title>
</head>
<body>
<table class="treeTable checkboxgroup" style="width:100%;">
  <thead>
    <tr>
      <th style="width:10%"><input type="checkbox" class="checkall custom"/></th>
      <th style="width:20%;">${action.getText('name')}</th>
      <th style="width:70%;">${action.getText('fullname')}</th>
    </tr>
  </thead>
  <tbody>
    <#list regionTree.children as var>
      <@renderTR var/>
    </#list>
  </tbody>
</table>
</body>
</html></#escape>