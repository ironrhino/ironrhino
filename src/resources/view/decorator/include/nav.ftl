<ul class="nav">
	<li><a href="<@url value="/"/>" class="ajax view">${action.getText('index')}</a></li>
	<@authorize ifAnyGranted="ROLE_ADMINISTRATOR">
	<li><a href="<@url value="${ssoServerBase!}/user"/>" class="ajax view">${action.getText('user')}</a></li>
	<li><a href="<@url value="${ssoServerBase!}/loginRecord"/>" class="ajax view">${action.getText('loginRecord')}</a></li>
	<li><a href="<@url value="/common/setting"/>" class="ajax view">${action.getText('setting')}</a></li>
	<li><a href="<@url value="/common/dictionary"/>" class="ajax view">${action.getText('dictionary')}</a></li>
	<li><a href="<@url value="/common/schema"/>" class="ajax view">${action.getText('schema')}</a></li>
	<li><a href="<@url value="/common/page"/>" class="ajax view">${action.getText('page')}</a></li>
	<li><a href="<@url value="/common/region/treeview"/>" class="ajax view">${action.getText('region')}</a></li>
	<li><a href="<@url value="/common/treeNode/treeview"/>" class="ajax view">${action.getText('treeNode')}</a></li>
	<li><a href="<@url value="/common/upload"/>" class="ajax view">${action.getText('upload')}</a></li>
	<li><a href="<@url value="/batch/job"/>" class="ajax view">${action.getText('job')}</a></li>
	<li class="dropdown">
	 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">${action.getText('open.platform')}</a>
	    <ul class="dropdown-menu">
	 		<li><a href="<@url value="/oauth/client"/>" class="ajax view">${action.getText("client")}</a></li>
			<li><a href="<@url value="/oauth/authorization"/>" class="ajax view">${action.getText("authorization")}</a></li>
	 		<li><a href="<@url value="/rest/docs"/>">${action.getText('docs')}</a></li>
	    </ul>
	</li>
	<li class="dropdown">
	 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">${action.getText('tool')}</a>
	    <ul class="dropdown-menu">
		  	<li><a href="<@url value="/common/console"/>">${action.getText('console')}</a></li>
		  	<li><a href="<@url value="/common/query"/>">${action.getText('query')}</a></li>
	  	</ul>
	</li>
  	</@authorize>
</ul>