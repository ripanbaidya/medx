# Spring Cloud Config Server Setup

## 1. Create Config Server Project

Create a Spring Boot project and add dependencies:

* `spring-boot-starter-actuator`
* `spring-cloud-config-server`
* `spring-cloud-bus`
* `spring-cloud-stream-binder-rabbit`

## 2. Enable Config Server

In your main application class:

```java
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

## 3. Decide Config Storage Strategy

Two approaches:

* **Development → Classpath (local files)** ✅ (we use this now)
* **Production → Git-based config**

## 4. Configure `application.yml`

Define basic properties: like - application info, profiles, config server related properties, management (actuator),
rabbitmq related for bus refresh at runtime, and port (8888 default here). these are mentioned here in my application
but it will depends on application requirement.

```yaml
spring:
  application:
    name: config-server
  profiles:
    active: native # for files or classpath
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config/{application}
server:
  port: 8888
```

👉 `{application}` = microservice name

## 5. Create Config Files Structure

Inside `src/main/resources`:

```
src/main/resources
 └── config
      ├── eureka-server
      │     ├── application.yml
      │     ├── application-dev.yml   # dev specific
      |     ├── application-prod.yml  # prod specific
      ├── user-service
      │     ├── application.yml
      │     ├── application-dev.yml
      |
      └──  and so on..
```

👉 Folder name = **microservice name**
👉 Profiles supported via `application-{profile}.yml`

## 6. RabbitMQ Requirement (for Bus Refresh)

Since using:

* `spring-cloud-bus`
* RabbitMQ binder

You must run RabbitMQ before starting config server:

```bash
docker-compose up -d
```

Then start your config server.

## 7. Verify Config Server

Access in browser:

```
http://localhost:8888/{application}/{profile}
```

Example:

```
http://localhost:8888/eureka-server/dev
```

---

## 🔁 Key Things to Remember

* Config Server default port → **8888**
* Folder name must match **spring.application.name** of client service
* Use **classpath for dev**, **Git for production**
* RabbitMQ required for **dynamic refresh (bus)**


--- 
My final project - Doctor Appointment Booking

First of all thanks for suggesting this as my final project, i liked it. I would like to share few point and based on that i would ask suggestions
from you, first i will share what features i want to build in this application and don't worry for those features maybe API's count will increase but
those features wont be super complex this is what i feel personally, I had nearly 35-40 api's and 12-14 services i had in my salon booking application iconic.
here after listening all my requirements i want to know what will be the total number of api's i would have the requirement and total number of microservices i would have.

i m sharing my features -

- In this application I will have three roles, Patient or end Users, Doctor, and Admin.

## Patient

- Login, register, profile management, upload profile photo very important. and something related that. leaving on you
- patient will be able to see the list of doctors, should have powerfull filter options so that he can apply filtering while seaching doctor based on his need
  like - fees range, department (eg. cardiology, dermatology and etc), years of experience of doctor and like that.
- patient should be able to book appointment with doctor, and for that i will have to handle the concurrency issue because if two patient try to book
  same slot at same time then only one should be able to book that slot and other should get the message that this slot is already booked.
- after appointment is done, patient should be able to pay online via (razorpay or stripe) and if patient select cash after visit then the from the doctor side
  for that appointment should have option to notify whether the payment has done or not. or we can automate this thing somehow. I would like to take advice how we
  can automate this thing because if doctor forget to mark the payment as done then it will create problem for patient and doctor both.
  so if we can automate this thing then it will be good.
- patient should be able to share his feedback on the platform about his experience with doctor and also about the platform, this will help us to improve our service
  and also it will help other patients to find out best doctor for them. (feedback should have category - like platform, doctor, his experience and like that)
- obviously notification, once booking done or payment done. can use websocket for realtime notification.
- should have the record of his all bookings and payments.

## Doctor

- Register, also he would have onboarding process, where he will have to submit his documents for verification and after that admin will verify those documents
  and approve or reject his registration. (want to keep it as much simple as possible otherwise it might take a hell amount of time)
- He can upload photos of his clinic, and also he can upload his profile photo, and also he can update his profile information like fees, experience, department and like that.
  so that while patinet will look for doctor if he clicks on doctor profile then he should be able to see all these information, which helps him for better decision making
- doctor should able to see his bookings, by default today or he can apply filter, completed, pending, cancelled
- payment history
- can use the feedback api's for doctor also so that doctor can share his feedback.

Currently i dont remeber other things, you can help me improve.


## Admin

- approve or reject doctor onboarding document (with rejection reason)
- list of all doctors, patients, appointments, payments and feedbacks with search and filter options.
- and other feaures if imporant, dont want to make it complext by adding more functionality.

## My requirement

- I want to use at least on of the microservice pattern - SAGA pattern for handling the booking and payment process,
  because these two are the most critical part of this application and we need to ensure that they are working perfectly without any issue.
  so if we can use SAGA pattern then it will help us to handle the failure scenario in a better way and also it will help us to maintain the
  data consistency across different services.

- spring boot microservices for backend.
- use kafka for message broker or async communication and OpenFeign for sync communication, websocket for notification
- razorpay and stripe both for payment to provide flexibility to users with strategy pattern
- postgresql for relational and mongodb for non-relational database, and redis for caching
- docker for containerization and kubernetes for orchestration, and for monitoring i will use prometheus and grafana, zipkin for distributed tracing
- keycloak with JWT for auth
- Resilience4j for resiliency
- typescript, reactjs, tailwindcss, zustand


