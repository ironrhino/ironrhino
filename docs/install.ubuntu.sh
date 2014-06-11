#!/bin/sh

#must run with sudo
if [ ! -n "$SUDO_USER" ];then
echo please run sudo $0
exit 1
else
USER="$SUDO_USER"
fi

#install packages
apt-get update
apt-get --force-yes --yes install openjdk-7-jdk ant mysql-server subversion git nginx chkconfig sysv-rc-conf fontconfig xfonts-utils zip unzip wget iptables make gcc
apt-get --force-yes --yes remove openjdk-6-jre-headless
if [ ! -f "/sbin/insserv" ] ; then
ln -s /usr/lib/insserv/insserv /sbin/insserv
fi

#config mysql
if [ -f "/etc/mysql/my.cnf" ] && ! $(more /etc/mysql/my.cnf|grep collation-server >/dev/null 2>&1) ; then
sed -i '32i innodb_stats_on_metadata = off' /etc/mysql/my.cnf
sed -i '32i collation-server = utf8_general_ci' /etc/mysql/my.cnf
sed -i '32i character-set-server = utf8' /etc/mysql/my.cnf
service mysql restart
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

#install tomcat
if [ ! -d tomcat8080 ];then
if ! $(ls -l apache-tomcat-*.tar.gz >/dev/null 2>&1) ; then
wget http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.54/bin/apache-tomcat-7.0.54.tar.gz
fi
tar xvf apache-tomcat-*.tar.gz >/dev/null && rm -rf apache-tomcat-*.tar.gz
rename s/^apache-tomcat.*$/tomcat/g apache-tomcat-*
cd tomcat && rm -rf bin/*.bat && rm -rf webapps/*
cd conf
sed -i  's/\s[3-4][a-x-]*manager.org.apache.juli.FileHandler,//g' logging.properties
sed -i '/manager/d' logging.properties
cd ..
cd ..
cat>tomcat/conf/server.xml<<EOF
<?xml version='1.0' encoding='utf-8'?>
<Server port="\${port.shutdown}" shutdown="SHUTDOWN">
  <Service name="Catalina">
    <Connector port="\${port.http}" connectionTimeout="20000" URIEncoding="UTF-8" useBodyEncodingForURI="true" useURIValidationHack="false" enableLookups="false" bindOnInit="false" server="ironrhino" maxPostSize="4194304" maxThreads="1000"/>
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false">
      </Host>
    </Engine>
  </Service>
</Server>
EOF
sed -i '99i export SPRING_PROFILES_DEFAULT' tomcat/bin/catalina.sh
sed -i '99i SPRING_PROFILES_DEFAULT="dual"' tomcat/bin/catalina.sh
sed -i '99i CATALINA_OPTS="-server -Xms128m -Xmx1024m -Xmn80m -Xss256k -XX:PermSize=128m -XX:MaxPermSize=512m -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:+UseParNewGC -XX:CMSMaxAbortablePrecleanTime=5 -Djava.awt.headless=true"' tomcat/bin/catalina.sh
cp -R tomcat tomcat8080
cp -R tomcat tomcat8081
rm -rf tomcat
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
cp tomcat8080/bin/catalina.sh .
rm -rf tomcat8080
cp -R apache-tomcat-\$version tomcat8080
mv server.xml tomcat8080/conf/
mv catalina.sh tomcat8080/bin/
cp -R tomcat8081/webapps* tomcat8080
if [ \$running = 1 ];then
tomcat8080/bin/catalina.sh start
sleep 120
tomcat8081/bin/catalina.sh stop 10 -force
fi
cp tomcat8081/conf/server.xml .
cp tomcat8081/bin/catalina.sh .
rm -rf tomcat8081
cp -R apache-tomcat-\$version tomcat8081
mv server.xml tomcat8081/conf/
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
gzip_min_length  1024;
gzip_types       text/xml text/css text/javascript application/x-javascript;
limit_conn_zone  \$binary_remote_addr zone=addr:10m;
upstream  backend  {
    server   localhost:8080;
    server   localhost:8081;
}
server {
        listen   80 default_server;
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
running=0
if [ -f /tmp/tomcat8080_pid ] && [ ! "\$( ps -P \`more /tmp/tomcat8080_pid\`|grep tomcat8080)" = "" ] ; then
running=1
fi
if [ \$running = 1 ];then
/home/$USER/tomcat8080/bin/catalina.sh stop -force 
fi
rm -rf \$1.bak
cd /home/$USER/tomcat8080/webapps/ROOT/
zip -r \$1.bak *  >/dev/null 2>&1
mv \$1.bak /home/$USER
cd
rm -rf /home/$USER/tomcat8080/webapps
mkdir -p /home/$USER/tomcat8080/webapps
unzip \$1 -d /home/$USER/tomcat8080/webapps/ROOT >/dev/null 2>&1
if [ \$running = 1 ];then
/home/$USER/tomcat8080/bin/catalina.sh start
sleep 60 
/home/$USER/tomcat8081/bin/catalina.sh stop -force 
fi
rm -rf /home/$USER/tomcat8081/webapps
mkdir -p /home/$USER/tomcat8081/webapps
cp -R /home/$USER/tomcat8080/webapps/ROOT /home/$USER/tomcat8081/webapps
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


#iptables
if [ ! -f /etc/init.d/iptables ]; then
cat>/etc/init.d/iptables<<EOF
#!/bin/sh
#
# Startup script for the tomcat
#
# chkconfig: 345 80 15
# description: Tomcat
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
wget http://download.redis.io/releases/redis-2.8.9.tar.gz
fi
if $(ls -l redis-*.tar.gz >/dev/null 2>&1) ; then
tar xvf redis-*.tar.gz >/dev/null && rm -rf redis-*.tar.gz
rename s/^redis.*$/redis/g redis-*
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


#svn checkout ironrhino
if [ ! -d ironrhino ];then
git clone https://github.com/ironrhino/ironrhino.git
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

