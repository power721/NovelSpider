cd web-ui
npm run build
cd ..
git add src/main/resources/static
mvn clean package -DskipTests
java -jar target/*.jar
