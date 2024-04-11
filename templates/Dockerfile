FROM bellsoft/liberica-runtime-container:jre-8-musl

ARG app=app

RUN addgroup -g 1001 -S app && \
	adduser -u 1001 -S app -G app -h /home/app && \
	mkdir /home/app/${app} && chown app:app /home/app/${app} && \
	apk add tzdata && \
	ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
#RUN apk add ca-certificates ttf-dejavu

ENV CONTAINER=docker TZ=Asia/Shanghai
#ENV SPRING_PROFILES_DEFAULT=dual

WORKDIR /home/app

ADD --chown=app:app target/ROOT.war ROOT.war

USER app

EXPOSE 8080

VOLUME ["/home/app/${app}/conf"]

ENTRYPOINT ["java","-server","-Xms128m","-Xmx1024m","-Xmn80m","-Xss256k","-XX:+UseG1GC","-XX:MaxGCPauseMillis=200","-XX:ParallelGCThreads=10","-XX:ConcGCThreads=2","-XX:InitiatingHeapOccupancyPercent=70","-Djava.awt.headless=true","-Djava.security.egd=file:/dev/./urandom","-jar","ROOT.war"]

#app=demo
#docker build --build-arg app=${app} -t ${app} .
#docker run -d -p 8080:8080 --volume ~/${app}/conf:/home/app/${app}/conf --name ${app} ${app}