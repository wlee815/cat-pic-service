# CatPicService

How to start the CatPicService application
---

1. Clone this repo, then cd into its root.
1. Install Postman, then open the collection in postman_collection/
1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/cat-pic-service-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`
