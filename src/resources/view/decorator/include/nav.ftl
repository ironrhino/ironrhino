<ul class="nav">
  <li><a href="<@url value="/"/>" class="ajax view">${action.getText('index')}</a></li>
  <@authorize ifAnyGranted="ROLE_ADMINISTRATOR">
  <li><a href="<@url value="${ssoServerBase!}/user"/>" class="ajax view">${action.getText('user')}</a></li>
  <li><a href="<@url value="/common/region"/>" class="ajax view">${action.getText('region')}</a></li>
  <li><a href="<@url value="/common/treeNode"/>" class="ajax view">${action.getText('treeNode')}</a></li>
  <li><a href="<@url value="/common/setting"/>" class="ajax view">${action.getText('setting')}</a></li>
  <li><a href="<@url value="/common/dictionary"/>" class="ajax view">${action.getText('dictionary')}</a></li>
  <li><a href="<@url value="/common/schema"/>" class="ajax view">${action.getText('schema')}</a></li>
  <li><a href="<@url value="/common/page"/>" class="ajax view">${action.getText('page')}</a></li>
  <li><a href="<@url value="/common/upload"/>" class="ajax view">${action.getText('upload')}</a></li>
  <li><a href="<@url value="/common/console"/>">${action.getText('console')}</a></li>
  <li><a href="<@url value="/common/query"/>">${action.getText('query')}</a></li>
  </@authorize>
</ul>