<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('error.occur')}</title>
<style>
			*{
				margin:0;
				padding:0;
			}
			body{
				background-color:#212121;
				color:white;
				font-size: 18px;
				padding-bottom:20px;
			}
			.error-code{
				font-size: 200px;
				color: white;
				color: rgba(255, 255, 255, 0.98);
				width: 50%;
				text-align: right;
				margin-top: 5%;
				text-shadow: 5px 5px hsl(0, 0%, 25%);
				float: left;
			}
			.error-occur{
				width: 47%;
				float: right;
				margin-top: 5%;
				font-size: 50px;
				color: white;
				text-shadow: 2px 2px 5px hsl(0, 0%, 61%);
				padding-top: 70px;
			}
			.clear{
				float:none;
				clear:both;
			}
			.content{
				text-align:center;
				line-height: 30px;
			}
			pre{
				text-align:left;
			}
			input[type=text]{
				border: hsl(247, 89%, 72%) solid 1px;
				outline: none;
				padding: 5px 3px;
				font-size: 16px;
				border-radius: 8px;
			}
			a{
				text-decoration: none;
				color: #9ECDFF;
				text-shadow: 0px 0px 2px white;
			}
			a:hover{
				color:white;
			}
</style>
</head>
<body>
<#assign exception=request.getAttribute('javax.servlet.error.exception')!>
<p class="error-code">500</p>
<p class="error-occur">${action.getText('error.occur')}</p>
<div class="clear"></div>
<div class="content">
	<#if exception?has_content>
	<pre>	${statics['org.ironrhino.core.util.ExceptionUtils'].getStackTraceAsString(exception)!}</pre>
	</br>
	</#if>
	<a href="javascript:history.back();">${action.getText('back')}</a>
	<a href="<@url value="/"/>">${action.getText('index')}</a>
</div>
</body>
</html></#escape>


