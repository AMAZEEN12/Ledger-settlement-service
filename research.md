 #Research

#Testcontainers Service Connections

Quote sentence: "When using Testcontainers, connection details can be automatically created for a service running in a container by annotating the container field in the test class."

Page title: Testcontainers :: Spring Boot

Date read: 20 September 2026

What I verified: The `@ServiceConnection` annotation allows Spring Boot to automatically use the connection details from the PostgreSQL Testcontainer for the application during the integration test.


##Agent Review

I asked an AI agent to add a GET `/payments/{id}` endpoint.

### AI-generated output

```java
@Autowired
private PaymentRepository paymentRepository;

@GetMapping("/{id}")
public PaymentEntity getPayment(@PathVariable String id) {
    return paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
}

##Defects identified

### 1. Field injection
The agent used @Autowired directly on a field. This should use constructor injection, consistent with the rest of the application.
Fix: Inject PaymentRepository through the controller constructor.

###2. JPA entity returned as API type
The agent returned PaymentEntity directly from the controller. This exposes the persistence model as the API response.
Fix: Create a separate response record for the payment endpoint.

###3. Missing validation
The generated endpoint does not provide the required validation approach for its input.
Fix: Apply appropriate validation constraints and ensure validation is triggered by the controller.

###4. Error response is not a Problem Detail
The agent throws a generic RuntimeException when the payment is not found. This does not follow the application's RFC 9457 Problem Details error format.
Fix: Create a specific exception for a missing payment and handle it in ProblemHandler by returning a ProblemDetail.

##Verification
The four defects were identified by comparing the AI-generated code with the application's existing constructor-injection, API response, validation, and Problem Details patterns.