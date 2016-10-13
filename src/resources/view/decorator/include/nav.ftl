<ul class="nav">
	<li><a href="<@url value="/"/>" class="ajax view">${getText('index')}</a></li>
	<@authorize ifAnyGranted="ROLE_ADMINISTRATOR">
	<li><a href="<@url value="${ssoServerBase!}/user"/>" class="ajax view">${getText('user')}</a></li>
	<li><a href="<@url value="${ssoServerBase!}/loginRecord"/>" class="ajax view">${getText('loginRecord')}</a></li>
	<li><a href="<@url value="/common/setting"/>" class="ajax view">${getText('setting')}</a></li>
	<li><a href="<@url value="/common/dictionary"/>" class="ajax view">${getText('dictionary')}</a></li>
	<li><a href="<@url value="/common/schema"/>" class="ajax view">${getText('schema')}</a></li>
	<li><a href="<@url value="/common/page"/>" class="ajax view">${getText('page')}</a></li>
	<li><a href="<@url value="/common/region/treeview"/>" class="ajax view">${getText('region')}</a></li>
	<li><a href="<@url value="/common/treeNode/treeview"/>" class="ajax view">${getText('treeNode')}</a></li>
	<li><a href="<@url value="/common/upload"/>" class="ajax view">${getText('upload')}</a></li>
	<li><a href="<@url value="/batch/job"/>" class="ajax view">${getText('job')}</a></li>
	<li class="dropdown">
	 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">${getText('open.platform')}</a>
	    <ul class="dropdown-menu">
	 		<li><a href="<@url value="/oauth/client"/>" class="ajax view">${getText("client")}</a></li>
			<li><a href="<@url value="/oauth/authorization"/>" class="ajax view">${getText("authorization")}</a></li>
	 		<li><a href="<@url value="/rest/docs"/>">${getText('docs')}</a></li>
	    </ul>
	</li>
	<li class="dropdown">
	 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">${getText('tool')}</a>
	    <ul class="dropdown-menu">
		  	<li><a href="<@url value="/common/console"/>">${getText('console')}</a></li>
		  	<li><a href="<@url value="/common/query"/>">${getText('query')}</a></li>
	  	</ul>
	</li>
  	</@authorize>
</ul>