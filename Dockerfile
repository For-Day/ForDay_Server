FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8080

# 이미지에는 시크릿을 굽지 않는다. application-prod.yml은 ${VAR} 플레이스홀더만
# 담고 있고, 실제 값은 컨테이너 기동 시 `docker run -e`/--env-file로 주입한다
# (.github/workflows/deploy.yml, docker-compose-{blue,green}.yml 참고).
ENTRYPOINT ["java", "-jar", "app.jar"]
