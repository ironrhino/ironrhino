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
	 		<li><a href="<@url value="/rest/docs"/>" class="ajax view">${getText('docs')}</a></li>
		</ul>
	</li>
	<li class="dropdown">
	 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">${getText('tool')}</a>
		<ul class="dropdown-menu">
			<li><a href="<@url value="/common/console"/>">${getText('console')}</a></li>
			<li><a href="<@url value="/common/query"/>">${getText('query')}</a></li>
		</ul>
	</li>
	<li class="dropdown">
	 	<a href="#" class="dropdown-toggle" data-toggle="dropdown">${getText('sample')}</a>
		<ul class="dropdown-menu">
			<li><a href="<@url value="/sample/customer"/>" class="ajax view">${getText('customer')}</a></li>
			<li><a href="<@url value="/sample/company"/>" class="ajax view">${getText('company')}</a></li>
			<li><a href="<@url value="/sample/boss"/>" class="ajax view">${getText('boss')}</a></li>
			<li><a href="<@url value="/sample/employee"/>" class="ajax view">${getText('employee')}</a></li>
			<li><a href="<@url value="/sample/task"/>" class="ajax view">${getText('task')}</a></li>
			<li><a href="<@url value="/sample/message"/>" class="ajax view">${getText('message')}</a></li>
			<li><a href="<@url value="/sample/person"/>" class="ajax view">${getText('person')}</a></li>
		</ul>
	</li>
	</@authorize>
	<@authorize ifAnyGranted="ROLE_AUDITOR">
	<li><a href="<@url value="${ssoServerBase!}/auditEvent"/>" class="ajax view">${getText('auditEvent')}</a></li>
	</@authorize>
</ul>
