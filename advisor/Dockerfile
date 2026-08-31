# Build the distribution, then copy only it into a JRE image, so the final
# image carries no Gradle, no sources and no build cache.
FROM gradle:8-jdk17 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle :advisor:installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /home/gradle/src/advisor/build/install/advisor/ ./

# Most hosts override this; the server reads PORT at startup.
ENV PORT=8080
EXPOSE 8080

# Secrets (SUPABASE_SERVICE_ROLE_KEY, GROQ_API_KEY, ...) are injected by the
# host at run time - never baked into the image.
CMD ["./bin/advisor"]
