#!/bin/bash

ANT_VERSION=1.10.5
TOMCAT_VERSION=9.0.11

#must run with sudo
#useradd app -m -s /bin/bash && su - app
if [ ! -n "$SUDO_USER" ];then
echo please run sudo $0
exit 1
else
USER="$SUDO_USER"
fi

#using aliyun as apt mirror
cp /etc/apt/sources.list /etc/apt/sources.list.bak
sed -i "s/archive\.ubuntu\.com/mirrors.aliyun.com/g" /etc/apt/sources.list

#add nginx
cat>>/etc/apt/sources.list<<EOF
deb http://nginx.org/packages/ubuntu/ trusty nginx
deb-src http://nginx.org/packages/ubuntu/ trusty nginx
EOF

#install packages
apt-get update
apt-get --force-yes --yes install openjdk-8-jdk mysql-server-5.7 subversion git nginx sysv-rc-conf fontconfig xfonts-utils zip unzip wget iptables make gcc
if [ ! -f "/sbin/insserv" ] ; then
ln -s /usr/lib/insserv/insserv /sbin/insserv
fi
grep "password" /var/log/mysqld.log


#install simsun font
if [ -f "simsun.ttf" ]; then
mv simsun.ttf /usr/share/fonts/truetype
chmod 644 /usr/share/fonts/truetype/simsun.ttf
cd /usr/share/fonts
mkfontscale
mkfontdir
fc-cache -fv
fi


#install ant
if [ ! -f /usr/bin/ant ];then
if ! $(ls -l apache-ant-*.tar.gz >/dev/null 2>&1) ; then
wget http://mirrors.aliyun.com/apache/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz
if [ $? -ne 0 ]; then
   http://archive.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz
fi
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
wget http://mirrors.aliyun.com/apache/tomcat/tomcat-${TOMCAT_VERSION:0:1}/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
if [ $? -ne 0 ]; then
   http://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_VERSION:0:1}/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
fi
fi
tar xf apache-tomcat-*.tar.gz >/dev/null && rm -rf apache-tomcat-*.tar.gz
mv apache-tomcat-* tomcat
cd tomcat && rm -rf bin/*.bat && rm -rf webapps/*
cd conf
sed -i 's/\s[3-4][a-x-]*manager\.org\.apache\.juli\.\(Async\)\?FileHandler,//g' logging.properties
sed -i '/manager/d' logging.properties
cat>>catalina.properties<<EOF
tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*.jar
tomcat.util.scan.StandardJarScanFilter.jarsToScan=ironrhino-*.jar
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
sed -i '99i export SPRING_PROFILES_DEFAULT=dual' tomcat/bin/catalina.sh
sed -i '99i CATALINA_OPTS="-server -Xms128m -Xmx1024m -Xmn80m -Xss256k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=10 -XX:ConcGCThreads=2 -XX:InitiatingHeapOccupancyPercent=70 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=../logs/dumps -Djava.awt.headless=true -Djava.security.egd=file:/dev/urandom"' tomcat/bin/catalina.sh
mv tomcat tomcat8080
cp -R tomcat8080 tomcat8081
sed -i '99i CATALINA_PID="/tmp/tomcat8080_pid"' tomcat8080/bin/catalina.sh
sed -i '99i JAVA_OPTS="-Dport.http=8080 -Dport.shutdown=8005"' tomcat8080/bin/catalina.sh
sed -i '99i CATALINA_PID="/tmp/tomcat8081_pid"' tomcat8081/bin/catalina.sh
sed -i '99i JAVA_OPTS="-Dport.http=8081 -Dport.shutdown=8006"' tomcat8081/bin/catalina.sh
chown -R $USER:$USER tomcat*
fi

if [ ! -f /etc/systemd/system/tomcat8080.service ]; then
cat>/etc/systemd/system/tomcat8080.service<<EOF
[Unit]
Description=Tomcat 8080 service
After=network.target

[Service]
Type=forking
User=$USER
ExecStart=/home/$USER/tomcat8080/bin/catalina.sh start
ExecStop=/home/$USER/tomcat8080/bin/catalina.sh stop -force

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable tomcat8080
fi

if [ ! -f /etc/systemd/system/tomcat8081.service ]; then
cat>/etc/systemd/system/tomcat8081.service<<EOF
[Unit]
Description=Tomcat 8081 service
After=network.target

[Service]
Type=forking
User=$USER
ExecStart=/home/$USER/tomcat8081/bin/catalina.sh start
ExecStop=/home/$USER/tomcat8081/bin/catalina.sh stop -force

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable tomcat8081
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
wget http://mirrors.aliyun.com/apache/tomcat/tomcat-\${version:0:1}/v\$version/bin/apache-tomcat-\$version.tar.gz
if [ $? -ne 0 ]; then
   wget http://archive.apache.org/dist/tomcat/tomcat-\${version:0:1}/v\$version/bin/apache-tomcat-\$version.tar.gz
fi
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
if [ -f "/etc/mysql/mysql.conf.d/mysqld.cnf" ] && ! $(more /etc/mysql/mysql.conf.d/mysqld.cnf|grep collation-server >/dev/null 2>&1) ; then
sed -i '/\[mysqld\]/a\lower_case_table_names = 1' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\innodb_stats_on_metadata = off' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\collation-server = utf8mb4_unicode_ci' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\character-set-server = utf8mb4' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\open_files_limit = 8192' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\max_connections = 1000' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\innodb_buffer_pool_size = 1G' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\key_buffer_size = 384M' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\sort_buffer_size = 4M' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\read_buffer_size = 1M' /etc/mysql/mysql.conf.d/mysqld.cnf
sed -i '/\[mysqld\]/a\table_open_cache = 2000' /etc/my.cnf
systemctl restart mysqld
fi


#config nginx
if ! $(more /etc/nginx/nginx.conf |grep worker_rlimit_nofile >/dev/null 2>&1); then
        sed -i '3i worker_rlimit_nofile 65535;' /etc/nginx/nginx.conf
fi
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
gzip	on;
gzip_min_length	10240;
gzip_types	text/plain text/xml text/css text/javascript application/javascript application/json application/x-javascript;
limit_conn_zone	\$binary_remote_addr zone=addr:10m;
upstream backend {
	server	localhost:8080;
	server	localhost:8081;
	keepalive 50;
}
server {
	listen	80 default_server;
	proxy_pass_header Server;
	client_max_body_size 4m;
	location /stub_status {
		stub_status;
	}
	location /assets/ {
		root	/home/$USER/tomcat8080/webapps/ROOT;
		expires	max;
		add_header	Cache-Control public;
		charset	utf-8;
	}
	location /remoting/ {
		return	403;
	}
	location /websocket/ {
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
		proxy_set_header  Connection "";
		proxy_http_version 1.1;
		limit_conn addr	8;
	}
}
EOF
systemctl restart nginx
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
wget http://download.redis.io/redis-stable.tar.gz
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
systemctl restart redis_6379
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
sed -i '$i root            soft    nofile          65535' /etc/security/limits.conf
sed -i '$i root            hard    nofile          65535' /etc/security/limits.conf
reboot
fi

