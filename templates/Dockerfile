FROM openjdk:8-jdk-alpine

ARG user=ironrhino

ARG app=app

ENV USER=${user}

#ENV SPRING_PROFILES_DEFAULT=dual

RUN mkdir /home/${user}

RUN adduser -D -h /home/${user} ${user}

VOLUME ["/home/${user}/${app}/conf"] 

WORKDIR /home/${user}

ADD target/ROOT.war ROOT.war

RUN chown -R ${user}:${user} /home/${user}

RUN apk update && apk add ca-certificates tzdata ttf-dejavu && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone

USER ${user}

EXPOSE 8080

ENTRYPOINT ["java","-server","-Xms128m","-Xmx1024m","-Xmn80m","-Xss256k","-XX:+DisableExplicitGC","-XX:+UseG1GC","-XX:SurvivorRatio=6","-XX:MaxGCPauseMillis=400","-XX:G1ReservePercent=15","-XX:InitiatingHeapOccupancyPercent=40","-XX:ConcGCThreads=2","-Djava.awt.headless=true","-jar","ROOT.war"]

#docker run -d -p 8080:8080 --volume ~/${app}/conf:/home/${user}/${app}/conf --name ${app} ${app}