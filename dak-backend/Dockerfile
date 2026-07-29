# Two stages: the first builds the jar, the second runs it. The build stage
# carries Maven and the full JDK — several hundred megabytes that the running
# container has no use for. Only the jar crosses over.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencies are copied and resolved before the source. Docker caches each
# step, so editing a Java file re-runs only the compile, not the download of
# every dependency in pom.xml.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage. JRE rather than JDK: nothing here compiles.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Wildcard rather than a version-specific filename. The jar is named from the
# version in pom.xml, so hardcoding it here would break silently the next time
# that version changes.
COPY --from=build /app/target/*.jar app.jar

# Render assigns a port through PORT and expects the service to bind to it.
# application.yml fixes server.port at 8080, which is correct locally and wrong
# here — the container would start, listen on the wrong port, and be marked
# unhealthy with nothing in the logs to say why. The fallback keeps 8080 working
# when the variable is absent, which is what happens running this locally.
#
# MaxRAMPercentage matters on a small instance: the JVM's default heap sizing
# assumes it owns the machine, and on a 512MB container that means being killed
# for using memory it thought it had.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -jar app.jar --server.port=${PORT:-8080}"]