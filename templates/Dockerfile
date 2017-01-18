FROM java:8u111-jdk-alpine

#ENV SPRING_PROFILES_DEFAULT=dual

ADD target/ROOT.war ROOT.war

EXPOSE 8080

ENTRYPOINT ["java","-jar","/ROOT.war"]