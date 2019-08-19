<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('error.occur')}</title>
<meta name="decorator" content="none"/>
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
				text-align: center;
				text-shadow: 5px 5px hsl(0, 0%, 25%);
				float: left;
			}
			.error-occur{
				width: 47%;
				float: right;
				font-size: 30px;
				color: white;
				text-shadow: 2px 2px 5px hsl(0, 0%, 61%);
				padding-top: 70px;
				padding-right: 20px;
				word-break: break-all;				
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
				padding: 20px;
				white-space: pre-wrap;
				word-wrap: break-word;
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

<p class="error-code">500</p>
<p class="error-occur">${getText('error.occur')}</p>
<div class="clear"></div>
<div class="content">
	<#if exception??>
	<pre>	${statics['org.ironrhino.core.util.ExceptionUtils'].getStackTraceAsString(exception)!}</pre>
	</br>
	</#if>
	<a href="javascript:history.back();">${getText('back')}</a>
	<a href="<@url value="/"/>">${getText('index')}</a>
</div>
</body>
</html>
