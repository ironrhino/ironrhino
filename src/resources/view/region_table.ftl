<#ftl output_format='HTML'>
<#macro renderTR region>
<tr id="node-${region.id}"<#if region.parent??&&region.parent.id gt 0> class="child-of-node-${region.parent.id}"</#if>>
        <td><input type="checkbox" name="id" value="${region.id}"/></td>
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
<html>
<head>
<title>region table</title>
</head>
<body>
<table class="treeTable checkboxgroup" style="width:100%;">
  <thead>
    <tr>
      <th style="width:10%"><input type="checkbox" class="checkall"/></th>
      <th style="width:20%;">${getText('name')}</th>
      <th style="width:70%;">${getText('fullname')}</th>
    </tr>
  </thead>
  <tbody>
    <#list regionTree.children as var>
      <@renderTR var/>
    </#list>
  </tbody>
</table>
</body>
</html>