#!/bin/sh

#must run with sudo
if [ ! -n "$SUDO_USER" ];then
echo please run sudo $0
exit 1
else
USER="$SUDO_USER"
fi

chmod +x /home/$USER

cat>/etc/yum.repos.d/mysql-community.repo<<EOF
[mysql56-community]
name=MySQL 5.6 Community Server
baseurl=http://repo.mysql.com/yum/mysql-5.6-community/el/5/\$basearch/
gpgcheck=0
enabled=1
EOF

cat>/etc/yum.repos.d/nginx.repo<<EOF
[nginx]
name=nginx repo
baseurl=http://nginx.org/packages/centos/\$releasever/\$basearch/
gpgcheck=0
enabled=1
EOF


#install packages
yum -y install mysql-server subversion git nginx chkconfig zip unzip wget make gcc telnet
if [ ! -f "/sbin/insserv" ] ; then
ln -s /usr/lib/insserv/insserv /sbin/insserv
fi

#install oracle jdk
if [ ! -d jdk ];then
wget --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/7u80-b15/jdk-7u80-linux-x64.tar.gz"
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
wget http://archive.apache.org/dist/ant/binaries/apache-ant-1.9.5-bin.tar.gz
tar xf apache-ant-*.tar.gz
rm apache-ant-*.tar.gz
mv apache-ant-* ant
chown -R $USER:$USER ant
ln -s /home/$USER/ant/bin/ant /usr/bin/ant
fi


#install tomcat
if [ ! -d tomcat8080 ];then
if ! $(ls -l apache-tomcat-*.tar.gz >/dev/null 2>&1) ; then
wget http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.62/bin/apache-tomcat-7.0.62.tar.gz
fi
tar xf apache-tomcat-*.tar.gz >/dev/null && rm -rf apache-tomcat-*.tar.gz
mv apache-tomcat-* tomcat
cd tomcat && rm -rf bin/*.bat && rm -rf webapps/*
cd conf
sed -i  's/\s[3-4][a-x-]*manager.org.apache.juli.FileHandler,//g' logging.properties
sed -i '/manager/d' logging.properties
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
cat>server.xml<<EOF
<?xml version="1.0" encoding="utf-8"?>
<Server port="\${port.shutdown}" shutdown="SHUTDOWN">
  <Service name="Catalina">
    <Connector port="\${port.http}" connectionTimeout="20000" URIEncoding="UTF-8" useBodyEncodingForURI="true" bindOnInit="false" server="Ironrhino" maxPostSize="4194304" maxThreads="1000"/>
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false">
      </Host>
    </Engine>
  </Service>
</Server>
EOF
cd ..
cd ..
sed -i '99i export SPRING_PROFILES_DEFAULT' tomcat/bin/catalina.sh
sed -i '99i SPRING_PROFILES_DEFAULT="dual"' tomcat/bin/catalina.sh
sed -i '99i CATALINA_OPTS="-server -Xms128m -Xmx1024m -Xmn80m -Xss256k -XX:PermSize=128m -XX:MaxPermSize=512m -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:+UseParNewGC -XX:CMSMaxAbortablePrecleanTime=5 -Djava.awt.headless=true"' tomcat/bin/catalina.sh
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
chkconfig tomcat8080 on
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
chkconfig tomcat8081 on
fi

if [ ! -f upgrade_tomcat.sh ]; then
cat>upgrade_tomcat.sh<<EOF
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
cd apache-tomcat-\$version && rm -rf bin/*.bat && rm -rf webapps/*
cd conf
sed -i  's/\s[3-4][a-x-]*manager.org.apache.juli.FileHandler,//g' logging.properties
sed -i '/manager/d' logging.properties
cd ..
cd ..
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
cp tomcat8080/bin/catalina.sh .
rm -rf tomcat8080
cp -R apache-tomcat-\$version tomcat8080
mv server.xml tomcat8080/conf/
mv catalina.properties tomcat8080/conf/
mv catalina.sh tomcat8080/bin/
cp -R tomcat8081/webapps* tomcat8080
if [ \$running = 1 ];then
tomcat8080/bin/catalina.sh start
sleep 120
tomcat8081/bin/catalina.sh stop 10 -force
fi
cp tomcat8081/conf/server.xml .
cp tomcat8081/conf/catalina.properties .
cp tomcat8081/bin/catalina.sh .
rm -rf tomcat8081
cp -R apache-tomcat-\$version tomcat8081
mv server.xml tomcat8081/conf/
mv catalina.properties tomcat8081/conf/
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
if [ -f "/etc/my.cnf" ] && ! $(more /etc/my.cnf|grep collation-server >/dev/null 2>&1) ; then
sed -i '22i innodb_stats_on_metadata = off' /etc/my.cnf
sed -i '22i collation-server = utf8_general_ci' /etc/my.cnf
sed -i '22i character-set-server = utf8' /etc/my.cnf
service mysqld restart
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
        location ~ ^/websocket/ {
                 proxy_pass http://backend;
                 proxy_http_version 1.1;
                 proxy_set_header  X-Forwarded-For  \$proxy_add_x_forwarded_for;
                 proxy_set_header  X-Real-IP  \$remote_addr;
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
                 proxy_set_header  Host \$http_host;
                 limit_conn addr   8;
        }
}
EOF
setsebool -P httpd_can_network_connect=1 httpd_read_user_content=1
service nginx restart
fi


#generate deploy.sh
if [ ! -f deploy.sh ]; then
cat>deploy.sh<<EOF
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
echo "\$gitpulloutput"
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
elif [ -d .git ];then
git reset --hard
#git clean -f
gitpulloutput=\`git pull 2>&1\`
echo "\$gitpulloutput"
if [[ \$gitpulloutput =~ fatal: ]] ; then
exit 1
fi
else
echo 'no svn or git'
fi
ant -Dserver.home=/home/$USER/tomcat8080 -Dwebapp.deploy.dir=/home/$USER/tomcat8080/webapps/ROOT deploy
chmod -R +X /home/$USER/tomcat8080/webapps
LANGUAGE=\$OLDLANGUAGE
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
date=\`date +%Y-%m-%d\`
backupdir=/home/$USER/backup/\$date
if test ! -d \$backupdir
then  mkdir -p \$backupdir
fi
cp -r /var/lib/mysql/xiangling \$backupdir
cp -r /home/$USER/web/assets/upload \$backupdir
mysql -u root -D ironrhino -e "optimize table user;"
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


#install redis
if ! which redis-server > /dev/null && ! $(ls -l redis-*.tar.gz >/dev/null 2>&1) ; then
wget http://download.redis.io/releases/redis-3.0.2.tar.gz
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

