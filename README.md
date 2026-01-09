# KissanMitra

A Spring Boot application for KissanMitra platform.

## Prerequisites

- Java 17 (OpenJDK recommended)
- Maven 3.6+
- MongoDB (local or Atlas)
- Redis (optional, for OTP storage)

## Setup

1. Clone the repository:

   ```bash
   git clone <repository-url>
   cd KissanMitraJava
   ```

2. Install dependencies:

   ```bash
   mvn clean install
   ```

3. Configure environment:

   - Copy `application-local.properties` and update with your settings
   - Ensure MongoDB is running locally or update connection string for Atlas
   - For Redis, update Redis config if using

4. Run the application:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

   The application will be available at http://localhost:8080/

## Troubleshooting

### Java Version Issues

- Ensure Java 17 is installed and set as JAVA_HOME
- Check version: `java -version`
- If using multiple versions, use jenv or update PATH

### Maven Build Failures

- Clean and rebuild: `mvn clean install`
- Check for dependency issues: `mvn dependency:tree`
- Ensure internet connection for downloading dependencies

### Lombok and Java Compatibility Issues

If you encounter compilation errors related to Lombok and Java versions (e.g., `ExceptionInInitializerError` with `TypeTag.UNKNOWN`):

- Ensure Lombok version is 1.18.38 or later in `pom.xml`
- Update Maven Compiler Plugin to 3.13.0 or later
- Use `<release>17</release>` instead of separate `<source>` and `<target>`
- For Java 25+ with Mockito tests, add `-Dnet.bytebuddy.experimental=true` to Surefire plugin configuration

### Database Connection

- Verify MongoDB is running: `mongosh` or check service
- Check connection string in application.properties
- For Atlas, ensure IP whitelist and credentials

### Port Conflicts

- Default port is 8080; change in application.properties if needed
- Check running processes: `lsof -i :8080`

### Redis Issues

- If Redis is not available, app falls back to in-memory OTP storage
- Install and start Redis if needed

### Logs

- Check application logs in console or `logs/` directory
- Enable debug logging by setting `logging.level.com.kissanmitra=DEBUG`
