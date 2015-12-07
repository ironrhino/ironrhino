#!/bin/bash

ANT_VERSION=1.9.6
JDK_VERSION=8u66-b17
TOMCAT_VERSION=8.0.29
REDIS_VERSION=3.0.5
#JDK_VERSION=7u80-b15
#TOMCAT_VERSION=7.0.65

#must run with sudo
if [ ! -n "$SUDO_USER" ];then
echo please run sudo $0
exit 1
else
USER="$SUDO_USER"
fi

cat>>/etc/apt/sources.list<<EOF
deb http://nginx.org/packages/ubuntu/ trusty nginx
deb-src http://nginx.org/packages/ubuntu/ trusty nginx
EOF

#install packages
apt-get update
apt-get --force-yes --yes install mysql-server-5.6 subversion git nginx sysv-rc-conf fontconfig xfonts-utils zip unzip wget iptables make gcc
if [ ! -f "/sbin/insserv" ] ; then
ln -s /usr/lib/insserv/insserv /sbin/insserv
fi


#install simsun font
if [ -f "simsun.ttf" ]; then
mv simsun.ttf /usr/share/fonts/truetype
chmod 644 /usr/share/fonts/truetype/simsun.ttf
cd /usr/share/fonts
mkfontscale
mkfontdir
fc-cache -fv
fi


#install oracle jdk
if [ ! -d jdk ];then
if ! $(ls -l jdk-*linux-x64.tar.gz >/dev/null 2>&1) ; then
wget --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/$JDK_VERSION/jdk-${JDK_VERSION:0:-4}-linux-x64.tar.gz"
fi
tar xf jdk-*.tar.gz
rm jdk-*.tar.gz
mv jdk* jdk
chown -R $USER:$USER jdk
echo JAVA_HOME=\"/home/$USER/jdk\" >> /etc/environment
env JAVA_HOME=\"/home/$USER/jdk\"
ln -s /home/$USER/jdk/bin/java /usr/bin/java
ln -s /home/$USER/jdk/bin/javac /usr/bin/javac
fi


#install ant
if [ ! -d ant ];then
if ! $(ls -l apache-ant-*.tar.gz >/dev/null 2>&1) ; then
wget http://archive.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz
fi
tar xf apache-ant-*.tar.gz
rm apache-ant-*.tar.gz
mv apache-ant-* ant
chown -R $USER:$USER ant
ln -s /home/$USER/ant/bin/ant /usr/bin/ant
fi


#install tomcat
if [ ! -d tomcat8080 ];then
if ! $(ls -l apache-tomcat-*.tar.gz >/dev/null 2>&1) ; then
wget "http://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_VERSION:0:1}/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
fi
tar xf apache-tomcat-*.tar.gz >/dev/null && rm -rf apache-tomcat-*.tar.gz
mv apache-tomcat-* tomcat
cd tomcat && rm -rf bin/*.bat && rm -rf webapps/*
cd conf
sed -i  's/\s[3-4][a-x-]*manager.org.apache.juli.FileHandler,//g' logging.properties
sed -i '/manager/d' logging.properties
if [ "${TOMCAT_VERSION:0:1}" = "7" ];then
sed -i 's/tomcat7-websocket/*/g' catalina.properties
sed -i '/ContextConfig.jarsToSkip/d' catalina.properties
cat>>catalina.properties<<EOF
org.apache.catalina.startup.ContextConfig.jarsToSkip=\\
activiti-*.jar,antlr-*.jar,aopalliance-*.jar,aspectj*.jar,bonecp-*.jar,commons-*.jar,\\
curator-*.jar,dom4j-*.jar,dynamicreports-*.jar,eaxy-*.jar,ehcache-*.jar,\\
elasticsearch-*.jar,freemarker-*.jar,guava-*.jar,hessian-*.jar,hibernate-*.jar,\\
http*.jar,itext*.jar, jackson-*.jar,jasperreports-*.jar,javamail-*.jar,\\
javassist-*.jar,jboss-logging-*.jar,jedis-*.jar, jericho-*.jar,joda-*.jar,jpa-*.jar,\\
jsoup-*.jar,jta-*.jar,log4j-*.jar,lucene-*.jar,mmseg4j-*.jar,\\
mongo-java-driver-*.jar,mvel2-*.jar,mybatis-*.jar,mysql-*.jar,ognl-*.jar,pinyin4j-*.jar,\\
poi-*.jar,rabbitmq-*.jar,sitemesh-*.jar,slf4j-*.jar,spring-*.jar,struts2-*.jar,\\
xmemcached-*.jar,xwork-*.jar,zookeeper-*.jar,zxing-*.jar,\\
ojdbc*.jar,sqljdbc*.jar,postgresql-*.jar,db2*.jar,jconn*.jar,h2-*.jar,hsqldb-*.jar,\\
ifxjdbc*.jar,derbyclient*.jar,rhino*.jar
EOF
else
sed -i '108,$d' catalina.properties
cat>>catalina.properties<<EOF
tomcat.util.scan.StandardJarScanFilter.jarsToSkip=\\
bootstrap.jar,commons-daemon.jar,tomcat-juli.jar,\\
annotations-api.jar,el-api.jar,jsp-api.jar,servlet-api.jar,websocket-api.jar,\\
catalina.jar,catalina-ant.jar,catalina-ha.jar,catalina-storeconfig.jar,\\
catalina-tribes.jar,\\
jasper.jar,jasper-el.jar,ecj-*.jar,\\
tomcat-api.jar,tomcat-util.jar,tomcat-util-scan.jar,tomcat-coyote.jar,\\
tomcat-dbcp.jar,tomcat-jni.jar,tomcat-websocket.jar,\\
tomcat-i18n-en.jar,tomcat-i18n-es.jar,tomcat-i18n-fr.jar,tomcat-i18n-ja.jar,\\
tomcat-juli-adapters.jar,catalina-jmx-remote.jar,catalina-ws.jar,\\
tomcat-jdbc.jar,\\
tools.jar,\\
commons-beanutils*.jar,commons-codec*.jar,commons-collections*.jar,\\
commons-dbcp*.jar,commons-digester*.jar,commons-fileupload*.jar,\\
commons-httpclient*.jar,commons-io*.jar,commons-lang*.jar,commons-logging*.jar,\\
commons-math*.jar,commons-pool*.jar,\\
jstl.jar,taglibs-standard-spec-*.jar,\\
geronimo-spec-jaxrpc*.jar,wsdl4j*.jar,\\
ant.jar,ant-junit*.jar,aspectj*.jar,jmx.jar,h2*.jar,hibernate*.jar,httpclient*.jar,\\
jmx-tools.jar,jta*.jar,log4j*.jar,mail*.jar,slf4j*.jar,\\
xercesImpl.jar,xmlParserAPIs.jar,xml-apis.jar,\\
junit.jar,junit-*.jar,ant-launcher.jar,\\
cobertura-*.jar,asm-*.jar,dom4j-*.jar,icu4j-*.jar,jaxen-*.jar,jdom-*.jar,\\
jetty-*.jar,oro-*.jar,servlet-api-*.jar,tagsoup-*.jar,xmlParserAPIs-*.jar,\\
xom-*.jar,\\
activiti-*.jar,antlr-*.jar,aopalliance-*.jar,aspectj*.jar,bonecp-*.jar,commons-*.jar,\\
curator-*.jar,dom4j-*.jar,dynamicreports-*.jar,eaxy-*.jar,ehcache-*.jar,\\
elasticsearch-*.jar,freemarker-*.jar,guava-*.jar,hessian-*.jar,hibernate-*.jar,\\
http*.jar,itext*.jar, jackson-*.jar,jasperreports-*.jar,javamail-*.jar,\\
javassist-*.jar,jboss-logging-*.jar,jedis-*.jar, jericho-*.jar,joda-*.jar,jpa-*.jar,\\
jsoup-*.jar,jta-*.jar,log4j-*.jar,lucene-*.jar,mmseg4j-*.jar,\\
mongo-java-driver-*.jar,mvel2-*.jar,mybatis-*.jar,mysql-*.jar,ognl-*.jar,pinyin4j-*.jar,\\
poi-*.jar,rabbitmq-*.jar,sitemesh-*.jar,slf4j-*.jar,spring-*.jar,struts2-*.jar,\\
xmemcached-*.jar,xwork-*.jar,zookeeper-*.jar,zxing-*.jar,\\
ojdbc*.jar,sqljdbc*.jar,postgresql-*.jar,db2*.jar,jconn*.jar,h2-*.jar,hsqldb-*.jar,\\
ifxjdbc*.jar,derbyclient*.jar,rhino*.jar
# Default list of JAR files that should be scanned that overrides the default
# jarsToSkip list above. This is typically used to include a specific JAR that
# has been excluded by a broad file name pattern in the jarsToSkip list.
# The list of JARs to scan may be over-ridden at a Context level for individual
# scan types by configuring a JarScanner with a nested JarScanFilter.
tomcat.util.scan.StandardJarScanFilter.jarsToScan=log4j-core*.jar,log4j-taglib*.jar
# String cache configuration.
tomcat.util.buf.StringCache.byte.enabled=true
#tomcat.util.buf.StringCache.char.enabled=true
#tomcat.util.buf.StringCache.trainThreshold=500000
#tomcat.util.buf.StringCache.cacheSize=5000
EOF
fi
cat>server.xml<<EOF
<?xml version="1.0" encoding="utf-8"?>
<Server port="\${port.shutdown}" shutdown="SHUTDOWN">
  <Service name="Catalina">
    <Connector port="\${port.http}" connectionTimeout="20000" URIEncoding="UTF-8" useBodyEncodingForURI="true" bindOnInit="false" server="Ironrhino" maxPostSize="4194304"/>
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false">
      </Host>
    </Engine>
  </Service>
</Server>
EOF
cd ..
cd ..
sed -i '99i export SPRING_PROFILES_DEFAULT=dual' tomcat/bin/catalina.sh
if [ "${JDK_VERSION:0:1}" = "7" ];then
sed -i '99i CATALINA_OPTS="-server -Xms128m -Xmx1024m -Xmn80m -Xss256k -XX:PermSize=128m -XX:MaxPermSize=512m -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:+UseParNewGC -XX:CMSMaxAbortablePrecleanTime=5 -Djava.awt.headless=true"' tomcat/bin/catalina.sh
else
sed -i '99i CATALINA_OPTS="-server -Xms128m -Xmx1024m -Xmn80m -Xss256k -XX:+DisableExplicitGC -XX:+UseG1GC -XX:SurvivorRatio=6 -XX:MaxGCPauseMillis=400 -XX:G1ReservePercent=15 -XX:InitiatingHeapOccupancyPercent=40 -XX:ConcGCThreads=2 -Djava.awt.headless=true"' tomcat/bin/catalina.sh
fi
mv tomcat tomcat8080
cp -R tomcat8080 tomcat8081
sed -i '99i CATALINA_PID="/tmp/tomcat8080_pid"' tomcat8080/bin/catalina.sh
sed -i '99i JAVA_OPTS="-Dport.http=8080 -Dport.shutdown=8005"' tomcat8080/bin/catalina.sh
sed -i '99i CATALINA_PID="/tmp/tomcat8081_pid"' tomcat8081/bin/catalina.sh
sed -i '99i JAVA_OPTS="-Dport.http=8081 -Dport.shutdown=8006"' tomcat8081/bin/catalina.sh
chown -R $USER:$USER tomcat*
fi

if [ ! -f /etc/init.d/tomcat8080 ]; then
cat>/etc/init.d/tomcat8080<<EOF
#!/bin/sh
#
# Startup script for the tomcat
#
# chkconfig: 345 80 15
# description: Tomcat
user=$USER

case "\$1" in
start)
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh start"
       ;;
stop)
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh stop -force"
       ;;
restart)
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh stop -force"
       su \$user -c "/home/$USER/tomcat8080/bin/catalina.sh start"
       ;;
*)
       echo "Usage: \$0 {start|stop|restart}"
esac
exit 0
EOF
chmod +x /etc/init.d/tomcat8080
update-rc.d tomcat8080 defaults
fi

if [ ! -f /etc/init.d/tomcat8081 ]; then
cat>/etc/init.d/tomcat8081<<EOF
#!/bin/sh
#
# Startup script for the tomcat
#
# chkconfig: 345 80 15
# description: Tomcat
user=$USER

case "\$1" in
start)
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh start"
       ;;
stop)
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh stop -force"
       ;;
restart)
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh stop -force"
       su \$user -c "/home/$USER/tomcat8081/bin/catalina.sh start"
       ;;
*)
       echo "Usage: \$0 {start|stop|restart}"
esac
exit 0
EOF
chmod +x /etc/init.d/tomcat8081
update-rc.d tomcat8081 defaults
fi

if [ ! -f upgrade_tomcat.sh ]; then
cat>upgrade_tomcat.sh<<EOF
#!/bin/bash

if [ -n "\$SUDO_USER" ];then
	echo please do not run with sudo
	exit 1
fi
version=\`tomcat8080/bin/version.sh|grep 'Server version'|awk -F '/' '{print \$2}'|tr -d ' '\`
if [ "\$1" = "" ] || [ "\$1" = "-help" ] || [ "\$1" = "--help" ];  then
    echo "current version is \$version, if you want to upgrade, please run \$0 version"
    exit 1
fi
if [  "\$1" = "\`echo -e "\$1\n\$version" | sort -V | head -n1\`" ]; then
   echo "target version \$1 is le than current version \$version"
   exit 1
fi
version="\$1"
if [ ! -d apache-tomcat-\$version ];then
if [ ! -f apache-tomcat-\$version.tar.gz ];then
wget http://archive.apache.org/dist/tomcat/tomcat-\${version:0:1}/v\$version/bin/apache-tomcat-\$version.tar.gz
fi
tar xf apache-tomcat-\$version.tar.gz && rm -rf apache-tomcat-\$version.tar.gz
cd apache-tomcat-\$version && rm -rf bin/*.bat && rm -rf webapps/* && cd ..
fi
running=0
if [ -f /tmp/tomcat8080_pid ] && [ ! "\$( ps -P \`more /tmp/tomcat8080_pid\`|grep tomcat8080)" = "" ] ; then
running=1
fi
if [ \$running = 1 ];then
tomcat8080/bin/catalina.sh stop 10 -force
fi
cp tomcat8080/conf/server.xml .
cp tomcat8080/conf/catalina.properties .
cp tomcat8080/conf/logging.properties .
cp tomcat8080/bin/catalina.sh .
rm -rf tomcat8080
cp -R apache-tomcat-\$version tomcat8080
mv server.xml tomcat8080/conf/
mv catalina.properties tomcat8080/conf/
mv logging.properties tomcat8080/conf/
mv catalina.sh tomcat8080/bin/
cp -R tomcat8081/webapps* tomcat8080
if [ \$running = 1 ];then
tomcat8080/bin/catalina.sh start
sleep 120
tomcat8081/bin/catalina.sh stop 10 -force
fi
cp tomcat8081/conf/server.xml .
cp tomcat8081/conf/catalina.properties .
cp tomcat8081/conf/logging.properties .
cp tomcat8081/bin/catalina.sh .
rm -rf tomcat8081
cp -R apache-tomcat-\$version tomcat8081
mv server.xml tomcat8081/conf/
mv catalina.properties tomcat8081/conf/
mv logging.properties tomcat8081/conf/
mv catalina.sh tomcat8081/bin/
cp -R tomcat8080/webapps* tomcat8081
if [ \$running = 1 ];then
tomcat8081/bin/catalina.sh start
fi
rm -rf apache-tomcat-\$version
EOF
chown $USER:$USER upgrade_tomcat.sh
chmod +x upgrade_tomcat.sh
fi



#config mysql
if [ -f "/etc/mysql/my.cnf" ] && ! $(more /etc/mysql/my.cnf|grep collation-server >/dev/null 2>&1) ; then
sed -i '32i innodb_stats_on_metadata = off' /etc/mysql/my.cnf
sed -i '32i collation-server = utf8_general_ci' /etc/mysql/my.cnf
sed -i '32i character-set-server = utf8' /etc/mysql/my.cnf
service mysql restart
fi


#config nginx
if [ -d /etc/nginx/sites-enabled ]  ; then
ngigxfile="/etc/nginx/sites-enabled/default"
else
ngigxfile="/etc/nginx/conf.d/default.conf"
fi
if [ -f $ngigxfile ] && ! $(more $ngigxfile|grep backend >/dev/null 2>&1) ; then
rm -rf $ngigxfile
fi
if [ ! -f $ngigxfile ]; then
cat>$ngigxfile<<EOF
gzip    on;
gzip_min_length  10240;
gzip_types       text/plain text/xml text/css text/javascript application/javascript application/json application/x-javascript;
limit_conn_zone  \$binary_remote_addr zone=addr:10m;
upstream  backend  {
	server   localhost:8080;
	server   localhost:8081;
}
server {
	listen   80 default_server;
	proxy_pass_header Server;
	client_max_body_size 4m;
	location ~ ^/assets/ {
		root   /home/$USER/tomcat8080/webapps/ROOT;
		expires      max;
		add_header Cache-Control public;
		charset utf-8;
    }
    location ~ ^/remoting/ {
		return     403;
	}
    location ~ ^/websocket/ {
		proxy_pass http://backend;
		proxy_http_version 1.1;
		proxy_set_header  X-Forwarded-For  \$proxy_add_x_forwarded_for;
		proxy_set_header  X-Real-IP  \$remote_addr;
		proxy_set_header  X-Url-Scheme \$scheme;
		proxy_set_header  Host \$http_host;
		proxy_set_header Upgrade \$http_upgrade;
		proxy_set_header Connection "upgrade";
		proxy_read_timeout 1h;
	}
    location  / {
		proxy_pass  http://backend;
		proxy_redirect    off;
		proxy_set_header  X-Forwarded-For  \$proxy_add_x_forwarded_for;
		proxy_set_header  X-Real-IP  \$remote_addr;
		proxy_set_header  X-Url-Scheme \$scheme;
		proxy_set_header  Host \$http_host;
		limit_conn addr   8;
    }
}
EOF
service nginx restart
fi


#generate deploy.sh
if [ ! -f deploy.sh ]; then
cat>deploy.sh<<EOF
#!/bin/bash

#must not run with sudo
if [ -n "\$SUDO_USER" ];then
	echo please do not run with sudo
	exit 1
fi
if [ "\$1" = "" ] || [ "\$1" = "-help" ] || [ "\$1" = "--help" ];  then
    echo "please run \$0 name"
    exit 1
fi
app="\$1"
if [[ "\$app" =~ "/" ]] ; then
app="\${app:0:-1}"
fi
if [[ "\$app" =~ ".war" ]] ; then
if [ ! -f "\$1" ]; then
    echo "file \$1 doesn't exists"
    exit 1
fi
app="\${app:0:-4}"
running=0
if [ -f /tmp/tomcat8080_pid ] && [ ! "\$( ps -P \`more /tmp/tomcat8080_pid\`|grep tomcat8080)" = "" ] ; then
running=1
fi
if [ \$running = 1 ];then
/home/$USER/tomcat8080/bin/catalina.sh stop -force 
fi
if [ -d "/home/$USER/tomcat8080/webapps/\$app" ]; then
rm -rf \$1.bak
cd /home/$USER/tomcat8080/webapps/\$app/
zip -r \$1.bak *  >/dev/null 2>&1
mv \$1.bak /home/$USER
cd
fi
rm -rf /home/$USER/tomcat8080/webapps/\$app
unzip \$1 -d /home/$USER/tomcat8080/webapps/\$app >/dev/null 2>&1
chmod -R +X /home/$USER/tomcat8080/webapps
if [ \$running = 1 ];then
/home/$USER/tomcat8080/bin/catalina.sh start
sleep 60 
/home/$USER/tomcat8081/bin/catalina.sh stop -force 
fi
rm -rf /home/$USER/tomcat8081/webapps/\$app
cp -R /home/$USER/tomcat8080/webapps/\$app /home/$USER/tomcat8081/webapps
if [ \$running = 1 ];then
/home/$USER/tomcat8081/bin/catalina.sh start
fi
else
if [ ! -d "\$1" ]; then
    echo "directory \$1 doesn't exists"
    exit 1
fi
cd ironrhino
OLDLANGUAGE=\$LANGUAGE
LANGUAGE=en
if [ -d .svn ];then
svnupoutput=\`svn up --force\`
echo "\$svnupoutput"
if \$(echo "\$svnupoutput"|grep Updated >/dev/null 2>&1) ; then
ant dist
fi
elif [ -d .git ];then
git reset --hard
git clean -df
gitpulloutput=\`git pull 2>&1\`
ret=\$?
echo "\$gitpulloutput"
if [ \$ret -ne 0 ]; then
        exit \$ret
fi
if ! [[ \$gitpulloutput =~ up-to-date ]] ; then
ant dist
fi
fi
if ! \$(ls -l target/ironrhino*.jar >/dev/null 2>&1) ; then
ant dist
fi
cd ..
cd \$app
if [ -d .svn ];then
svn revert -R .
svn up --force
ret=\$?
elif [ -d .git ];then
git reset --hard
git pull
ret=\$?
else
echo 'no svn or git'
fi
LANGUAGE=\$OLDLANGUAGE
if [ \$ret -ne 0 ]; then
        exit \$ret
fi
ant -Dserver.home=/home/$USER/tomcat8080 -Dwebapp.deploy.dir=/home/$USER/tomcat8080/webapps/ROOT deploy
chmod -R +X /home/$USER/tomcat8080/webapps
sleep 5
ant -Dserver.home=/home/$USER/tomcat8081 -Dserver.shutdown.port=8006 -Dserver.startup.port=8081 shutdown
rm -rf /home/$USER/tomcat8081/webapps
mkdir -p /home/$USER/tomcat8081/webapps
cp -R /home/$USER/tomcat8080/webapps/ROOT /home/$USER/tomcat8081/webapps
ant -Dserver.home=/home/$USER/tomcat8081 -Dserver.shutdown.port=8006 -Dserver.startup.port=8081 startup
fi
EOF
chown $USER:$USER deploy.sh
chmod +x deploy.sh
fi


#generate rollback.sh
if [ ! -f rollback.sh ]; then
cat>rollback.sh<<EOF
#!/bin/bash

if [ -n "\$SUDO_USER" ];then
	echo please do not run with sudo
	exit 1
fi
if [ "\$1" = "" ] || [ "\$1" = "-help" ] || [ "\$1" = "--help" ];  then
    echo "please run \$0 name"
    exit 1
elif [ ! -d "\$1" ]; then
    echo "directory \$1 doesn't exists"
    exit 1
fi
app="\$1"
if [[ "\$app" =~ "/" ]] ; then
app="\${app:0:-1}"
fi
cd \$app
ant -Dserver.home=/home/$USER/tomcat8080 -Dwebapp.deploy.dir=/home/$USER/tomcat8080/webapps/ROOT rollback
ant -Dserver.home=/home/$USER/tomcat8081 -Dwebapp.deploy.dir=/home/$USER/tomcat8081/webapps/ROOT -Dserver.shutdown.port=8006 -Dserver.startup.port=8081 rollback
EOF
chown $USER:$USER rollback.sh
chmod +x rollback.sh
fi


#generate backup.sh
if [ ! -f backup.sh ]; then
cat>backup.sh<<EOF
#!/bin/bash

date=\`date +%Y-%m-%d\`
backupdir=/home/$USER/backup/\$date
if test ! -d \$backupdir
then  mkdir -p \$backupdir
fi
#mysql -u root -D ironrhino -e "optimize table user;"
cp -r /var/lib/mysql \$backupdir
#cp -r /home/$USER/app/assets/upload \$backupdir
olddate=`date +%F -d"-30 days"`
rm -rf /home/$USER/backup/\$olddate*
chown -R $USER:$USER /home/$USER/backup
EOF
chown $USER:$USER backup.sh
chmod +x backup.sh
fi


#generate exportdb.sh and importdb.sh
if [ ! -f exportdb.sh ]; then
cat>exportdb.sh<<EOF
#!/bin/bash

DB_NAME=test
DB_USERNAME=root
DB_PASSWORD=secret
mysqldump --no-data --routines -u \$DB_USERNAME -p\$DB_PASSWORD \$DB_NAME > db-schema.sql
mysqldump --single-transaction --quick --no-autocommit --no-create-info --extended-insert=false -u \$DB_USERNAME -p\$DB_PASSWORD \$DB_NAME > db-data.sql
EOF
cat>importdb.sh<<EOF
DB_NAME=test
DB_USERNAME=root
DB_PASSWORD=secret
mysql -f -u \$DB_USERNAME -p\$DB_PASSWORD \$DB_NAME < db-schema.sql
mysql -f -u \$DB_USERNAME -p\$DB_PASSWORD \$DB_NAME < db-data.sql
EOF
chown $USER:$USER exportdb.sh
chown $USER:$USER importdb.sh
chmod +x exportdb.sh
chmod +x importdb.sh
fi


#iptables
if [ ! -f /etc/init.d/iptables ]; then
cat>/etc/init.d/iptables<<EOF
#!/bin/sh
#
# Startup script for the iptables
#
# chkconfig: 345 80 15
# description: iptables
user=$USER

case "\$1" in
start)
	iptables -A INPUT -s 127.0.0.1 -d 127.0.0.1 -j ACCEPT
	iptables -A INPUT -p tcp --dport 8080 -j DROP
	iptables -A INPUT -p tcp --dport 8081 -j DROP
	iptables -A INPUT -p tcp --dport 8005 -j DROP
	iptables -A INPUT -p tcp --dport 8006 -j DROP
       ;;
stop)
	iptables -F
	iptables -X
	iptables -Z
       ;;
*)
       echo "Usage: \$0 {start|stop}"
esac
exit 0
EOF
chmod +x /etc/init.d/iptables
update-rc.d iptables defaults
service iptables start
fi

#install redis
if ! which redis-server > /dev/null && ! $(ls -l redis-*.tar.gz >/dev/null 2>&1) ; then
wget http://download.redis.io/releases/redis-$REDIS_VERSION.tar.gz
fi
if $(ls -l redis-*.tar.gz >/dev/null 2>&1) ; then
tar xf redis-*.tar.gz >/dev/null && rm -rf redis-*.tar.gz
mv redis-* redis
cd redis && make > /dev/null && make install > /dev/null
cd utils && ./install_server.sh
cd ../../
rm -rf redis
sed -i '31i bind 127.0.0.1' /etc/redis/6379.conf
fi

if [ ! -f upgrade_redis.sh ]; then
cat>upgrade_redis.sh<<EOF
#!/bin/bash

#must run with sudo
if [ ! -n "\$SUDO_USER" ];then
echo please run sudo \$0
exit 1
fi
version=\`redis-cli --version|awk -F ' ' '{print \$2}'|tr -d ' '\`
if [ "\$1" = "" ] || [ "\$1" = "-help" ] || [ "\$1" = "--help" ];  then
    echo "current version is \$version, if you want to upgrade, please run \$0 version"
    exit 1
fi
if [  "\$1" = "\`echo -e "\$1\n\$version" | sort -V | head -n1\`" ]; then
   echo "target version \$1 is le than current version \$version"
   exit 1
fi
version="\$1"
if [ ! -d redis-\$version ];then
if [ ! -f redis-\$version.tar.gz ];then
wget http://download.redis.io/releases/redis-\$version.tar.gz
fi
tar xf redis-\$version.tar.gz && rm -rf redis-\$version.tar.gz
fi
cd redis-\$version && make > /dev/null && make install > /dev/null
cd utils && ./install_server.sh
cd ../../
rm -rf redis-\$version
sed -i '31i bind 127.0.0.1' /etc/redis/6379.conf
service redis_6379 stop
service redis_6379 start
rm -rf redis-\$version
EOF
chown $USER:$USER upgrade_redis.sh
chmod +x upgrade_redis.sh
fi


#git clone ironrhino
if [ ! -d ironrhino ];then
git clone --depth 1 https://github.com/ironrhino/ironrhino.git
chown -R $USER:$USER ironrhino
fi

#ulimit
if $(more /etc/pam.d/su |grep pam_limits.so|grep "#" >/dev/null 2>&1); then
sed -i '/pam_limits/d' /etc/pam.d/su 
sed -i '53i session    required   pam_limits.so' /etc/pam.d/su
sed -i '$i *               soft    nofile          65535' /etc/security/limits.conf
sed -i '$i *               hard    nofile          65535' /etc/security/limits.conf
reboot
fi

