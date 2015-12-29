#新手入门

[TOC]
## 准备所需软件
- **JDK** : 最低版本要求1.7推荐使用1.8, 如使用1.7请使用[1.x](https://github.com/ironrhino/ironrhino/tree/1.x)分支
- **Tomcat** : 最低版本要求7.0推荐使用8.0, 其他实现了Servlet3.0规范的应用服务器也可以
- **MySQL** : 最低版本要求5.0推荐使用5.7, 若使用其他数据库则需自行引入相应的数据库JDBC驱动
- **Ant** :  ironhino用ant作为构建工具, 并未采用maven或gradle
- **Git** : 需要从github上拉取[ironrhino](https://github.com/ironrhino/ironrhino.git)
-  **Eclipse** :  不用IDE或者用其他IDE的请略过, 不推荐 for Java EE 版本, for Java 版本即可, 推荐安装插件 Properties Editor 和 FreeMarker IDE
-  **Chrome** : 或其他非IE浏览器, IE需要9.0+

##设置环境参数
1. 设置环境变量 **JAVA_HOME** **ANT_HOME** **PATH**等
2. 设置MySQL的配置文件修改默认字符集为UTF8以支持中文 `
character-set-server = utf8
collation-server = utf8_general_ci
`确保root用户密码为空否则需要在自己的applicationContext.properties文件里面指定用户名和密码, 为了安全请不要在生产环境修改密码为空
3. 删除tomcat的webapps目录下所有文件, 修改bin/catalina.bat设置JVM参数, 修改bin/startup.bat , 在最后一行的 start 前面增加 jpda  ,这样以开启远程调试方式启动, 为了性能请不要在生产环境开启远程调试功能
4. 设置环境变量 **STAGE=DEVELOPMENT** 此设置可以让很多情况不需重启即可看到效果, 为了性能请不要在生产环境设置

## 运行演示工程
1. 在eclipse的workspace目录里面执行 `git clone --depth 1 https://github.com/ironrhino/ironrhino.git`
2. 将ironrhino工程导入eclipse刷新工程确保无编译错误
3. 在tomcat的conf目录下增加一个xml文件, 路径为conf/Catalina/localhost/ROOT.xml  ```
<Context docBase="D:/workspace/ironrhino/webapp" reloadable="false"/>
```路径请根据自己的真实情况修改, 然后运行bin/startup.bat 启动tomcat
4. 浏览器访问 http://localhost:8080/setup , 设置系统管理员用户名和密码

## 创建自有工程
1. 在ironrhino工程目录下运行 `ant create -Dproject.name=demo` , 这样会在同一个workspace下面创建一个demo工程,将新创建的工程导入到eclipse
2. 进入到demo工程目录下执行 `ant sync`, 刷新eclipse里面的工程确保无编译错误
3. 修改tomcat的ROOT.xml, 将docBase指向demo工程后启动tomcat
4. 浏览器访问 http://localhost:8080/setup , 设置系统管理员用户名和密码