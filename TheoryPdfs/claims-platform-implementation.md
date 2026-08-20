# Claims Platform — Complete Implementation Reference

Every file, manifest and script needed to build the system described in your project narrative, **in the same order the explanation walks through it**.

Convention used throughout:

> `// ⟵ TALKING POINT: ...` marks the exact line(s) that back a specific claim in your project explanation. When an interviewer drills, these are the lines you are describing from memory.

---

## 0. The system, end to end

```
Agent Portal / Mobile App
        │  HTTPS
        ▼
   Route 53  (claims.insurer.com  →  ALIAS)
        ▼
   ALB  (public subnets, TLS terminated)          ← created by AWS Load Balancer Controller
        │                                            from the Ingress resource
        │  path routing, IP-mode target group
        ▼
┌──────────────────────── EKS cluster (3 AZs, private subnets) ─────────────────────┐
│  claims-service   policy-service   payment-service      (Spring Boot pods)        │
│        │                                                                          │
│        │ 1. JDBC write (Multi-AZ RDS PostgreSQL)                                  │
│        │ 2. PL/SQL call → legacy Oracle policy admin                              │
│        │ 3. after commit → publish ClaimSubmitted                                 │
└────────┼──────────────────────────────────────────────────────────────────────────┘
         ▼
   SNS topic  claims-events   ── filter policies per subscription ──┐
         │                                                          │
   ┌─────┴───────────────┬──────────────────────┬──────────────────┘
   ▼                     ▼                      ▼
 SQS document-q       SQS notification-q     SQS fraud-q
   │  + DLQ              │  + DLQ               │  + DLQ
   ▼                     ▼                      ▼
 document-worker     notification-worker    fraud-worker

Observability spine (all boxes above):
  stdout JSON  → Fluent Bit DaemonSet → CloudWatch Logs → Logs Insights
  Micrometer   → CloudWatch Metrics   → Alarms → SNS → PagerDuty
  X-Ray SDK    → X-Ray daemon (UDP 2000) → Service Map / traces
```

**Repo layout assumed:**

```
claims-platform/
├── claims-service/
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/insurer/claims/...
│   └── src/main/resources/
│       ├── application.yml
│       ├── logback-spring.xml
│       └── db/changelog/
├── notification-worker/
├── k8s/
│   ├── base/
│   └── overlays/{dev,uat,prod}/
├── terraform/
│   ├── vpc.tf  rds.tf  messaging.tf  alarms.tf  iam.tf
└── Jenkinsfile
```

---

# PART 1 — The Spring Boot service

## 1.1 `build.gradle` (Java 17, Spring Boot 2.7)

```groovy
plugins {
    id 'org.springframework.boot' version '2.7.18'   // ⟵ TALKING POINT: "Spring Boot needed to be
    id 'io.spring.dependency-management' version '1.1.4'  //   at least 2.5, we targeted 2.7"
    id 'java'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)  // ⟵ TALKING POINT: Java 11 → 17 LTS upgrade.
    }                                                 //   Toolchain, not sourceCompatibility, so the
}                                                     //   build is reproducible on any agent.

repositories { mavenCentral() }

ext {
    set('springCloudAwsVersion', '2.4.4')
    set('awsSdkVersion', '2.25.11')
    set('xrayVersion', '2.15.3')
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // --- Actuator + Micrometer: liveness/readiness probes AND metrics -------------
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-cloudwatch2'  // ⟵ metrics → CloudWatch

    // --- AWS SDK v2 --------------------------------------------------------------
    implementation platform("software.amazon.awssdk:bom:${awsSdkVersion}")
    implementation 'software.amazon.awssdk:sns'
    implementation 'software.amazon.awssdk:sqs'
    implementation 'software.amazon.awssdk:sts'   // ⟵ TALKING POINT: IRSA needs STS on the
                                                  //   classpath for AssumeRoleWithWebIdentity

    // --- X-Ray -------------------------------------------------------------------
    implementation platform("com.amazonaws:aws-xray-recorder-sdk-bom:${xrayVersion}")
    implementation 'com.amazonaws:aws-xray-recorder-sdk-core'
    implementation 'com.amazonaws:aws-xray-recorder-sdk-spring'
    implementation 'com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2'
    implementation 'com.amazonaws:aws-xray-recorder-sdk-sql'        // JDBC subsegments

    // --- Resilience ---------------------------------------------------------------
    implementation 'io.github.resilience4j:resilience4j-spring-boot2:1.7.1'
    implementation 'org.springframework.boot:spring-boot-starter-aop'

    // --- Persistence --------------------------------------------------------------
    runtimeOnly 'org.postgresql:postgresql'
    runtimeOnly 'com.oracle.database.jdbc:ojdbc11'   // ⟵ ojdbc11 = the Java 11+ build; ojdbc8
    implementation 'org.liquibase:liquibase-core'    //   pulls in JDK-internal reflection that
                                                     //   JEP 403 blocks on 17.

    // --- Logging: structured JSON, not plain text ---------------------------------
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'

    compileOnly 'org.projectlombok:lombok:1.18.30'   // ⟵ TALKING POINT: Lombok < 1.18.22 does not
    annotationProcessor 'org.projectlombok:lombok:1.18.30'  //   compile on 17 — it reflects into
                                                     //   com.sun.tools.javac internals.
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:postgresql:1.19.7'
    testImplementation 'org.testcontainers:localstack:1.19.7'
}

dependencyManagement {
    imports { mavenBom "io.awspring.cloud:spring-cloud-aws-dependencies:${springCloudAwsVersion}" }
}

tasks.named('test') { useJUnitPlatform() }
```

---

## 1.2 `Dockerfile`

```dockerfile
# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build   # ⟵ TALKING POINT: "changed the base image from
WORKDIR /workspace                           #   eclipse-temurin:11-jre to 17-jre"
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon        # layer-cache deps before source changes
COPY src src
RUN ./gradlew bootJar --no-daemon

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
RUN groupadd -r spring && useradd -r -g spring spring   # non-root: pairs with the pod
USER spring                                             # securityContext below
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

# ⟵ TALKING POINT: "you don't set -Xmx, you set -XX:MaxRAMPercentage=75".
#    The JVM is container-aware since Java 10 — it reads the cgroup limit, not host RAM.
#    Setting -Xmx here would break the moment someone changes the pod memory limit.
ENV JAVA_TOOL_OPTIONS="\
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Dnetworkaddress.cache.ttl=5 \
  -Djava.security.egd=file:/dev/./urandom"
#                    ↑ ⟵ TALKING POINT: JVM DNS TTL. Default caches forever in some configs;
#                      RDS failover flips a CNAME, so a cached entry keeps you pointed at a
#                      host that no longer exists. 5 seconds is the standard hardening.

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

> **Why `JAVA_TOOL_OPTIONS` and not `ENTRYPOINT java -XX:... `?** Because `JAVA_TOOL_OPTIONS` is also picked up by anything else that starts a JVM in the container (Liquibase CLI, a debug shell), and it can be overridden per-environment from a ConfigMap without rebuilding the image.

---

## 1.3 `application.yml` — the annotated version

Almost every line here is a talking point. This is the single most useful file to have memorised.

```yaml
spring:
  application:
    name: claims-service

  # ---------- Graceful shutdown ----------------------------------------------
  lifecycle:
    timeout-per-shutdown-phase: 30s   # ⟵ TALKING POINT: pairs with server.shutdown below.
                                      #   Without this the JVM SIGTERMs mid-request → 502s.
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/claimsdb?ApplicationName=claims-service
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}          # ⟵ injected from Secrets Manager via CSI driver, §2.7
    driver-class-name: org.postgresql.Driver
    hikari:
      # ⟵ TALKING POINT — the connection-pool arithmetic story.
      #   12 pods × 20 = 240 connections. Autoscale to 40 pods = 800 and RDS falls over,
      #   because every PostgreSQL connection is a separate OS process, not a thread.
      #   A pool of 10 is plenty: connections should be scarce and fast, not numerous.
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 3000        # fail fast rather than pile up threads
      validation-timeout: 2000        # ⟵ validate borrowed connections (JDBC4 isValid)
      idle-timeout: 300000
      max-lifetime: 900000            # ⟵ < RDS Proxy / NLB idle timeout, and forces periodic
                                      #   reconnection so a post-failover pool self-heals
      keepalive-time: 120000
      leak-detection-threshold: 20000
      data-source-properties:
        socketTimeout: 30             # ⟵ TALKING POINT: "set a socket timeout so a hung
        connectTimeout: 5             #   connection doesn't block a thread indefinitely."
        tcpKeepAlive: true            #   During failover the old host is a black hole — without
                                      #   socketTimeout the thread waits on the OS default (~15 min).

  jpa:
    open-in-view: false               # ⟵ senior signal: OSIV holds a connection for the whole
    hibernate:                        #   request and hides N+1 problems behind lazy loading
      ddl-auto: validate              # ⟵ never `update` in prod — Liquibase owns the schema
    properties:
      hibernate:
        jdbc.batch_size: 50
        order_inserts: true
        query.in_clause_parameter_padding: true

  liquibase:
    enabled: false                    # ⟵ TALKING POINT: migrations run as a Kubernetes Job,
                                      #   NOT on app startup. 12 pods starting at once would
                                      #   race for the changelog lock. See §2.8.

server:
  shutdown: graceful                  # ⟵ TALKING POINT: stop accepting new connections, drain
  port: 8080                          #   in-flight requests, then exit.
  tomcat:
    threads.max: 100                  # ⟵ keep this ≥ Hikari pool, and think about it: 100 threads
    accept-count: 100                 #   fighting over 10 connections is a queue, not a deadlock

management:
  endpoint:
    health:
      probes:
        enabled: true                 # ⟵ TALKING POINT: this is what splits /actuator/health into
      group:                          #   .../liveness and .../readiness. Without it they 404.
        liveness:
          include: livenessState      # ⟵ TALKING POINT: liveness asks ONLY "is this JVM alive".
        readiness:                    #   Do NOT add db here. A DB blip would fail liveness on
          include: readinessState,db  #   every pod → whole fleet restart-loops.
  endpoints:
    web.exposure.include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:dev}
      # ⟵ TALKING POINT (cost gotcha): NEVER put claimId here. Custom metrics bill per unique
      #   name+dimension combination — a claimId dimension creates one metric per claim.
      #   Identifiers belong in logs and X-Ray annotations, not metric dimensions.
    distribution:
      percentiles-histogram.http.server.requests: true
      percentiles.http.server.requests: 0.5, 0.95, 0.99
    export:
      cloudwatch:
        namespace: ClaimsPlatform
        step: 1m
        batch-size: 20

app:
  sns:
    claims-topic-arn: ${CLAIMS_TOPIC_ARN}
  sqs:
    notification-queue-url: ${NOTIFICATION_QUEUE_URL}

resilience4j:
  circuitbreaker:
    instances:
      documentService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 50
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 5
  bulkhead:
    instances:
      documentService:
        maxConcurrentCalls: 10   # ⟵ TALKING POINT: the cascading-failure incident. Bulkhead caps
                                 #   how many threads can be stuck on one slow downstream.
  timelimiter:
    instances:
      documentService:
        timeoutDuration: 2s
```

---

## 1.4 Domain model — Java 17 language features, used for real

### `ClaimStatus` — sealed interface (closed domain hierarchy)

```java
package com.insurer.claims.domain;

import java.time.Instant;

/**
 * ⟵ TALKING POINT: "sealed interfaces for closed domain hierarchies like claim status".
 * The compiler now knows the complete set of subtypes, which makes the switch in
 * ClaimStatusMapper exhaustive WITHOUT a default branch. Add a new status and every
 * switch that doesn't handle it fails to compile — that is the whole point.
 */
public sealed interface ClaimStatus
        permits ClaimStatus.Submitted,
                ClaimStatus.UnderReview,
                ClaimStatus.Approved,
                ClaimStatus.Rejected {

    Instant occurredAt();

    record Submitted(Instant occurredAt, String channel) implements ClaimStatus {}
    record UnderReview(Instant occurredAt, String assessorId) implements ClaimStatus {}
    record Approved(Instant occurredAt, java.math.BigDecimal settledAmount) implements ClaimStatus {}
    record Rejected(Instant occurredAt, String reasonCode, String narrative) implements ClaimStatus {}
}
```

### Exhaustive switch expression + pattern matching

```java
package com.insurer.claims.domain;

public final class ClaimStatusPresenter {

    /**
     * ⟵ TALKING POINT: "switch expressions" + "pattern matching for switch".
     * No `default:` branch, no fall-through, no `break`, and the compiler enforces
     * exhaustiveness because ClaimStatus is sealed.
     */
    public static String customerFacingMessage(ClaimStatus status) {
        return switch (status) {
            case ClaimStatus.Submitted s ->
                    "We received your claim via " + s.channel() + ".";
            case ClaimStatus.UnderReview r ->
                    "An assessor is reviewing your claim.";
            case ClaimStatus.Approved a ->
                    "Your claim was approved for " + a.settledAmount() + ".";
            case ClaimStatus.Rejected x ->
                    "Your claim was not approved (" + x.reasonCode() + ").";
        };
    }

    /**
     * ⟵ TALKING POINT: "pattern matching for instanceof" — no cast after the check.
     */
    public static boolean isTerminal(ClaimStatus status) {
        if (status instanceof ClaimStatus.Approved a && a.settledAmount().signum() > 0) {
            return true;
        }
        return status instanceof ClaimStatus.Rejected;
    }
}
```

### Records as DTOs

```java
package com.insurer.claims.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ⟵ TALKING POINT: "records for DTOs" — the Java 11 version of this class was ~90 lines
 * with Lombok or ~140 hand-written. Immutable, equals/hashCode/toString for free.
 */
public record SubmitClaimRequest(
        @NotBlank @Pattern(regexp = "POL-\\d{8}") String policyNumber,
        @NotNull LocalDate incidentDate,
        @NotBlank String claimType,            // AUTO | HEALTH | PROPERTY — drives SNS filtering
        @NotNull @DecimalMin("0.01") BigDecimal claimedAmount,
        @Size(max = 2000) String description
) {
    // compact constructor: normalisation belongs here, not in the service
    public SubmitClaimRequest {
        claimType = claimType == null ? null : claimType.toUpperCase();
    }
}

public record SubmitClaimResponse(String claimId, String status, java.time.Instant receivedAt) {}
```

### JPA entity

```java
package com.insurer.claims.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claims",
       indexes = @Index(name = "idx_claims_policy_date",
                        columnList = "policy_number, claim_date DESC"))
//                      ⟵ TALKING POINT: the composite index from the performance story.
//                        Declared here for documentation; Liquibase actually creates it (§3.5).
public class Claim {

    @Id
    @Column(name = "claim_id", length = 20)
    private String claimId;                       // CLM-88231 — business key, used as X-Ray annotation

    @Column(name = "policy_number", nullable = false, length = 20)
    private String policyNumber;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "claim_type", nullable = false, length = 20)
    private String claimType;

    @Column(name = "claimed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Version
    private Long version;                         // optimistic locking — two assessors, one claim

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ⟵ TALKING POINT: this association is the source of the N+1 problem. LAZY is correct;
    //    the fix is a JOIN FETCH at the query that needs it (§3.6), not EAGER here.
    @OneToMany(mappedBy = "claim", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Claimant> claimants = new ArrayList<>();

    protected Claim() {}                          // JPA

    // getters/setters/builder omitted for brevity
}
```

---

## 1.5 Controller

```java
package com.insurer.claims.api;

import com.amazonaws.xray.AWSXRay;
import jakarta.validation.Valid;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);
    private final ClaimSubmissionService submissionService;

    public ClaimController(ClaimSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmitClaimResponse> submit(@Valid @RequestBody SubmitClaimRequest request) {

        SubmitClaimResponse response = submissionService.submit(request);

        // ⟵ TALKING POINT: annotations vs metadata.
        //    ANNOTATION = indexed, searchable with the filter expression
        //        annotation.claimId = "CLM-88231"
        //    Limited to 50 per segment, so reserve them for identifiers you will search by.
        AWSXRay.getCurrentSegmentOptional()
               .ifPresent(seg -> {
                   seg.putAnnotation("claimId", response.claimId());
                   seg.putAnnotation("claimType", request.claimType());
                   // METADATA = arbitrary JSON, NOT indexed, NOT searchable. Context only.
                   seg.putMetadata("request", "payload", request);
               });

        log.info("Claim accepted");   // claimId comes from MDC — see logback config §6.2
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
```

---

## 1.6 The service — write, then publish (and why order matters)

```java
package com.insurer.claims.api;

import com.insurer.claims.domain.*;
import com.insurer.claims.messaging.ClaimEventPublisher;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.*;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class ClaimSubmissionService {

    private final ClaimRepository claimRepository;
    private final PolicyGateway policyGateway;              // Oracle PL/SQL, §3.7
    private final ApplicationEventPublisher events;

    public ClaimSubmissionService(ClaimRepository claimRepository,
                                  PolicyGateway policyGateway,
                                  ApplicationEventPublisher events) {
        this.claimRepository = claimRepository;
        this.policyGateway  = policyGateway;
        this.events         = events;
    }

    @Transactional
    public SubmitClaimResponse submit(SubmitClaimRequest request) {

        // 1. Legacy Oracle validation — decades of regulatory logic nobody will rewrite
        PolicyValidation validation = policyGateway.validatePolicy(
                request.policyNumber(), request.incidentDate());

        if (!validation.valid()) {
            throw new PolicyNotEligibleException(validation.reasonCode());
        }

        // 2. Write to RDS PostgreSQL
        Claim claim = Claim.create(request, validation.policyHolderId());
        claimRepository.save(claim);

        MDC.put("claimId", claim.getClaimId());   // ⟵ correlation ID onto every subsequent log line

        // 3. ⟵ TALKING POINT: "Once the transaction COMMITS, the service publishes."
        //    Publishing inside the transaction is a classic bug: if the commit later fails,
        //    consumers have already been told about a claim that does not exist.
        //    Spring's AFTER_COMMIT event phase gives us the ordering guarantee for free.
        events.publishEvent(new ClaimSubmittedEvent(
                claim.getClaimId(), claim.getPolicyNumber(),
                claim.getClaimType(), claim.getClaimedAmount()));

        return new SubmitClaimResponse(claim.getClaimId(), "SUBMITTED", claim.getCreatedAt());
    }
}
```

```java
package com.insurer.claims.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class ClaimEventListener {

    private final ClaimEventPublisher publisher;

    public ClaimEventListener(ClaimEventPublisher publisher) { this.publisher = publisher; }

    /**
     * ⟵ TALKING POINT: AFTER_COMMIT. The SNS publish happens only once the DB transaction
     * has actually committed. If SNS then fails we log + alarm; the durable fallback is the
     * transactional-outbox variant below, which is the answer to "what if SNS is down?"
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClaimSubmitted(ClaimSubmittedEvent event) {
        publisher.publishClaimSubmitted(event);
    }
}
```

> **Follow-up you will get:** *"What if the commit succeeds and the SNS publish fails?"*
> Honest answer: you have a dual-write problem. The durable fix is the **transactional outbox** — insert the event into an `outbox_events` table in the *same* transaction as the claim, and have a poller (or Debezium CDC) publish rows to SNS and mark them sent. Then the only failure mode is duplicate delivery, which your consumers already tolerate because they are idempotent.

Outbox variant, if you want to be able to describe it concretely:

```java
@Transactional
public SubmitClaimResponse submitWithOutbox(SubmitClaimRequest request) {
    Claim claim = Claim.create(request, /* ... */ null);
    claimRepository.save(claim);

    // same transaction, same commit → atomic with the business write
    outboxRepository.save(OutboxEvent.of(
            "ClaimSubmitted", claim.getClaimId(), toJson(claim)));

    return new SubmitClaimResponse(claim.getClaimId(), "SUBMITTED", claim.getCreatedAt());
}

@Scheduled(fixedDelay = 500)
@Transactional
public void drainOutbox() {
    // SKIP LOCKED so multiple pods can poll the outbox without fighting each other
    List<OutboxEvent> batch = outboxRepository.lockNextUnpublished(100);
    for (OutboxEvent e : batch) {
        snsPublisher.publishRaw(e);
        e.markPublished();
    }
}
```

```sql
-- repository query behind lockNextUnpublished
SELECT * FROM outbox_events
 WHERE published_at IS NULL
 ORDER BY created_at
 LIMIT :limit
   FOR UPDATE SKIP LOCKED;   -- ⟵ PostgreSQL 9.5+; the reason multiple pollers don't collide
```

---

# PART 2 — Amazon EKS

## 2.1 Cluster + IRSA (the thing to learn cold)

### `eksctl` cluster definition

```yaml
# k8s/cluster.yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: insurance-prod
  region: ap-south-1
  version: "1.29"

iam:
  withOIDC: true          # ⟵ TALKING POINT: THIS is the prerequisite for IRSA. It registers an
                          #   OIDC identity provider for the cluster with IAM. Without it,
                          #   AssumeRoleWithWebIdentity has nothing to trust.

vpc:
  id: vpc-0abc123
  subnets:
    private:              # ⟵ worker nodes in PRIVATE subnets across 3 AZs
      ap-south-1a: { id: subnet-0aaa }
      ap-south-1b: { id: subnet-0bbb }
      ap-south-1c: { id: subnet-0ccc }
    public:               # ⟵ ALB lives here
      ap-south-1a: { id: subnet-0ddd }
      ap-south-1b: { id: subnet-0eee }
      ap-south-1c: { id: subnet-0fff }

managedNodeGroups:
  - name: app-ng
    instanceTypes: ["m6i.large", "m6i.xlarge"]
    minSize: 3
    maxSize: 12
    desiredCapacity: 6
    privateNetworking: true
    availabilityZones: ["ap-south-1a", "ap-south-1b", "ap-south-1c"]
    volumeSize: 50
    volumeType: gp3

addons:
  - name: vpc-cni
    configurationValues: |
      {
        "enableNetworkPolicy": "true",
        "env": {
          "ENABLE_PREFIX_DELEGATION": "true",
          "WARM_PREFIX_TARGET": "1"
        }
      }
      # ⟵ TALKING POINT (networking gotcha): the VPC CNI gives every pod a REAL VPC IP.
      #   A /24 subnet has 251 usable IPs — you run out of IPs long before you run out of CPU.
      #   Prefix delegation allocates /28 blocks per ENI instead of individual IPs, raising
      #   pod density roughly 16×. The other mitigation is simply larger subnet CIDRs (/20).
  - name: coredns
  - name: kube-proxy
  - name: aws-ebs-csi-driver
```

```bash
eksctl create cluster -f k8s/cluster.yaml
```

### The IAM role and trust policy behind IRSA

```json
// terraform/iam/claims-service-trust-policy.json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::123456789012:oidc-provider/oidc.eks.ap-south-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B716D3041E"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "oidc.eks.ap-south-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B716D3041E:aud":
          "sts.amazonaws.com",

        // ⟵ TALKING POINT: "the trust policy is scoped to that specific NAMESPACE and
        //    SERVICE ACCOUNT NAME". This one condition is the entire security story.
        //    Get it wrong (e.g. use StringLike with a wildcard) and any pod in any
        //    namespace can assume the role.
        "oidc.eks.ap-south-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B716D3041E:sub":
          "system:serviceaccount:claims:claims-service-sa"
      }
    }
  }]
}
```

```hcl
# terraform/iam.tf
resource "aws_iam_role" "claims_service" {
  name               = "eks-claims-service-role"
  assume_role_policy = file("${path.module}/iam/claims-service-trust-policy.json")
}

# ⟵ TALKING POINT: "permissions are per-SERVICE instead of per-NODE".
#   The pre-IRSA alternative was attaching this to the node instance profile — which means
#   EVERY pod on that node inherits SNS publish rights, including a compromised sidecar.
resource "aws_iam_policy" "claims_service" {
  name = "claims-service-policy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["sns:Publish"]
        Resource = aws_sns_topic.claims_events.arn        # least privilege: one topic, not *
      },
      {
        Effect   = "Allow"
        Action   = ["sqs:ReceiveMessage", "sqs:DeleteMessage",
                    "sqs:ChangeMessageVisibility", "sqs:GetQueueAttributes"]
        Resource = [aws_sqs_queue.notifications.arn]
      },
      {
        Effect   = "Allow"
        Action   = ["secretsmanager:GetSecretValue"]
        Resource = aws_secretsmanager_secret.claims_db.arn
      },
      {
        Effect   = "Allow"
        Action   = ["xray:PutTraceSegments", "xray:PutTelemetryRecords",
                    "xray:GetSamplingRules", "xray:GetSamplingTargets"]
        Resource = "*"                                    # X-Ray write actions are not resource-scoped
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "claims_service" {
  role       = aws_iam_role.claims_service.name
  policy_arn = aws_iam_policy.claims_service.arn
}
```

### The ServiceAccount

```yaml
# k8s/base/serviceaccount.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: claims-service-sa
  namespace: claims
  annotations:
    # ⟵ TALKING POINT: this single annotation is what the EKS Pod Identity Webhook watches.
    #   It mutates every pod using this SA to add:
    #     - env AWS_ROLE_ARN + AWS_WEB_IDENTITY_TOKEN_FILE
    #     - a projected service-account-token volume at
    #       /var/run/secrets/eks.amazonaws.com/serviceaccount/token
    #   The AWS SDK's DEFAULT CREDENTIAL CHAIN then finds the token file and calls
    #   sts:AssumeRoleWithWebIdentity. Result: no static access keys anywhere.
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/eks-claims-service-role
    eks.amazonaws.com/sts-regional-endpoints: "true"
```

**Verifying it in an interview-credible way:**

```bash
# what the webhook actually injected
kubectl -n claims exec deploy/claims-service -- env | grep AWS_
# AWS_ROLE_ARN=arn:aws:iam::123456789012:role/eks-claims-service-role
# AWS_WEB_IDENTITY_TOKEN_FILE=/var/run/secrets/eks.amazonaws.com/serviceaccount/token
# AWS_REGION=ap-south-1

# prove the pod assumed the role, not the node role
kubectl -n claims exec deploy/claims-service -- \
  aws sts get-caller-identity
# Arn: arn:aws:sts::123456789012:assumed-role/eks-claims-service-role/botocore-session-...
```

The Java side needs **no code at all** — that's the point:

```java
package com.insurer.claims.config;

import com.amazonaws.xray.interceptors.TracingInterceptor;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import org.springframework.context.annotation.*;

@Configuration
public class AwsClientConfig {

    /**
     * ⟵ TALKING POINT: notice there is NO credentials provider and NO access key.
     * DefaultCredentialsProvider walks: system properties → env vars → web identity token
     * file (IRSA lands here) → profile → container → EC2 IMDS.
     * The token file is present, so it wins before IMDS/node role.
     */
    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())  // ⟵ X-Ray subsegments
                        .build())                                           //   for every SNS call
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build())
                .build();
    }
}
```

---

## 2.2 Deployment — every field is a talking point

```yaml
# k8s/base/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: claims-service
  namespace: claims
spec:
  replicas: 6
  revisionHistoryLimit: 5
  selector:
    matchLabels: { app: claims-service }

  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0     # ⟵ TALKING POINT: "zero downtime deploy". maxUnavailable=0 means
                            #   a new pod must be READY before an old one is removed. With
                            #   maxUnavailable=1 you knowingly run below capacity mid-deploy.

  template:
    metadata:
      labels: { app: claims-service }
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: claims-service-sa        # ⟵ IRSA wiring, §2.1
      terminationGracePeriodSeconds: 60            # ⟵ must exceed preStop sleep + graceful
                                                   #   shutdown timeout (5s + 30s), else SIGKILL
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000

      # ⟵ TALKING POINT: "What happens when a node dies?" — the node controller marks it
      #   NotReady after the monitor grace period, pods get evicted, the scheduler places
      #   replacements. That only helps if the replicas weren't all on the same node/AZ.
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone     # spread across the 3 AZs
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels: { app: claims-service }
        - maxSkew: 1
          topologyKey: kubernetes.io/hostname          # and across nodes within an AZ
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels: { app: claims-service }

      containers:
        - name: claims-service
          image: 123456789012.dkr.ecr.ap-south-1.amazonaws.com/claims-service:a1b2c3d
          #                                                                    ↑ git SHA, never :latest
          imagePullPolicy: IfNotPresent
          ports:
            - { name: http, containerPort: 8080 }

          # ---- LIVENESS ------------------------------------------------------------
          # ⟵ TALKING POINT: "liveness should only ask 'is this JVM alive'."
          #   The classic mistake is pointing liveness at a check that includes the DB:
          #   one RDS blip → every pod fails liveness → the WHOLE FLEET restart-loops
          #   and you've turned a 90-second degradation into a full outage.
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            initialDelaySeconds: 45      # JVM + Spring context startup
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 3          # 30s of failure before the kill

          # ---- READINESS -----------------------------------------------------------
          # ⟵ TALKING POINT: readiness failing removes the pod from the ALB target group
          #   but LEAVES IT RUNNING — correct for "my connection pool is exhausted,
          #   don't send me traffic yet". It self-heals; liveness does not.
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            initialDelaySeconds: 20
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 2

          # ---- STARTUP -------------------------------------------------------------
          # Lets you keep liveness aggressive without killing a slow-booting JVM:
          # liveness doesn't start until startupProbe succeeds. 30 × 10s = 5 min budget.
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
            failureThreshold: 30

          lifecycle:
            preStop:
              exec:
                # ⟵ TALKING POINT: "a preStop hook that sleeps ~5 seconds so the ALB finishes
                #   deregistering the pod before the JVM stops accepting connections.
                #   WITHOUT THIS YOU GET 502s ON EVERY DEPLOY."
                #   Why: pod deletion fires SIGTERM and target deregistration IN PARALLEL.
                #   The ALB is still sending traffic for a moment after Tomcat closes.
                command: ["sh", "-c", "sleep 5"]

          resources:
            requests: { cpu: "500m", memory: "1Gi" }
            limits:   { memory: "1Gi" }      # ⟵ memory limit == request (Guaranteed-ish).
                                             #   NOTE: no CPU limit — CFS throttling on a JVM
                                             #   during GC is a classic latency mystery.
          env:
            - name: ENVIRONMENT
              value: prod
            - { name: DB_HOST,              valueFrom: { configMapKeyRef: { name: claims-config, key: DB_HOST } } }
            - { name: CLAIMS_TOPIC_ARN,     valueFrom: { configMapKeyRef: { name: claims-config, key: CLAIMS_TOPIC_ARN } } }
            - { name: NOTIFICATION_QUEUE_URL, valueFrom: { configMapKeyRef: { name: claims-config, key: NOTIFICATION_QUEUE_URL } } }
            - { name: DB_USERNAME, valueFrom: { secretKeyRef: { name: claims-db-secret, key: username } } }
            - { name: DB_PASSWORD, valueFrom: { secretKeyRef: { name: claims-db-secret, key: password } } }

            # ⟵ TALKING POINT: X-Ray daemon address. The SDK sends UDP segment documents to
            #   the daemon on the NODE (hostPort 2000) — the app never calls the X-Ray API.
            - name: AWS_XRAY_DAEMON_ADDRESS
              valueFrom: { fieldRef: { fieldPath: status.hostIP } }
            - name: AWS_XRAY_CONTEXT_MISSING
              value: LOG_ERROR      # don't throw when a segment is missing (e.g. @Async threads)

          volumeMounts:
            - name: secrets-store
              mountPath: /mnt/secrets
              readOnly: true

      volumes:
        - name: secrets-store
          csi:
            driver: secrets-store.csi.k8s.io
            readOnly: true
            volumeAttributes: { secretProviderClass: claims-db-spc }
```

## 2.3 Service (ClusterIP)

```yaml
# k8s/base/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: claims-service
  namespace: claims
spec:
  type: ClusterIP        # ⟵ TALKING POINT: "a Service of type ClusterIP gives it a stable
  selector:              #   internal DNS name" — claims-service.claims.svc.cluster.local.
    app: claims-service  #   NOT LoadBalancer: we do NOT want one ELB per service; the ALB
  ports:                 #   Controller gives us ONE shared ALB via Ingress.
    - name: http
      port: 80
      targetPort: http
```

## 2.4 Ingress → the ALB Controller provisions the ALB

```yaml
# k8s/base/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: claims-platform
  namespace: claims
  annotations:
    # ⟵ TALKING POINT: "an ALB created and managed by the AWS Load Balancer Controller
    #    FROM A KUBERNETES INGRESS RESOURCE". The controller watches Ingress objects and
    #    calls the ELBv2 API. You never touch the console.
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/subnets: subnet-0ddd,subnet-0eee,subnet-0fff   # public subnets

    # ⟵ TALKING POINT: IP MODE. Because the VPC CNI gives every pod a real VPC IP, the ALB
    #    registers POD IPs directly as targets. instance mode would hop through a NodePort
    #    and kube-proxy, adding a hop and losing per-pod health granularity.
    alb.ingress.kubernetes.io/target-type: ip

    # ⟵ TALKING POINT: "the ALB terminates TLS"
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP":80},{"HTTPS":443}]'
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:ap-south-1:123456789012:certificate/abcd
    alb.ingress.kubernetes.io/ssl-redirect: '443'
    alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS13-1-2-2021-06

    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health/readiness
    alb.ingress.kubernetes.io/healthcheck-interval-seconds: '10'
    alb.ingress.kubernetes.io/healthy-threshold-count: '2'

    # ⟵ pairs with the preStop sleep: give the ALB time to finish draining
    alb.ingress.kubernetes.io/target-group-attributes: deregistration_delay.timeout_seconds=30

    alb.ingress.kubernetes.io/group.name: insurance-prod   # ONE ALB shared by all Ingresses
    alb.ingress.kubernetes.io/load-balancer-attributes: >
      routing.http.xff_header_processing.mode=append,
      access_logs.s3.enabled=true,
      access_logs.s3.bucket=insurance-alb-logs
    alb.ingress.kubernetes.io/wafv2-acl-arn: arn:aws:wafv2:ap-south-1:123456789012:regional/webacl/insurance/xyz
spec:
  ingressClassName: alb
  rules:
    - host: claims.insurer.com
      http:
        paths:                        # ⟵ TALKING POINT: "routes BY PATH to claims-service,
          - path: /claims             #    policy-service, or payment-service"
            pathType: Prefix
            backend: { service: { name: claims-service,  port: { number: 80 } } }
          - path: /policies
            pathType: Prefix
            backend: { service: { name: policy-service,  port: { number: 80 } } }
          - path: /payments
            pathType: Prefix
            backend: { service: { name: payment-service, port: { number: 80 } } }
```

Installing the controller (this is what people forget to be able to describe):

```bash
# 1. IRSA for the controller itself
eksctl create iamserviceaccount \
  --cluster insurance-prod --namespace kube-system \
  --name aws-load-balancer-controller \
  --attach-policy-arn arn:aws:iam::123456789012:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# 2. Helm install
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=insurance-prod \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

## 2.5 HorizontalPodAutoscaler

```yaml
# k8s/base/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: claims-service
  namespace: claims
spec:
  scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: claims-service }
  minReplicas: 6
  maxReplicas: 24        # ⟵ TALKING POINT: bound this deliberately! 24 pods × Hikari 10 = 240
                         #   connections. maxReplicas is a DATABASE safety limit as much as a
                         #   cost limit. This is the connection-pool arithmetic in practice.
  metrics:
    - type: Resource
      resource: { name: cpu, target: { type: Utilization, averageUtilization: 65 } }
    # ⟵ "or a custom metric" — scale the worker on queue backlog, not CPU
    - type: External
      external:
        metric:
          name: sqs_approximate_age_of_oldest_message
          selector: { matchLabels: { queue: claims-notifications } }
        target: { type: AverageValue, averageValue: "30" }
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300   # don't flap: JVMs are expensive to start
      policies: [{ type: Percent, value: 50, periodSeconds: 60 }]
    scaleUp:
      stabilizationWindowSeconds: 30
      policies: [{ type: Percent, value: 100, periodSeconds: 30 }]
```

## 2.6 PodDisruptionBudget

```yaml
# k8s/base/pdb.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: claims-service
  namespace: claims
spec:
  minAvailable: 4        # of 6
  selector:
    matchLabels: { app: claims-service }
# ⟵ TALKING POINT: "a PodDisruptionBudget so VOLUNTARY disruptions like node drains can't
#   take all replicas at once." Note the word voluntary — a PDB does NOT protect you from a
#   node crashing (involuntary). It constrains `kubectl drain`, Cluster Autoscaler scale-down,
#   and managed node group version upgrades.
```

## 2.7 ConfigMap + Secrets Store CSI driver

```yaml
# k8s/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: claims-config
  namespace: claims
data:
  DB_HOST: claims-db.cluster-abc123.ap-south-1.rds.amazonaws.com
  CLAIMS_TOPIC_ARN: arn:aws:sns:ap-south-1:123456789012:claims-events
  NOTIFICATION_QUEUE_URL: https://sqs.ap-south-1.amazonaws.com/123456789012/claims-notifications
  LOG_LEVEL: INFO
```

```yaml
# k8s/base/secretproviderclass.yaml
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: claims-db-spc
  namespace: claims
spec:
  provider: aws
  parameters:
    objects: |
      - objectName: "prod/claims/db"        # ⟵ TALKING POINT: "secrets from AWS Secrets Manager
        objectType: "secretsmanager"        #   via the Secrets Store CSI driver". The pod reads
        jmesPath:                           #   them using its IRSA role — no keys, and rotation
          - path: "username"                #   is handled by Secrets Manager.
            objectAlias: "db-username"
          - path: "password"
            objectAlias: "db-password"
  secretObjects:                            # optionally mirror into a k8s Secret for env vars
    - secretName: claims-db-secret
      type: Opaque
      data:
        - { objectName: db-username, key: username }
        - { objectName: db-password, key: password }
```

## 2.8 Migrations as a Job, not on startup

```yaml
# k8s/base/migration-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: claims-db-migrate-a1b2c3d          # unique per release → immutable, re-runnable audit
  namespace: claims
spec:
  backoffLimit: 2
  template:
    spec:
      restartPolicy: Never
      serviceAccountName: claims-service-sa
      containers:
        - name: liquibase
          image: 123456789012.dkr.ecr.ap-south-1.amazonaws.com/claims-service:a1b2c3d
          command: ["java","-cp","/app/app.jar",
                    "-Dloader.main=liquibase.integration.commandline.Main",
                    "org.springframework.boot.loader.PropertiesLauncher",
                    "--changeLogFile=db/changelog/db.changelog-master.yaml",
                    "--url=jdbc:postgresql://$(DB_HOST):5432/claimsdb",
                    "--username=$(DB_USERNAME)","--password=$(DB_PASSWORD)","update"]
          envFrom:
            - configMapRef: { name: claims-config }
            - secretRef:    { name: claims-db-secret }
# ⟵ TALKING POINT: "Liquibase/Flyway run either on app startup or as a Kubernetes
#   initContainer/Job." Prefer the Job: with 6 pods starting simultaneously they all contend
#   for the DATABASECHANGELOGLOCK row, and a pod killed mid-migration leaves the lock held,
#   which then blocks every subsequent start until you manually clear it.
```

---

# PART 3 — Amazon RDS (PostgreSQL + legacy Oracle)

## 3.1 Terraform: Multi-AZ instance + read replica side by side

Having both in one file is the clearest way to hold the distinction in your head.

```hcl
# terraform/rds.tf

resource "aws_db_subnet_group" "claims" {
  name       = "claims-db-subnets"
  subnet_ids = [var.private_subnet_a, var.private_subnet_b, var.private_subnet_c]
}

resource "aws_db_parameter_group" "claims_pg" {
  name   = "claims-pg15"
  family = "postgres15"

  # ⟵ TALKING POINT: "parameter groups for engine settings"
  parameter { name = "log_min_duration_statement" value = "500" }   # log slow queries > 500ms
  parameter { name = "shared_preload_libraries"  value = "pg_stat_statements"
              apply_method = "pending-reboot" }
  parameter { name = "max_connections"           value = "500"      # default formula is
              apply_method = "pending-reboot" }                     # LEAST({DBInstanceClassMemory/9531392}, 5000)
}

resource "aws_db_instance" "claims_primary" {
  identifier     = "claims-db"
  engine         = "postgres"
  engine_version = "15.5"
  instance_class = "db.r6g.xlarge"

  allocated_storage     = 200
  max_allocated_storage = 1000
  storage_type          = "gp3"        # ⟵ "gp3 for general storage with independently
  iops                  = 12000        #    provisioned IOPS" — decoupled from volume size,
  storage_throughput    = 500          #    unlike gp2 where IOPS scale with GB.

  # ⟵ TALKING POINT #1, THE MOST-ASKED RDS QUESTION.
  #   multi_az = SYNCHRONOUS replication to a DORMANT standby in another AZ.
  #   It is HIGH AVAILABILITY ONLY. It is NOT readable. It does NOTHING for performance.
  #   Confusing this with a read replica is described as "fatal, and extremely common".
  multi_az = true

  db_subnet_group_name   = aws_db_subnet_group.claims.name
  parameter_group_name   = aws_db_parameter_group.claims_pg.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # ⟵ "storage encryption via KMS set AT CREATION TIME and NOT CHANGEABLE afterwards.
  #    You'd have to snapshot and restore into an encrypted instance."
  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn

  # ⟵ "automated backups with point-in-time recovery to ANY SECOND in the retention window"
  backup_retention_period = 30            # insurance = regulatory retention
  backup_window           = "18:00-19:00" # UTC, off-peak for IST business hours
  maintenance_window      = "sun:19:30-sun:20:30"
  copy_tags_to_snapshot   = true
  deletion_protection     = true

  performance_insights_enabled          = true
  performance_insights_retention_period = 731
  monitoring_interval                   = 30
  monitoring_role_arn                   = aws_iam_role.rds_enhanced_monitoring.arn
  enabled_cloudwatch_logs_exports       = ["postgresql", "upgrade"]

  manage_master_user_password = true      # RDS-managed secret in Secrets Manager + rotation
}

# ⟵ TALKING POINT #2: THE READ REPLICA. Completely different purpose.
#   ASYNCHRONOUS. READABLE. Manual promotion only. Non-zero lag.
#   This one exists to keep the actuarial/reporting team's heavy queries off the writer.
resource "aws_db_instance" "claims_reporting_replica" {
  identifier          = "claims-db-reporting"
  replicate_source_db = aws_db_instance.claims_primary.identifier
  instance_class      = "db.r6g.large"
  # NOTE: no multi_az, no backup_retention here — it's a read-scaling tool, NOT an HA tool.
  #       If the primary dies, this replica does NOT take over automatically.
  skip_final_snapshot = true
}
```

### The comparison table, in the words you should say it

| | Multi-AZ standby | Read replica |
|---|---|---|
| Purpose | High availability | Read scaling |
| Replication | **Synchronous** (commit waits for standby) | **Asynchronous** |
| Readable? | **No — dormant** | **Yes** |
| Failover | **Automatic** (DNS CNAME flip) | **Manual promotion** |
| Lag | None | Seconds, possibly more |
| Cost | ~2× (you pay, can't use it) | +1 instance, but usable |

> One-liner: *"Multi-AZ is about surviving an AZ failure and does nothing for performance. Read replicas are about offloading reporting queries and do nothing for HA unless you promote one manually."*

### Routing reads to the replica in Spring

```java
package com.insurer.claims.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * ⟵ TALKING POINT: how you ACTUALLY use a read replica from the app. The replica has a
 * different endpoint, so something has to choose. @Transactional(readOnly=true) sets the
 * TransactionSynchronizationManager flag, and this router reads it.
 *
 * The catch to volunteer: replication lag means read-your-own-writes is NOT guaranteed.
 * Submit a claim then immediately GET it from the replica and you may 404. So: writes and
 * any read-after-write go to the primary; only reporting/search goes to the replica.
 */
public class ReplicaAwareRoutingDataSource extends AbstractRoutingDataSource {

    public enum Route { PRIMARY, REPLICA }

    @Override
    protected Object determineCurrentLookupKey() {
        return org.springframework.transaction.support.TransactionSynchronizationManager
                       .isCurrentTransactionReadOnly()
                ? Route.REPLICA : Route.PRIMARY;
    }
}
```

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(@Qualifier("primaryDs") DataSource primary,
                                 @Qualifier("replicaDs") DataSource replica) {
        var router = new ReplicaAwareRoutingDataSource();
        router.setTargetDataSources(Map.of(
                ReplicaAwareRoutingDataSource.Route.PRIMARY, primary,
                ReplicaAwareRoutingDataSource.Route.REPLICA, replica));
        router.setDefaultTargetDataSource(primary);   // fail SAFE, not fast: default to writer

        // LazyConnectionDataSourceProxy is REQUIRED here. Without it Spring grabs a connection
        // BEFORE the transaction's readOnly flag is set, so routing always picks PRIMARY.
        return new LazyConnectionDataSourceProxy(router);
    }
}
```

```java
@Repository
public interface ClaimSearchRepository extends JpaRepository<Claim, String> {

    @Transactional(readOnly = true)     // ⟵ routes to the replica
    @Query("select c from Claim c where c.policyNumber = :pn and c.claimDate between :from and :to")
    List<Claim> searchForReporting(String pn, LocalDate from, LocalDate to);
}
```

---

## 3.2 Surviving a failover — the client-side work

> *"RDS keeps a DNS CNAME for the writer endpoint. On failover it flips the CNAME to the standby — typically 60–120 seconds. Your endpoint string never changes. But your connection pool is holding TCP connections to a host that just vanished."*

Three defences, and you should be able to name all three:

```java
package com.insurer.claims.config;

import java.security.Security;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DnsCacheConfig {

    /**
     * DEFENCE 1 — JVM DNS TTL.
     * ⟵ TALKING POINT: "the JVM caches DNS FOREVER by default in some configurations,
     *    and this bites people." Specifically: with a SecurityManager installed the default
     *    networkaddress.cache.ttl is -1 (cache forever). Without one it's typically 30s.
     *    Either way, after an RDS failover a stale entry keeps you dialling a dead host.
     *    Also set negative TTL low, or a single NXDOMAIN during the flip gets cached for 10s.
     */
    @PostConstruct
    void hardenDnsCache() {
        Security.setProperty("networkaddress.cache.ttl", "5");
        Security.setProperty("networkaddress.cache.negative.ttl", "1");
    }
}
```

**Defence 2 — pool validation** (already in `application.yml`):
```yaml
hikari:
  validation-timeout: 2000   # Hikari calls Connection.isValid() (JDBC4) before handing out
  max-lifetime: 900000       # a connection; a dead one is evicted and replaced silently
  keepalive-time: 120000
```

**Defence 3 — socket timeout** so a hung socket doesn't pin a Tomcat thread:
```yaml
hikari:
  data-source-properties:
    socketTimeout: 30       # seconds — PostgreSQL JDBC. Without it you inherit the OS
    connectTimeout: 5       # TCP retransmit timeout, which can be ~15 minutes.
    tcpKeepAlive: true
```

**Defence 4 (optional but great to mention) — RDS Proxy:**

```hcl
resource "aws_db_proxy" "claims" {
  name                   = "claims-db-proxy"
  engine_family          = "POSTGRESQL"
  role_arn               = aws_iam_role.rds_proxy.arn
  vpc_subnet_ids         = [var.private_subnet_a, var.private_subnet_b, var.private_subnet_c]
  require_tls            = true
  idle_client_timeout    = 1800

  auth {
    auth_scheme = "SECRETS"
    iam_auth    = "REQUIRED"
    secret_arn  = aws_secretsmanager_secret.claims_db.arn
  }
}
# ⟵ TALKING POINT: RDS Proxy "MULTIPLEXES and also SPEEDS UP FAILOVER by holding the client
#   connections open while it reconnects behind the scenes." Reported failover improvement is
#   up to ~66% faster from the application's point of view, because the app's TCP connection
#   to the proxy never breaks — only the proxy's connection to the DB does.
```

---

## 3.3 Connection-pool arithmetic — the numbers to say out loud

```
  12 pods × maximumPoolSize 20  = 240 connections
  40 pods × maximumPoolSize 20  = 800 connections   ← exhausts a db.r5.large

  db.r5.large → max_connections ≈ LEAST(DBInstanceClassMemory / 9531392, 5000) ≈ 1600
  BUT: every PostgreSQL connection is a SEPARATE OS PROCESS (~5–10 MB RSS each),
       so you hit memory pressure long before you hit the configured ceiling.

  Fix A: maximumPoolSize 10 → 40 pods × 10 = 400. "Connections should be scarce and
         fast, not numerous." Little's Law: throughput = pool_size / avg_hold_time.
         10 connections holding for 5 ms = 2,000 queries/sec per pod. Plenty.
  Fix B: RDS Proxy — multiplexes N client connections onto a smaller DB connection pool.
  Fix C: cap HPA maxReplicas (see §2.5) — the autoscaler is a DB risk, not just a cost risk.
```

Useful queries to be able to write:

```sql
-- who is actually connected, and doing what
SELECT usename, application_name, state, count(*)
  FROM pg_stat_activity
 GROUP BY 1,2,3
 ORDER BY count DESC;

-- how close are we to the ceiling
SELECT (SELECT count(*) FROM pg_stat_activity) AS in_use,
       current_setting('max_connections')::int  AS ceiling;
```

Alarm on `DatabaseConnections` (§6.5).

---

## 3.4 Expand / contract migrations — Liquibase changesets

> *"You never rename in one step... Every intermediate state is compatible with both the old and new application version, which matters because during a rolling deploy BOTH VERSIONS ARE RUNNING SIMULTANEOUSLY."*

Scenario: rename `claims.claimant_name` → `claims.policy_holder_name`.

```yaml
# src/main/resources/db/changelog/db.changelog-master.yaml
databaseChangeLog:
  - include: { file: db/changelog/changes/001-baseline.yaml }
  - include: { file: db/changelog/changes/002-expand-add-column.yaml }      # release N
  - include: { file: db/changelog/changes/003-backfill.yaml }               # release N
  - include: { file: db/changelog/changes/004-add-not-null.yaml }           # release N+1
  - include: { file: db/changelog/changes/005-contract-drop-old.yaml }      # release N+2
```

### Step 1 — EXPAND (release N): add the new column, nullable

```yaml
# 002-expand-add-column.yaml
databaseChangeLog:
  - changeSet:
      id: 002-add-policy-holder-name
      author: rahul
      comment: >
        EXPAND phase. Nullable + no default, so this is a metadata-only change in
        PostgreSQL 11+ and does NOT rewrite the 40M-row table or hold a long ACCESS
        EXCLUSIVE lock. Old application version is unaffected — it never sees the column.
      changes:
        - addColumn:
            tableName: claims
            columns:
              - column: { name: policy_holder_name, type: VARCHAR(200), constraints: { nullable: true } }
      rollback:
        - dropColumn: { tableName: claims, columnName: policy_holder_name }
```

Application code for release N — **dual write, read old**:

```java
// ⟵ TALKING POINT: "deploy code that WRITES TO BOTH and READS FROM THE OLD"
@Entity
public class Claim {
    @Column(name = "claimant_name")        private String claimantName;       // old
    @Column(name = "policy_holder_name")   private String policyHolderName;   // new

    public void setPolicyHolder(String name) {
        this.claimantName     = name;   // keep the old column correct for the old pods
        this.policyHolderName = name;   // populate the new one for the next release
    }

    public String getPolicyHolder() {
        return claimantName;            // still the source of truth in release N
    }
}
```

### Step 2 — BACKFILL, in batches

```yaml
# 003-backfill.yaml
databaseChangeLog:
  - changeSet:
      id: 003-backfill-policy-holder-name
      author: rahul
      comment: >
        Batched backfill. A single UPDATE over 40M rows creates one enormous transaction,
        bloats the WAL, blocks autovacuum, and can stall replication to the read replica.
        Loop in chunks instead.
      changes:
        - sql:
            splitStatements: false
            sql: |
              DO $$
              DECLARE
                rows_updated INT;
              BEGIN
                LOOP
                  UPDATE claims
                     SET policy_holder_name = claimant_name
                   WHERE claim_id IN (
                         SELECT claim_id FROM claims
                          WHERE policy_holder_name IS NULL
                            AND claimant_name IS NOT NULL
                          LIMIT 10000
                         );
                  GET DIAGNOSTICS rows_updated = ROW_COUNT;
                  EXIT WHEN rows_updated = 0;
                  COMMIT;                    -- release locks between batches
                  PERFORM pg_sleep(0.1);     -- let replication catch up
                END LOOP;
              END $$;
      rollback: { empty: {} }
```

### Step 3 — release N+1: read new, still write both

```java
public String getPolicyHolder() {
    return policyHolderName;    // ⟵ "deploy code that READS THE NEW"
}
```

### Step 4 — CONTRACT (release N+2): drop the old column

```yaml
# 005-contract-drop-old.yaml
databaseChangeLog:
  - changeSet:
      id: 005-drop-claimant-name
      author: rahul
      comment: >
        CONTRACT phase, a SEPARATE RELEASE. Safe only because no running application
        version references claimant_name any more. Irreversible in practice — the
        rollback recreates the column but not the data, so PITR is the real rollback.
      preConditions:
        - onFail: MARK_RAN
        - sqlCheck:
            expectedResult: 0
            sql: SELECT count(*) FROM claims WHERE policy_holder_name IS NULL
      changes:
        - dropColumn: { tableName: claims, columnName: claimant_name }
```

**The `DATABASECHANGELOG` table** — know what it is: Liquibase records every applied changeset with its `id`, `author`, `filename` and an MD5 checksum. Edit an already-applied changeset and the checksum mismatch fails the next run — which is why you *add* changesets rather than editing them. `DATABASECHANGELOGLOCK` is the single-row advisory lock that stops two runners colliding (and the reason for the Job in §2.8).

---

## 3.5 The query optimisation story — with the actual artefacts

> *"Our claims search endpoint had p99 around 4 seconds..."*

**Before:**

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM claims
 WHERE policy_number = 'POL-10023481'
   AND claim_date BETWEEN '2023-01-01' AND '2023-12-31'
 ORDER BY claim_date DESC
 LIMIT 50;
```

```
Limit  (cost=1284500.12..1284500.24 rows=50 width=142)
        (actual time=3980.221..3980.244 rows=50 loops=1)
  ->  Sort  (cost=1284500.12..1284512.98 rows=5144 width=142)
            (actual time=3980.219..3980.231 rows=50 loops=1)
        Sort Key: claim_date DESC
        Sort Method: top-N heapsort  Memory: 42kB           ← ⟵ an explicit SORT
        ->  Seq Scan on claims  (cost=0.00..1284328.00 rows=5144 width=142)
                                 (actual time=12.401..3971.882 rows=5102 loops=1)
              Filter: ((policy_number = 'POL-10023481') AND (claim_date >= ...))
              Rows Removed by Filter: 39994898              ← ⟵ SEQ SCAN over 40M rows
              Buffers: shared read=982431
Execution Time: 3980.402 ms
```

**The fix — a composite index that matches BOTH the filter and the sort order:**

```yaml
# 006-composite-index.yaml
databaseChangeLog:
  - changeSet:
      id: 006-idx-claims-policy-date
      author: rahul
      runInTransaction: false      # ⟵ REQUIRED: CREATE INDEX CONCURRENTLY cannot run inside
      comment: >                   #    a transaction block. Liquibase wraps changesets in one
        CONCURRENTLY so the 40M-row table is never locked against writes. Takes longer and
        needs two table scans, but does not take an outage.
      changes:
        - sql:
            sql: |
              CREATE INDEX CONCURRENTLY idx_claims_policy_date
                ON claims (policy_number, claim_date DESC);
      rollback:
        - sql: { sql: "DROP INDEX CONCURRENTLY IF EXISTS idx_claims_policy_date" }
```

**After:**

```
Limit  (cost=0.56..104.22 rows=50 width=142) (actual time=0.061..0.191 rows=50 loops=1)
  ->  Index Scan using idx_claims_policy_date on claims
        (cost=0.56..10682.11 rows=5144 width=142) (actual time=0.059..0.178 rows=50 loops=1)
        Index Cond: ((policy_number = 'POL-10023481') AND (claim_date >= ...))
        Buffers: shared hit=54
Execution Time: 0.238 ms        ← ⟵ NO Sort node at all. p99 endpoint latency 4s → ~200ms.
```

**Why this specific index and not `(claim_date, policy_number)`?**
B-tree indexes are ordered left-to-right. An equality predicate must come first so the range/sort column is contiguous underneath it. With `(policy_number, claim_date DESC)` the rows for one policy are already stored in descending date order, so PostgreSQL walks the index and stops after 50 — **the sort disappears entirely**. Reverse the columns and the leading column is a range predicate, which prevents efficient use of the second.

> *"The single-column index on `policy_number` wasn't selective enough"* — with 40M claims across ~2M policies, one policy is ~20 rows... but a **corporate** policy had 5,000, and every one of them had to be fetched and sorted. The composite index removes the sort for all of them.

---

## 3.6 The N+1 problem — the other half of the performance story

**The bug:**

```java
// ⟵ TALKING POINT: "fetching 50 claims triggered 50 SEPARATE QUERIES for claimant details"
//   1 query for the claims + 50 lazy-load queries = 51 round trips. At 2ms RTT that is
//   ~100ms of pure network wait that no index will ever fix.
List<Claim> claims = claimRepository.findByPolicyNumber(policyNumber);   // 1 query
for (Claim c : claims) {
    c.getClaimants().size();                                             // N queries
}
```

**The fix:**

```java
public interface ClaimRepository extends JpaRepository<Claim, String> {

    /**
     * ⟵ TALKING POINT: "fixed with a JOIN FETCH in the JPQL".
     * DISTINCT is needed because the join multiplies rows; Hibernate 6 de-duplicates
     * entities automatically, but on Hibernate 5 (Boot 2.7) you still need it.
     */
    @Query("""
           select distinct c
             from Claim c
             left join fetch c.claimants
            where c.policyNumber = :policyNumber
              and c.claimDate between :from and :to
            order by c.claimDate desc
           """)                                    // ⟵ text block: Java 15+, readable SQL
    List<Claim> findWithClaimants(@Param("policyNumber") String policyNumber,
                                  @Param("from") LocalDate from,
                                  @Param("to")   LocalDate to);
}
```

**Two caveats a senior candidate volunteers:**

1. **`JOIN FETCH` + `Pageable` is a trap.** Hibernate cannot paginate in SQL once you fetch a collection, so it logs `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory` and pulls the **entire** result set into heap before paging it. For paginated screens use `@EntityGraph` on a two-query strategy, or fetch IDs first then entities.

2. **`JOIN FETCH` on two collections throws** `MultipleBagFetchException` — you get a Cartesian product. Fix: make one a `Set`, or use `@BatchSize(size = 50)` so Hibernate loads the second collection in one `IN (...)` query rather than N.

```java
@OneToMany(mappedBy = "claim", fetch = FetchType.LAZY)
@BatchSize(size = 50)      // 51 queries → 2 queries, without touching the JPQL
private List<ClaimDocument> documents = new ArrayList<>();
```

Catch it in tests rather than production:

```java
@DataJpaTest
class ClaimRepositoryNPlusOneTest {

    @Autowired ClaimRepository repository;
    @Autowired EntityManagerFactory emf;

    @Test
    void findWithClaimants_issuesExactlyOneQuery() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        List<Claim> claims = repository.findWithClaimants(
                "POL-10023481", LocalDate.of(2023,1,1), LocalDate.of(2023,12,31));
        claims.forEach(c -> c.getClaimants().size());   // would trigger N+1 if lazy

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);   // regression guard
    }
}
```

---

## 3.7 Oracle + PL/SQL — calling legacy stored procedures

> *"The legacy policy administration system was Oracle-based, with decades of business logic in stored procedures — premium calculation, regulatory validation — that nobody was going to rewrite."*

### The procedure you are calling

```sql
CREATE OR REPLACE PACKAGE policy_admin_pkg AS

  PROCEDURE validate_policy(
      p_policy_number  IN  VARCHAR2,
      p_incident_date  IN  DATE,
      p_is_valid       OUT NUMBER,
      p_reason_code    OUT VARCHAR2,
      p_holder_id      OUT VARCHAR2,
      p_coverages      OUT SYS_REFCURSOR      -- ⟵ result set as an OUT param, very Oracle
  );

  FUNCTION calculate_premium(
      p_policy_number IN VARCHAR2,
      p_risk_score    IN NUMBER
  ) RETURN NUMBER;

END policy_admin_pkg;
```

### Calling it with `SimpleJdbcCall`

```java
package com.insurer.claims.legacy;

import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;

@Component
public class PolicyGateway {

    private final SimpleJdbcCall validatePolicyCall;
    private final SimpleJdbcCall calculatePremiumCall;

    public PolicyGateway(@Qualifier("oracleDataSource") DataSource oracleDataSource) {

        // ⟵ TALKING POINT: "From Spring you call these with SimpleJdbcCall ... mapping OUT
        //    parameters and SYS_REFCURSOR results."
        this.validatePolicyCall = new SimpleJdbcCall(oracleDataSource)
                .withCatalogName("POLICY_ADMIN_PKG")     // the PACKAGE
                .withProcedureName("VALIDATE_POLICY")
                .withoutProcedureColumnMetaDataAccess()  // ⟵ IMPORTANT: skip the metadata
                                                         //    round-trip. Without this, Spring
                                                         //    queries ALL_ARGUMENTS on EVERY
                                                         //    call — a measurable latency tax,
                                                         //    and it often gets package
                                                         //    overloads wrong.
                .declareParameters(
                        new SqlParameter("P_POLICY_NUMBER", Types.VARCHAR),
                        new SqlParameter("P_INCIDENT_DATE", Types.DATE),
                        new SqlOutParameter("P_IS_VALID",    Types.NUMERIC),
                        new SqlOutParameter("P_REASON_CODE", Types.VARCHAR),
                        new SqlOutParameter("P_HOLDER_ID",   Types.VARCHAR),
                        // SYS_REFCURSOR → a RowMapper, streamed as a List
                        new SqlOutParameter("P_COVERAGES", OracleTypes.CURSOR,
                                (RowMapper<Coverage>) (rs, i) -> new Coverage(
                                        rs.getString("COVERAGE_CODE"),
                                        rs.getBigDecimal("SUM_INSURED"),
                                        rs.getBigDecimal("DEDUCTIBLE")))
                );

        this.calculatePremiumCall = new SimpleJdbcCall(oracleDataSource)
                .withCatalogName("POLICY_ADMIN_PKG")
                .withFunctionName("CALCULATE_PREMIUM")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter("RETURN", Types.NUMERIC),
                        new SqlParameter("P_POLICY_NUMBER", Types.VARCHAR),
                        new SqlParameter("P_RISK_SCORE",    Types.NUMERIC));
    }

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "policyAdmin", fallbackMethod = "validateFallback")
    @Bulkhead(name = "policyAdmin")        // ⟵ legacy Oracle is the slow dependency; cap it
    public PolicyValidation validatePolicy(String policyNumber, LocalDate incidentDate) {

        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("P_POLICY_NUMBER", policyNumber)
                .addValue("P_INCIDENT_DATE", java.sql.Date.valueOf(incidentDate));

        Map<String, Object> out = validatePolicyCall.execute(in);

        return new PolicyValidation(
                ((Number) out.get("P_IS_VALID")).intValue() == 1,
                (String) out.get("P_REASON_CODE"),
                (String) out.get("P_HOLDER_ID"),
                (List<Coverage>) out.getOrDefault("P_COVERAGES", List.of()));
    }

    private PolicyValidation validateFallback(String policyNumber, LocalDate d, Throwable t) {
        // fail CLOSED for a financial validation — never auto-approve because Oracle is down
        throw new PolicyAdminUnavailableException(policyNumber, t);
    }

    public BigDecimal calculatePremium(String policyNumber, BigDecimal riskScore) {
        return calculatePremiumCall.executeFunction(BigDecimal.class,
                new MapSqlParameterSource()
                        .addValue("P_POLICY_NUMBER", policyNumber)
                        .addValue("P_RISK_SCORE", riskScore));
    }
}

public record Coverage(String code, BigDecimal sumInsured, BigDecimal deductible) {}
public record PolicyValidation(boolean valid, String reasonCode,
                               String policyHolderId, List<Coverage> coverages) {}
```

### Two DataSources side by side

```java
@Configuration
public class DualDataSourceConfig {

    @Primary
    @Bean("primaryDs")
    @ConfigurationProperties("spring.datasource")
    public DataSource postgresDataSource() { return DataSourceBuilder.create().build(); }

    /**
     * ⟵ Legacy Oracle. Note the DELIBERATELY TINY pool: the mainframe-era policy admin DB
     * has a hard session licence limit and we are one of a dozen consumers. This is also
     * why the bulkhead above matters — 10 app threads must not become 10 Oracle sessions
     * each waiting 8 seconds.
     */
    @Bean("oracleDataSource")
    @ConfigurationProperties("app.oracle")
    public DataSource oracleDataSource() {
        HikariDataSource ds = DataSourceBuilder.create().type(HikariDataSource.class).build();
        ds.setMaximumPoolSize(5);
        ds.setConnectionTimeout(2000);
        ds.addDataSourceProperty("oracle.jdbc.ReadTimeout", "10000");
        ds.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "3000");
        return ds;
    }
}
```

```yaml
# application.yml additions
app:
  oracle:
    jdbc-url: jdbc:oracle:thin:@//policy-admin.internal:1521/POLPRD
    username: ${ORACLE_USER}
    password: ${ORACLE_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
```

> **Distributed-transaction trap they may set:** *"You write to PostgreSQL and call Oracle — is that one transaction?"* **No.** Two resource managers means XA/2PC, which we deliberately avoided (poor performance, painful recovery, and the legacy DBAs would not enable it). The Oracle call is a **read-only validation performed before** the PostgreSQL write, so there is nothing to roll back. If it had been a write, the answer is a saga with a compensating action, not XA.

---

# PART 4 — Amazon SQS

(Following the explanation's order: SQS depth first, then SNS. The publisher code lives in §5.2 — flip between the two.)

## 4.1 Terraform: three queues, three DLQs, one redrive policy each

```hcl
# terraform/messaging.tf

locals {
  consumers = ["documents", "notifications", "fraud"]
}

# ---- Dead-letter queues -------------------------------------------------------
resource "aws_sqs_queue" "dlq" {
  for_each = toset(local.consumers)
  name     = "claims-${each.key}-dlq"

  # ⟵ TALKING POINT: "the DLQ's retention should be LONGER than the source queue's so you
  #    have time to investigate." Source = 4 days, DLQ = 14 days (the maximum).
  message_retention_seconds = 1209600      # 14 days
  kms_master_key_id         = aws_kms_key.sqs.id
}

# ---- Source queues ------------------------------------------------------------
resource "aws_sqs_queue" "consumer" {
  for_each = toset(local.consumers)
  name     = "claims-${each.key}"

  # ⟵ TALKING POINT: "default retention 4 days, maximum 14"
  message_retention_seconds = 345600       # 4 days

  # ⟵ TALKING POINT — THE MOST-GRILLED SQS CONCEPT.
  #    Message is NOT deleted on receive; it becomes INVISIBLE for this long.
  #    Rule: set it ABOVE your p99 processing time. Our fraud model p99 is 45s → 90s here.
  #    The failure mode they will describe: timeout 30s, processing takes 45s → the message
  #    reappears at 30s, a SECOND consumer picks it up, and you process it twice while the
  #    first is still working.
  visibility_timeout_seconds = 90

  # ⟵ TALKING POINT: LONG POLLING. Short polling (0) returns immediately even when the queue
  #    is empty — you pay for every empty ReceiveMessage AND you add latency, because short
  #    polling only samples a subset of servers so a message may not be returned even when
  #    one exists. 20 is the maximum and the correct default. ALWAYS use it.
  receive_wait_time_seconds = 20

  delay_seconds = 0     # ⟵ delay queues can postpone NEW messages up to 15 minutes (900)

  # ⟵ TALKING POINT: THE REDRIVE POLICY. After maxReceiveCount receives WITHOUT a
  #    DeleteMessage, SQS moves the message to the DLQ. This is what stops a poison-pill
  #    message from being retried forever and blocking the queue.
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = 5
  })

  kms_master_key_id                 = aws_kms_key.sqs.id
  kms_data_key_reuse_period_seconds = 300
}

# ---- Allow the DLQ to be redriven back to the source once you've fixed the bug --
resource "aws_sqs_queue_redrive_allow_policy" "dlq" {
  for_each  = toset(local.consumers)
  queue_url = aws_sqs_queue.dlq[each.key].id
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.consumer[each.key].arn]
  })
}

# ---- The resource policy that lets SNS write into the queue -------------------
# ⟵ TALKING POINT: "The SQS queue needs a resource policy allowing sns.amazonaws.com to
#    SendMessage, conditioned on the topic ARN — a common 'why isn't anything arriving'
#    debugging story." Miss this and SNS reports a successful publish, delivery silently
#    fails, and nothing lands in the queue. Check the SNS delivery-status logs.
resource "aws_sqs_queue_policy" "allow_sns" {
  for_each  = toset(local.consumers)
  queue_url = aws_sqs_queue.consumer[each.key].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "sns.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.consumer[each.key].arn
      Condition = {
        ArnEquals = { "aws:SourceArn" = aws_sns_topic.claims_events.arn }
      }   # ← the condition is the confused-deputy protection. Without it ANY SNS topic
    }]    #   in ANY account could publish into your queue.
  })
}
```

### FIFO, when you actually need it

```hcl
# ⟵ TALKING POINT: "Default to Standard and design for idempotency; reach for FIFO only when
#    order genuinely matters, like a sequence of STATUS TRANSITIONS ON THE SAME CLAIM."
resource "aws_sqs_queue" "claim_status_fifo" {
  name                        = "claim-status-transitions.fifo"   # .fifo suffix is MANDATORY
  fifo_queue                  = true
  content_based_deduplication = false   # we supply an explicit MessageDeduplicationId
  deduplication_scope         = "messageGroup"
  fifo_throughput_limit       = "perMessageGroupId"   # ⟵ high-throughput mode: 3,000 → 30,000 TPS
  visibility_timeout_seconds  = 60
}
```

| | Standard | FIFO |
|---|---|---|
| Throughput | Nearly unlimited | 300 TPS/API action, 3,000 with batching (higher with high-throughput mode) |
| Ordering | Best-effort | Strict **within a message group** |
| Delivery | **At-least-once** | Exactly-once *processing* within the 5-min dedup window |
| Cost | Lower | ~20% higher |

> **The trap:** never say *"exactly once"* about a Standard queue — it is **at-least-once**, so duplicates **will** happen, not *might*. And even FIFO's guarantee is about deduplication **within a 5-minute window**, not a universal exactly-once promise. Redeliver the same `MessageDeduplicationId` at minute six and it goes through.
>
> **Head-of-line blocking with FIFO:** one poison message in a message group blocks *every* later message in that group until it hits the DLQ. Choose `MessageGroupId = claimId` (thousands of independent groups), never a constant.

---

## 4.2 The consumer — visibility timeout, heartbeat, idempotency

```java
package com.insurer.claims.messaging;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.*;
import java.util.concurrent.*;

@Component
public class NotificationQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationQueueConsumer.class);

    private final SqsClient sqs;
    private final NotificationProcessor processor;
    private final ScheduledExecutorService heartbeats =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "sqs-visibility-heartbeat");
                t.setDaemon(true);
                return t;
            });

    @Value("${app.sqs.notification-queue-url}")
    private String queueUrl;

    public NotificationQueueConsumer(SqsClient sqs, NotificationProcessor processor) {
        this.sqs = sqs;
        this.processor = processor;
    }

    @Scheduled(fixedDelay = 100)
    public void poll() {

        ReceiveMessageResponse response = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)          // batch: 1 API call instead of 10

                // ⟵ TALKING POINT: LONG POLLING. Holds the connection open until a message
                //    arrives or 20s elapses. Short polling = constant empty responses that
                //    you pay for and that add latency.
                .waitTimeSeconds(20)

                // ⟵ we must ask for these explicitly or they don't come back
                .messageAttributeNames("All")
                .messageSystemAttributeNames(
                        MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                        MessageSystemAttributeName.AWS_TRACE_HEADER)   // ⟵ X-Ray, §7.6
                .build());

        for (Message message : response.messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        // The heartbeat: extend the invisibility window while we're still working.
        // ⟵ TALKING POINT: "or by calling ChangeMessageVisibility as a HEARTBEAT to extend it
        //    during long work." This is the alternative to just setting a huge timeout —
        //    better, because a crashed consumer's message becomes visible again quickly
        //    instead of being stuck invisible for the full padded duration.
        ScheduledFuture<?> heartbeat = heartbeats.scheduleAtFixedRate(
                () -> extendVisibility(message, 90), 45, 45, TimeUnit.SECONDS);

        Segment segment = AWSXRay.beginSegment("notification-worker");
        try {
            int receiveCount = Integer.parseInt(message.attributes()
                    .getOrDefault(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "1"));

            if (receiveCount > 1) {
                // Not an error — at-least-once means this is NORMAL. Log it, don't panic.
                log.warn("Redelivery detected, receiveCount={}", receiveCount);
            }

            processor.process(message.body());          // idempotent — see §4.3

            // ⟵ TALKING POINT: "The consumer must EXPLICITLY call DeleteMessage after
            //    successful processing." Receiving does not remove anything. If we crash
            //    before this line, the visibility timeout expires and someone else retries.
            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());

        } catch (PoisonMessageException e) {
            // Permanently unprocessable — don't burn 5 receives waiting for the redrive
            // policy. Shortcut it to the DLQ by setting visibility to 0 repeatedly, or
            // (better) send it explicitly and delete. Here: let the redrive policy work,
            // but make it fast.
            log.error("Poison message, releasing for redrive", e);
            extendVisibility(message, 0);

        } catch (Exception e) {
            // ⟵ CRITICAL: do NOT delete. Let the visibility timeout expire so SQS redelivers.
            //    After maxReceiveCount=5 receives, SQS moves it to the DLQ automatically.
            log.error("Processing failed, message will be redelivered", e);

        } finally {
            heartbeat.cancel(true);
            AWSXRay.endSegment();
        }
    }

    private void extendVisibility(Message message, int seconds) {
        try {
            sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .visibilityTimeout(seconds)
                    .build());
        } catch (ReceiptHandleIsInvalidException e) {
            // the message already went back to the queue — nothing to extend
            log.debug("Receipt handle expired during heartbeat");
        }
    }
}
```

### The Spring Cloud AWS alternative (less code, same semantics)

```java
package com.insurer.claims.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    /**
     * ⟵ Same three concepts, declarative:
     *    - pollTimeoutSeconds = long polling
     *    - messageVisibility  = visibility timeout override for this listener
     *    - acknowledgementMode MANUAL = you control DeleteMessage
     * Worth knowing both: interviewers ask "did you hand-roll the poller or use a framework?"
     */
    @SqsListener(
            value = "${app.sqs.notification-queue-url}",
            pollTimeoutSeconds = "20",
            messageVisibilitySeconds = "90",
            maxConcurrentMessages = "10",
            acknowledgementMode = "MANUAL")
    public void onMessage(ClaimSubmittedEvent event, Acknowledgement ack) {
        notificationProcessor.process(event);
        ack.acknowledge();          // == DeleteMessage
    }
}
```

---

## 4.3 Idempotency — the answer to "how did you make sure a customer didn't get two emails"

```sql
-- Liquibase changeset 007-processed-events.sql
CREATE TABLE processed_events (
    event_id      VARCHAR(64)  NOT NULL,
    consumer_name VARCHAR(64)  NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- ⟵ TALKING POINT: the UNIQUE CONSTRAINT is the whole mechanism. It's not a SELECT-then-
    --    INSERT check (which races between two concurrent consumers) — it's the DATABASE
    --    enforcing uniqueness atomically. Composite key so the same event can be processed
    --    once by EACH consumer.
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer_name)
);

CREATE INDEX idx_processed_events_at ON processed_events (processed_at);
-- housekeeping: delete rows older than the queue retention + margin, else it grows forever
```

```java
package com.insurer.claims.messaging;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessor {

    private static final String CONSUMER = "notification-worker";

    private final ProcessedEventRepository processedEvents;
    private final EmailGateway emailGateway;
    private final NotificationAuditRepository audit;

    /**
     * ⟵ TALKING POINT — the exact pattern from the explanation:
     *    "every event carries a unique eventId. The consumer inserts that ID into a
     *     processed_events table with a unique constraint INSIDE THE SAME TRANSACTION as
     *     the business write. A duplicate throws a constraint violation, you catch it,
     *     delete the message, and move on."
     *
     * The "same transaction" part is what makes it correct. If you insert the marker in its
     * own transaction and then do the business write, a crash in between means the event is
     * marked processed but never actually processed — silently dropped.
     */
    @Transactional
    public void process(ClaimSubmittedEvent event) {
        try {
            processedEvents.insert(event.eventId(), CONSUMER);   // ← may throw
        } catch (DuplicateKeyException duplicate) {
            log.info("Duplicate event {} ignored — already processed", event.eventId());
            return;    // caller then calls DeleteMessage. Idempotent: no second email.
        }

        // business write — SAME transaction as the marker insert
        audit.recordNotificationQueued(event.claimId());

        // ⟵ The honest caveat to volunteer: the EMAIL SEND is a side effect on an external
        //    system and is NOT transactional. If the send succeeds and the commit then
        //    fails, we will retry and send twice. Options: (a) send AFTER commit via
        //    TransactionSynchronization and accept at-least-once at the SMTP layer,
        //    (b) pass the eventId as SES's idempotency/message-dedup key. We did (b).
        emailGateway.sendClaimConfirmation(event.claimId(), event.policyHolderEmail(),
                                           /* idempotencyKey */ event.eventId());
    }
}
```

```java
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    /**
     * Native INSERT rather than save(), because JPA's save() does a SELECT first
     * (merge semantics) and that reintroduces the read-then-write race.
     */
    @Modifying
    @Query(value = "INSERT INTO processed_events (event_id, consumer_name) VALUES (:id, :consumer)",
           nativeQuery = true)
    void insert(@Param("id") String id, @Param("consumer") String consumer);
}
```

> **Variant worth mentioning:** `INSERT ... ON CONFLICT DO NOTHING RETURNING *` gives you the same result in one statement without an exception. Nice, but the exception version reads more clearly and exceptions on a duplicate path are rare enough not to matter.

---

## 4.4 Large payloads and other numbers

```java
// ⟵ TALKING POINT: "Maximum message size 256 KB — for larger payloads use the Extended
//    Client Library, which stores the body in S3 and puts a POINTER in the message."
//    Claim documents (scanned FIRs, photos) blow past 256 KB immediately.
@Bean
public AmazonSQSExtendedClient extendedSqsClient(AmazonSQS sqs, AmazonS3 s3) {
    ExtendedClientConfiguration config = new ExtendedClientConfiguration()
            .withPayloadSupportEnabled(s3, "claims-large-payloads")
            .withAlwaysThroughS3(false)          // only offload when actually > threshold
            .withPayloadSizeThreshold(200 * 1024);
    return new AmazonSQSExtendedClient(sqs, config);
}
```

**Numbers to have ready:**

| Property | Value |
|---|---|
| Default retention | 4 days |
| Max retention | 14 days |
| Max message size | 256 KB |
| Max long-poll wait | 20 seconds |
| Max visibility timeout | 12 hours |
| Max delay (delay queue) | 15 minutes |
| Batch size (send/receive/delete) | 10 |
| FIFO dedup window | 5 minutes |

---

## 4.5 Why SQS and not Kafka

> *"SQS is a fully managed work queue: no brokers, no partitions, no consumer group rebalancing, scales automatically. But messages are DELETED after consumption — no replay, no event log, no multiple independent consumer groups reading the same stream at their own offsets. If you need to replay six months of events into a new service, that's Kafka or Kinesis. We needed decoupled task distribution, so SQS was the right cost/complexity trade."*

| | SQS | Kafka / Kinesis |
|---|---|---|
| Model | Work queue — message deleted on ack | Append-only log — retained by policy |
| Replay | ✗ | ✓ (seek to offset / timestamp) |
| Multiple independent consumers | Needs SNS fan-out to N queues | Native: N consumer groups, own offsets |
| Ordering | Best-effort (Standard) | Per-partition |
| Ops burden | Zero | Brokers, partitions, rebalancing, ZK/KRaft |
| Scaling | Automatic | You size partitions |
| Per-message cost | Higher | Much lower at volume |

**Say what would have changed your mind:** *"If the fraud team had needed to retrain on six months of historical claim events, or if we'd wanted event sourcing as the system of record, SQS would have been the wrong choice — you can't replay what you've deleted. We had three consumers doing independent, forward-only work, and no replay requirement, so the operational simplicity won."*

---

# PART 5 — Amazon SNS

## 5.1 Terraform: topic, subscriptions, and the filter policies

```hcl
# terraform/messaging.tf (continued)

resource "aws_sns_topic" "claims_events" {
  name              = "claims-events"
  kms_master_key_id = aws_kms_key.sns.id

  # ⟵ Delivery status logging — this is how you debug "SNS says success but nothing arrived"
  sqs_success_feedback_role_arn    = aws_iam_role.sns_feedback.arn
  sqs_success_feedback_sample_rate = 5
  sqs_failure_feedback_role_arn    = aws_iam_role.sns_feedback.arn
}

# ---- Subscription 1: documents — ALL claim types ------------------------------
resource "aws_sns_topic_subscription" "documents" {
  topic_arn = aws_sns_topic.claims_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.consumer["documents"].arn

  # ⟵ TALKING POINT: RAW MESSAGE DELIVERY.
  #    By DEFAULT SNS wraps your payload in a JSON envelope: Type, MessageId, TopicArn,
  #    Timestamp, Signature, SignatureVersion, UnsubscribeURL, and your body as a STRING
  #    inside "Message". A consumer expecting the bare payload gets a parse error.
  #    Raw delivery strips the envelope — BUT you then lose the envelope's copy of the
  #    message attributes and metadata, and if this queue is fed by multiple topics you
  #    can no longer tell which topic a message came from. Know WHY you chose what you chose.
  raw_message_delivery = true

  redrive_policy = jsonencode({          # ⟵ "Subscriptions can have their own DLQs"
    deadLetterTargetArn = aws_sqs_queue.subscription_dlq.arn
  })
}

# ---- Subscription 2: notifications — ALL types, envelope kept ------------------
resource "aws_sns_topic_subscription" "notifications" {
  topic_arn            = aws_sns_topic.claims_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.consumer["notifications"].arn
  raw_message_delivery = false          # this consumer reads the envelope's Timestamp
}

# ---- Subscription 3: fraud — MOTOR + PROPERTY only, high value ------------------
resource "aws_sns_topic_subscription" "fraud" {
  topic_arn            = aws_sns_topic.claims_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.consumer["fraud"].arn
  raw_message_delivery = true

  # ⟵ TALKING POINT: MESSAGE FILTERING — "mention this UNPROMPTED, it's a differentiator."
  #    SNS evaluates the policy against the message ATTRIBUTES and only delivers matches.
  #    Without it every consumer receives everything and discards most of it — and you pay
  #    THREE TIMES: the SNS delivery, the SQS receive, and the wasted consumer compute.
  filter_policy = jsonencode({
    claimType     = ["AUTO", "PROPERTY"]
    claimedAmount = [{ numeric = [">=", 100000] }]     # only score high-value claims
    region        = [{ "anything-but" = ["TEST"] }]
  })
  filter_policy_scope = "MessageAttributes"   # the default; the alternative is MessageBody
}
```

### Filter policy operators worth knowing

```json
{
  "claimType":     ["AUTO", "PROPERTY"],
  "claimedAmount": [{ "numeric": [">=", 100000, "<", 5000000] }],
  "policyNumber":  [{ "prefix": "POL-2024" }],
  "region":        [{ "anything-but": ["TEST", "SANDBOX"] }],
  "assessorId":    [{ "exists": false }],
  "channel":       [{ "suffix": "-mobile" }]
}
```

Filtering on the **body** instead of attributes (newer, avoids duplicating fields into attributes):

```hcl
filter_policy_scope = "MessageBody"
filter_policy = jsonencode({
  claim = { type = ["AUTO"], amount = [{ numeric = [">=", 100000] }] }
})
```

---

## 5.2 The publisher

```java
package com.insurer.claims.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.Map;

@Component
public class ClaimEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClaimEventPublisher.class);

    private final SnsClient sns;                 // ⟵ IRSA-credentialed, X-Ray-instrumented (§2.1)
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${app.sns.claims-topic-arn}")
    private String topicArn;

    public void publishClaimSubmitted(ClaimSubmittedEvent event) {

        String payload = writeJson(event);

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(payload)
                .subject("ClaimSubmitted")

                // ⟵ TALKING POINT: THESE ATTRIBUTES ARE WHAT THE FILTER POLICIES MATCH ON.
                //    "If ClaimSubmitted events carry a message attribute claimType, the
                //     auto-claims queue subscribes with {"claimType": ["AUTO"]} and NEVER
                //     SEES health claims at all."
                //    NOTE the DataType strings — "Number" not "Numeric", and a numeric
                //    filter will silently never match a value sent as "String".
                .messageAttributes(Map.of(
                        "claimType", MessageAttributeValue.builder()
                                .dataType("String").stringValue(event.claimType()).build(),
                        "claimedAmount", MessageAttributeValue.builder()
                                .dataType("Number").stringValue(event.claimedAmount().toPlainString()).build(),
                        "region", MessageAttributeValue.builder()
                                .dataType("String").stringValue(event.region()).build(),
                        "eventId", MessageAttributeValue.builder()
                                .dataType("String").stringValue(event.eventId()).build()
                ))
                .build();

        try {
            PublishResponse response = sns.publish(request);
            log.info("Published ClaimSubmitted, snsMessageId={}", response.messageId());
            meterRegistry.counter("claims.events.published",
                                  "type", "ClaimSubmitted",
                                  "result", "success").increment();
            // ⟵ note the LOW-CARDINALITY tags. type and result have a handful of values.
            //    claimId here would create one metric per claim → the "spectacular bill".
        } catch (SnsException e) {
            meterRegistry.counter("claims.events.published",
                                  "type", "ClaimSubmitted", "result", "failure").increment();
            throw new EventPublishException(event.eventId(), e);
        }
    }
}
```

### The event itself — note `eventId`

```java
package com.insurer.claims.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ⟵ TALKING POINT: "every event carries a unique eventId." Generated ONCE at the producer,
 * NOT at the consumer — a consumer-generated ID would be different on every redelivery,
 * which defeats the entire idempotency mechanism in §4.3.
 * Do NOT use the SNS MessageId either: SNS assigns a NEW one on retry.
 */
public record ClaimSubmittedEvent(
        String eventId,
        String claimId,
        String policyNumber,
        String claimType,
        BigDecimal claimedAmount,
        String region,
        String policyHolderEmail,
        Instant occurredAt
) {
    public static ClaimSubmittedEvent from(Claim claim) {
        return new ClaimSubmittedEvent(
                UUID.randomUUID().toString(),   // generated once, at the source
                claim.getClaimId(), claim.getPolicyNumber(), claim.getClaimType(),
                claim.getClaimedAmount(), claim.getRegion(),
                claim.getPolicyHolderEmail(), Instant.now());
    }
}
```

---

## 5.3 Parsing the SNS envelope (when raw delivery is off)

```java
package com.insurer.claims.messaging;

/**
 * ⟵ TALKING POINT: what the envelope actually looks like. Being able to describe this is
 * the difference between "I read about SNS" and "I debugged SNS".
 *
 * {
 *   "Type": "Notification",
 *   "MessageId": "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
 *   "TopicArn": "arn:aws:sns:ap-south-1:123456789012:claims-events",
 *   "Subject": "ClaimSubmitted",
 *   "Message": "{\"eventId\":\"...\",\"claimId\":\"CLM-88231\"}",   ← YOUR payload, as a STRING
 *   "Timestamp": "2024-03-11T09:12:44.111Z",
 *   "SignatureVersion": "1",
 *   "Signature": "EXAMPLEw6JRN...",
 *   "SigningCertURL": "https://sns.ap-south-1.amazonaws.com/SimpleNotificationService-x.pem",
 *   "UnsubscribeURL": "https://sns.ap-south-1.amazonaws.com/?Action=Unsubscribe&...",
 *   "MessageAttributes": { "claimType": { "Type": "String", "Value": "AUTO" } }
 * }
 */
public record SnsEnvelope(
        String Type, String MessageId, String TopicArn, String Subject,
        String Message, Instant Timestamp,
        Map<String, SnsAttribute> MessageAttributes) {

    public record SnsAttribute(String Type, String Value) {}
}

@Component
public class SnsMessageUnwrapper {

    private final ObjectMapper mapper;

    /** Handles BOTH shapes, so the same consumer works whether raw delivery is on or off. */
    public ClaimSubmittedEvent unwrap(String sqsBody) throws JsonProcessingException {
        JsonNode root = mapper.readTree(sqsBody);
        if (root.has("Type") && "Notification".equals(root.get("Type").asText())) {
            return mapper.readValue(root.get("Message").asText(), ClaimSubmittedEvent.class);
        }
        return mapper.readValue(sqsBody, ClaimSubmittedEvent.class);   // raw delivery
    }
}
```

---

## 5.4 Why SNS → SQS and not SNS → your service directly

> This is *the* design question in the explanation. The answer:

```
  SNS ──HTTPS──▶ your service              SNS ──▶ SQS ──poll──▶ your service
  ─────────────────────────────            ────────────────────────────────────
  Consumer down  → SNS retries on a        Consumer down  → messages ACCUMULATE
                   schedule, then GIVES                     safely in the queue
                   UP. Message lost.
  Traffic spike  → 10,000 claims hit a     Traffic spike  → the queue ABSORBS it;
                   service sized for 100                     the consumer drains at
                   req/s. It falls over.                     its own pace (BACK-PRESSURE)
  Slow consumer  → SNS delivery times      Slow consumer  → irrelevant; it polls when ready
                   out, retries pile up
  Retry policy   → SNS's, shared           Retry policy   → PER CONSUMER, with its own DLQ
  Replay a fix   → impossible              Replay a fix   → redrive from the DLQ
```

**One line:** *"The fan-out pattern is SNS for distribution, SQS for durability and back-pressure."*

**SNS vs SQS in one line:** *"SNS is one-to-many push; messages are not stored waiting for a consumer to ask. SQS is one-to-one pull; messages persist until deleted."*

And the concrete payoff from your own narrative: *"Consumers poll their queues independently, so a slow fraud model never blocks the customer's confirmation email."*

> **Also know:** FIFO topics can only deliver to FIFO queues (you cannot mix). And an SNS subscription DLQ catches messages SNS could not *deliver* — which is a different failure from the SQS DLQ, which catches messages a consumer could not *process*. Being able to distinguish those two DLQs is a strong signal.

---

# PART 6 — Amazon CloudWatch

Structure your answer as **three planes: logs, metrics, alarms.**

## 6.1 Fluent Bit DaemonSet

```yaml
# k8s/logging/fluent-bit-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluent-bit-config
  namespace: amazon-cloudwatch
data:
  fluent-bit.conf: |
    [SERVICE]
        Flush                     5
        Log_Level                 info
        Daemon                    off
        Parsers_File              parsers.conf
        HTTP_Server               On
        HTTP_Listen               0.0.0.0
        HTTP_Port                 2020
        storage.path              /var/fluent-bit/state/flb-storage/
        storage.sync              normal
        storage.backlog.mem_limit 5M

    # ⟵ TALKING POINT: "a Fluent Bit DaemonSet runs on EVERY NODE, TAILS CONTAINER STDOUT
    #    FROM THE NODE FILESYSTEM, and ships to CloudWatch Logs."
    #    The app writes to stdout. The container runtime writes that to
    #    /var/log/containers/*.log on the node. Fluent Bit tails those files.
    #    The app knows nothing about CloudWatch — no SDK, no IAM, no coupling.
    [INPUT]
        Name                tail
        Tag                 application.*
        Path                /var/log/containers/*.log
        multiline.parser    docker, cri
        DB                  /var/fluent-bit/state/flb_container.db   # ← offset DB: survives
        Mem_Buf_Limit       50MB                                      #   Fluent Bit restarts
        Skip_Long_Lines     On
        Refresh_Interval    10

    [FILTER]
        Name                kubernetes
        Match               application.*
        Kube_Tag_Prefix     application.var.log.containers.
        Merge_Log           On          # ⟵ parse the JSON log line into real fields
        Merge_Log_Key       log_processed
        Keep_Log            Off
        K8S-Logging.Parser  On
        Labels              On
        Annotations         Off

    [OUTPUT]
        Name                cloudwatch_logs
        Match               application.*
        region              ap-south-1
        # ⟵ "Log group per APPLICATION, log stream per POD"
        log_group_name      /aws/eks/insurance-prod/application
        log_stream_prefix   ${HOST_NAME}-
        log_stream_template $kubernetes['pod_name']
        auto_create_group   true
        # ⟵ TALKING POINT: "log groups DEFAULT TO NEVER EXPIRE, and unbounded log retention
        #    is a real cost line item. We set 30 days for application logs, LONGER FOR AUDIT
        #    LOGS because insurance has regulatory retention requirements."
        log_retention_days  30
        extra_user_agent    container-insights
```

```yaml
# k8s/logging/fluent-bit-daemonset.yaml  (abridged — the important bits)
apiVersion: apps/v1
kind: DaemonSet                      # ⟵ DaemonSet = exactly one pod per node, by definition
metadata: { name: fluent-bit, namespace: amazon-cloudwatch }
spec:
  selector: { matchLabels: { k8s-app: fluent-bit } }
  template:
    spec:
      serviceAccountName: fluent-bit  # ⟵ IRSA role with logs:CreateLogStream/PutLogEvents
      tolerations:
        - operator: Exists            # must run on EVERY node, including tainted ones
      containers:
        - name: fluent-bit
          image: public.ecr.aws/aws-observability/aws-for-fluent-bit:stable
          env:
            - name: HOST_NAME
              valueFrom: { fieldRef: { fieldPath: spec.nodeName } }
          resources:
            limits:   { memory: 200Mi }
            requests: { cpu: 50m, memory: 100Mi }
          volumeMounts:
            - { name: varlog,           mountPath: /var/log,                  readOnly: true }
            - { name: varlibdockercontainers, mountPath: /var/lib/docker/containers, readOnly: true }
            - { name: fluentbitstate,   mountPath: /var/fluent-bit/state }
            - { name: fluent-bit-config, mountPath: /fluent-bit/etc/ }
      volumes:
        - { name: varlog,           hostPath: { path: /var/log } }
        - { name: varlibdockercontainers, hostPath: { path: /var/lib/docker/containers } }
        - { name: fluentbitstate,   hostPath: { path: /var/fluent-bit/state } }
        - { name: fluent-bit-config, configMap: { name: fluent-bit-config } }
```

---

## 6.2 Structured JSON logging + the correlation ID

> *"CRITICALLY: your app logs STRUCTURED JSON, NOT PLAIN TEXT, with a CORRELATION ID in Logback's MDC. That's what makes CloudWatch Logs Insights useful, because you can QUERY FIELDS rather than regex over strings."*

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>

  <springProperty scope="context" name="appName" source="spring.application.name"/>
  <springProperty scope="context" name="env"     source="ENVIRONMENT" defaultValue="dev"/>

  <!-- Local dev: human-readable -->
  <springProfile name="local">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <encoder>
        <pattern>%d{HH:mm:ss} %-5level [%X{claimId}] %logger{36} - %msg%n</pattern>
      </encoder>
    </appender>
  </springProfile>

  <!-- Everything else: ONE JSON OBJECT PER LINE, straight to stdout -->
  <springProfile name="!local">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
          <timestamp><fieldName>timestamp</fieldName><timeZone>UTC</timeZone></timestamp>
          <logLevel><fieldName>level</fieldName></logLevel>
          <loggerName><fieldName>logger</fieldName><shortenedLoggerNameLength>36</shortenedLoggerNameLength></loggerName>
          <threadName><fieldName>thread</fieldName></threadName>
          <message><fieldName>message</fieldName></message>
          <stackTrace><fieldName>stackTrace</fieldName></stackTrace>

          <!-- ⟵ THIS IS THE LINE THAT MATTERS. Everything in the MDC becomes a TOP-LEVEL
                  JSON FIELD, which means Logs Insights can filter on `claimId` and
                  `correlationId` directly instead of regexing the message string. -->
          <mdc/>

          <pattern>
            <pattern>
              {
                "service": "${appName}",
                "environment": "${env}",
                "podName": "${HOSTNAME}"
              }
            </pattern>
          </pattern>
        </providers>
      </encoder>
    </appender>
    <root level="INFO"><appender-ref ref="JSON"/></root>
  </springProfile>

</configuration>
```

### Populating the MDC — a servlet filter

```java
package com.insurer.claims.observability;

import com.amazonaws.xray.AWSXRay;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)   // run BEFORE the X-Ray filter so the correlation ID is present for every log line
public class CorrelationIdFilter implements Filter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) req;

        // Accept an inbound ID (the portal generates one) or mint one at the edge
        String correlationId = Optional.ofNullable(http.getHeader(HEADER))
                                       .filter(s -> !s.isBlank())
                                       .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put("correlationId", correlationId);
        MDC.put("path", http.getRequestURI());
        MDC.put("method", http.getMethod());

        // ⟵ Tie logs to traces: putting the X-Ray trace ID in the MDC means you can jump
        //    from a CloudWatch log line straight to the trace, and back.
        AWSXRay.getCurrentSegmentOptional()
               .ifPresent(seg -> MDC.put("traceId", seg.getTraceId().toString()));

        try {
            ((HttpServletResponse) res).setHeader(HEADER, correlationId);
            chain.doFilter(req, res);
        } finally {
            MDC.clear();   // ⟵ MANDATORY. MDC is a ThreadLocal and Tomcat threads are POOLED.
        }                  //   Skip this and request N+1 logs request N's claimId. Silent,
    }                      //   confusing, and it will absolutely be asked about.
}
```

> **Follow-up they love:** *"What happens to the MDC on an `@Async` thread or inside a `CompletableFuture`?"* **It is lost** — `MDC` is a plain `ThreadLocal`, not `InheritableThreadLocal` by default in the async case. Fix with a `TaskDecorator`:

```java
@Bean
public TaskDecorator mdcTaskDecorator() {
    return runnable -> {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) MDC.setContextMap(contextMap);
            try { runnable.run(); } finally { MDC.clear(); }
        };
    };
}
```

### What a log line actually looks like

```json
{"timestamp":"2024-03-11T09:12:44.117Z","level":"INFO","logger":"c.i.c.api.ClaimController",
 "thread":"http-nio-8080-exec-7","message":"Claim accepted","service":"claims-service",
 "environment":"prod","podName":"claims-service-7d9f-x2k4c",
 "correlationId":"9f2c...","claimId":"CLM-88231","traceId":"1-65eea1cc-1a2b3c4d5e6f",
 "durationMs":142,"errorCode":null}
```

---

## 6.3 CloudWatch Logs Insights queries

The one to be able to write on a whiteboard:

```sql
fields @timestamp, claimId, durationMs, @message
| filter service = "claims-service" and level = "ERROR"
| stats count() by errorCode
| sort by count desc
| limit 20
```

Others worth having:

```sql
-- p50/p90/p99 latency per endpoint, straight out of logs
fields @timestamp, path, durationMs
| filter service = "claims-service" and ispresent(durationMs)
| stats count() as requests,
        pct(durationMs, 50) as p50,
        pct(durationMs, 90) as p90,
        pct(durationMs, 99) as p99
    by path
| sort p99 desc
```

```sql
-- ⟵ the payoff of the correlation ID: ONE claim's complete journey across ALL services
fields @timestamp, service, level, message
| filter claimId = "CLM-88231"
| sort @timestamp asc
```

```sql
-- error rate per minute — the shape you'd graph next to an alarm
fields @timestamp
| filter service = "claims-service"
| stats sum(level = "ERROR") * 100.0 / count() as errorRatePct by bin(1m)
```

```sql
-- find the noisiest log source before you get the bill
fields @logStream
| stats sum(strlen(@message)) as bytes by @logStream
| sort bytes desc | limit 20
```

---

## 6.4 Metrics — infrastructure and application

```java
package com.insurer.claims.observability;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

@Component
public class ClaimMetrics {

    private final Counter submitted;
    private final Counter rejected;
    private final Timer   endToEnd;
    private final DistributionSummary amounts;

    public ClaimMetrics(MeterRegistry registry) {

        // ⟵ LOW cardinality tags only. channel ∈ {web, mobile, agent}, type ∈ {AUTO,...}
        this.submitted = Counter.builder("claims.submitted")
                .description("Claims accepted for processing")
                .tag("channel", "unset")
                .register(registry);

        this.endToEnd = Timer.builder("claims.submission.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.amounts = DistributionSummary.builder("claims.amount")
                .baseUnit("INR")
                .register(registry);

        // ⟵ TALKING POINT (COST GOTCHA): "custom metrics are billed per UNIQUE COMBINATION
        //    of metric name and dimension values. Putting a HIGH-CARDINALITY dimension like
        //    claimId on a metric creates A SEPARATE METRIC PER CLAIM and produces a
        //    SPECTACULAR BILL." At $0.30/metric/month and 40,000 claims/day that is
        //    ~$360,000/month. This is the single most expensive mistake in the whole stack.
        //
        //    NEVER DO THIS:
        //    Counter.builder("claims.submitted").tag("claimId", claimId).register(registry);
        //
        //    claimId belongs in: LOGS (§6.2) and X-RAY ANNOTATIONS (§7.4).
    }

    public void recordSubmission(String channel, String claimType,
                                 java.math.BigDecimal amount, long millis) {
        Counter.builder("claims.submitted")
               .tags("channel", channel, "type", claimType)   // bounded sets — safe
               .register(Metrics.globalRegistry).increment();
        endToEnd.record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
        amounts.record(amount.doubleValue());
    }
}
```

### Embedded Metric Format — the cheap alternative

```java
package com.insurer.claims.observability;

/**
 * ⟵ TALKING POINT: "EMF is the CHEAPER ALTERNATIVE — you emit a specially structured JSON
 * LOG LINE and CloudWatch EXTRACTS METRICS FROM IT, so you pay LOG INGESTION instead of
 * PutMetricData calls." Also: no API call in the request path, and you can attach
 * high-cardinality fields as PROPERTIES (searchable in Logs Insights) alongside the
 * low-cardinality DIMENSIONS that actually become metrics. Best of both worlds.
 */
@Component
public class EmfMetricEmitter {

    private static final Logger metricsLog = LoggerFactory.getLogger("EMF");
    private final ObjectMapper mapper = new ObjectMapper();

    public void emitClaimProcessed(String claimType, String channel,
                                   String claimId, long durationMs) throws Exception {
        Map<String, Object> emf = Map.of(
            "_aws", Map.of(
                "Timestamp", System.currentTimeMillis(),
                "CloudWatchMetrics", List.of(Map.of(
                    "Namespace", "ClaimsPlatform",
                    // DIMENSIONS become the metric's identity — keep these LOW cardinality
                    "Dimensions", List.of(List.of("ClaimType", "Channel")),
                    "Metrics", List.of(
                        Map.of("Name", "ProcessingDuration", "Unit", "Milliseconds"),
                        Map.of("Name", "ClaimsProcessed",    "Unit", "Count"))
                ))
            ),
            "ClaimType", claimType,          // dimension value
            "Channel",   channel,            // dimension value
            "ProcessingDuration", durationMs,
            "ClaimsProcessed", 1,
            // ⟵ a PROPERTY, not a dimension: searchable in Logs Insights, does NOT create
            //    a metric, does NOT cost you anything per unique value.
            "claimId", claimId
        );
        metricsLog.info(mapper.writeValueAsString(emf));
    }
}
```

### The metric names to know by heart

| Source | Metrics that matter |
|---|---|
| **EKS Container Insights** | `pod_cpu_utilization`, `pod_memory_utilization`, `node_filesystem_utilization`, `pod_number_of_container_restarts` |
| **RDS** | `CPUUtilization`, `DatabaseConnections`, `ReadLatency`, `WriteLatency`, `FreeableMemory`, `FreeStorageSpace`, `ReplicaLag`, `BurstBalance` |
| **SQS** | `ApproximateNumberOfMessagesVisible`, **`ApproximateAgeOfOldestMessage`**, `NumberOfMessagesSent`, `NumberOfMessagesDeleted`, `ApproximateNumberOfMessagesNotVisible` |
| **ALB** | `TargetResponseTime`, `HTTPCode_Target_5XX_Count`, `HTTPCode_ELB_5XX_Count`, `UnHealthyHostCount`, `RejectedConnectionCount` |
| **Micrometer / Actuator** | `jvm.memory.used`, `jvm.gc.pause`, `http.server.requests`, `hikaricp.connections.active`, `hikaricp.connections.pending` |

---

## 6.5 Alarms — alarm on symptoms, not causes

```hcl
# terraform/alarms.tf

resource "aws_sns_topic" "alerts" { name = "claims-platform-alerts" }

resource "aws_sns_topic_subscription" "pagerduty" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "https"
  endpoint  = var.pagerduty_integration_url
}

# ── SYMPTOM 1: customers are seeing errors ──────────────────────────────────────
# ⟵ TALKING POINT: "alarm on SYMPTOMS CUSTOMERS FEEL, not on causes. HIGH CPU IS NOT AN
#    INCIDENT; elevated 5xx rate and p99 latency are." High CPU during a healthy traffic
#    peak is a machine doing its job. Paging on it trains people to ignore pages.
resource "aws_cloudwatch_metric_alarm" "elevated_5xx" {
  alarm_name          = "claims-elevated-5xx-rate"
  comparison_operator = "GreaterThanThreshold"
  threshold           = 1                    # percent
  evaluation_periods  = 2                    # ⟵ "N datapoints in M periods"
  datapoints_to_alarm = 2
  treat_missing_data  = "notBreaching"       # no traffic ≠ broken

  metric_query {
    id          = "error_rate"
    expression  = "(errors / requests) * 100"
    label       = "5xx rate %"
    return_data = true
  }
  metric_query {
    id = "errors"
    metric {
      metric_name = "HTTPCode_Target_5XX_Count"
      namespace   = "AWS/ApplicationELB"
      period      = 60
      stat        = "Sum"
      dimensions  = { LoadBalancer = var.alb_suffix }
    }
  }
  metric_query {
    id = "requests"
    metric {
      metric_name = "RequestCount"
      namespace   = "AWS/ApplicationELB"
      period      = 60
      stat        = "Sum"
      dimensions  = { LoadBalancer = var.alb_suffix }
    }
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]
}

# ── SYMPTOM 2: it's slow ────────────────────────────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "p99_latency" {
  alarm_name          = "claims-p99-latency-high"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "TargetResponseTime"
  extended_statistic  = "p99"                # ⟵ p99, NOT Average. Averages hide the tail.
  period              = 60
  evaluation_periods  = 3
  datapoints_to_alarm = 2                    # 2 of 3 → tolerate a single noisy minute
  comparison_operator = "GreaterThanThreshold"
  threshold           = 1.5                  # seconds
  dimensions          = { LoadBalancer = var.alb_suffix }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

# ── SYMPTOM 3: THE QUEUE ALARM THAT ACTUALLY MATTERS ────────────────────────────
# ⟵ TALKING POINT: "for the queues, the alarm that actually matters is
#    ApproximateAgeOfOldestMessage, NOT QUEUE DEPTH — a DEEP QUEUE THAT'S DRAINING FAST IS
#    FINE, but an OLD MESSAGE MEANS CONSUMERS ARE STUCK."
#    Depth alarms fire on every marketing campaign. Age alarms fire when something is wrong.
resource "aws_cloudwatch_metric_alarm" "queue_age" {
  for_each            = toset(local.consumers)
  alarm_name          = "claims-${each.key}-oldest-message-age"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateAgeOfOldestMessage"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  comparison_operator = "GreaterThanThreshold"
  threshold           = 300                  # 5 minutes
  dimensions          = { QueueName = aws_sqs_queue.consumer[each.key].name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

# ── DLQ: any message here is a human problem ────────────────────────────────────
# ⟵ TALKING POINT: "set a CloudWatch alarm on ApproximateNumberOfMessagesVisible on the DLQ
#    SO SOMEONE GETS PAGED."
resource "aws_cloudwatch_metric_alarm" "dlq_not_empty" {
  for_each            = toset(local.consumers)
  alarm_name          = "claims-${each.key}-dlq-not-empty"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  comparison_operator = "GreaterThanThreshold"
  threshold           = 0                    # ANY message in a DLQ deserves attention
  treat_missing_data  = "notBreaching"
  dimensions          = { QueueName = aws_sqs_queue.dlq[each.key].name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

# ── Connection-pool exhaustion (the §3.3 arithmetic, alarmed) ───────────────────
resource "aws_cloudwatch_metric_alarm" "db_connections" {
  alarm_name          = "claims-db-connections-high"
  namespace           = "AWS/RDS"
  metric_name         = "DatabaseConnections"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  comparison_operator = "GreaterThanThreshold"
  threshold           = 350                  # 70% of the practical ceiling
  dimensions          = { DBInstanceIdentifier = aws_db_instance.claims_primary.id }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

# ── COMPOSITE ALARM: suppress the noise ─────────────────────────────────────────
# ⟵ TALKING POINT: "Use COMPOSITE ALARMS to suppress noise, so a DOWNSTREAM alarm doesn't
#    page separately when the UPSTREAM one already fired."
#    Concretely: RDS falls over → DB connections alarm AND 5xx alarm AND p99 alarm AND
#    queue-age alarm all fire = 4 pages for 1 incident. The composite alarm is the ONLY
#    thing wired to PagerDuty; the individual alarms just feed it and populate dashboards.
resource "aws_cloudwatch_composite_alarm" "claims_service_degraded" {
  alarm_name = "claims-service-DEGRADED"
  alarm_rule = join(" OR ", [
    "ALARM(${aws_cloudwatch_metric_alarm.elevated_5xx.alarm_name})",
    "ALARM(${aws_cloudwatch_metric_alarm.p99_latency.alarm_name})"
  ])
  # only page if the DB is NOT already known-bad — that has its own runbook & its own page
  actions_suppressor                          = aws_cloudwatch_metric_alarm.db_connections.alarm_name
  actions_suppressor_wait_period              = 60
  actions_suppressor_extension_period         = 120

  alarm_actions = [aws_sns_topic.alerts.arn]
}
```

**Alarm anatomy, stated the way the explanation phrases it:** *"Alarms evaluate a metric against a threshold over **N datapoints in M periods** and publish state changes to an SNS topic, which routes to email or PagerDuty."* `treat_missing_data` is the fourth dimension people forget — `missing`, `notBreaching`, `breaching`, `ignore` — and getting it wrong is why alarms fire at 3am when a low-traffic endpoint simply had no requests.

---

# PART 7 — AWS X-Ray

## 7.1 The daemon as a DaemonSet

```yaml
# k8s/observability/xray-daemon.yaml
apiVersion: v1
kind: ConfigMap
metadata: { name: xray-config, namespace: amazon-cloudwatch }
data:
  xray-daemon-config.yaml: |
    TotalBufferSizeMB: 24
    Socket:
      UDPAddress: "0.0.0.0:2000"     # ⟵ segments in, over UDP
      TCPAddress: "0.0.0.0:2000"     # ⟵ sampling rule polling, over TCP
    Region: "ap-south-1"
    LocalMode: false
---
apiVersion: apps/v1
kind: DaemonSet
metadata: { name: xray-daemon, namespace: amazon-cloudwatch }
spec:
  selector: { matchLabels: { app: xray-daemon } }
  template:
    metadata: { labels: { app: xray-daemon } }
    spec:
      hostNetwork: true                # ⟵ so pods can reach it at status.hostIP:2000
      dnsPolicy: ClusterFirstWithHostNet
      serviceAccountName: xray-daemon  # IRSA: xray:PutTraceSegments, PutTelemetryRecords
      containers:
        - name: xray-daemon
          image: public.ecr.aws/xray/aws-xray-daemon:latest
          command: ["/usr/bin/xray", "-c", "/aws/xray/config.yaml"]
          ports:
            - { name: xray-ingest, containerPort: 2000, hostPort: 2000, protocol: UDP }
            - { name: xray-tcp,    containerPort: 2000, hostPort: 2000, protocol: TCP }
          resources:
            limits:   { memory: 64Mi }
            requests: { cpu: 32m, memory: 32Mi }
          volumeMounts:
            - { name: config, mountPath: /aws/xray }
      volumes:
        - name: config
          configMap:
            name: xray-config
            items: [{ key: xray-daemon-config.yaml, path: config.yaml }]
```

> ⟵ **TALKING POINT:** *"Your app doesn't call the X-Ray API directly — the SDK sends segment documents to the LOCAL DAEMON over **UDP port 2000**, which BATCHES and UPLOADS them."*
> Why this design matters: UDP is fire-and-forget, so instrumentation adds **microseconds** and can never block or fail a request. If the daemon is down you lose traces; you do not lose requests. That trade is the entire reason for the daemon's existence.

---

## 7.2 Spring Boot wiring — the servlet filter

```java
package com.insurer.claims.observability;

import com.amazonaws.xray.*;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import jakarta.servlet.Filter;
import org.springframework.context.annotation.*;

@Configuration
public class XRayConfig {

    static {
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
                .withPlugin(new EKSPlugin())     // ⟵ tags segments with cluster/pod/container
                                                 //   so the service map knows it's EKS
                .withSamplingStrategy(new LocalizedSamplingStrategy(
                        XRayConfig.class.getResource("/xray-sampling-rules.json")));
        AWSXRay.setGlobalRecorder(builder.build());
    }

    /**
     * ⟵ TALKING POINT: "In Spring Boot you REGISTER THE AWSXRayServletFilter for INBOUND
     * REQUESTS." This filter reads the X-Amzn-Trace-Id header, opens a SEGMENT named after
     * the argument below, and closes it when the response completes. The name is what shows
     * up as a NODE ON THE SERVICE MAP — so it must be the service name, not the endpoint.
     */
    @Bean
    public Filter tracingFilter() {
        return new AWSXRayServletFilter("claims-service");
    }
}
```

### Subsegments for your own code

```java
package com.insurer.claims.observability;

import com.amazonaws.xray.spring.aop.BaseAbstractXRayInterceptor;
import org.aspectj.lang.annotation.*;

/**
 * ⟵ Anything annotated @XRayEnabled gets a subsegment automatically. Cheaper than
 * hand-rolling beginSubsegment/endSubsegment everywhere, and impossible to leak.
 */
@Aspect
@Component
public class XRayInspector extends BaseAbstractXRayInterceptor {

    @Override
    @Pointcut("@within(com.amazonaws.xray.spring.aop.XRayEnabled) "
            + "|| @annotation(com.amazonaws.xray.spring.aop.XRayEnabled)")
    protected void xrayEnabledClasses() {}
}
```

```java
@Service
@XRayEnabled                      // every public method → its own subsegment
public class ClaimSubmissionService { /* ... */ }
```

Manual, when you need control:

```java
Subsegment sub = AWSXRay.beginSubsegment("oracle-policy-validation");
try {
    sub.putAnnotation("policyNumber", policyNumber);   // indexed → searchable
    return policyGateway.validatePolicy(policyNumber, incidentDate);
} catch (Exception e) {
    sub.addException(e);          // ⟵ marks the subsegment as errored/faulted on the map
    throw e;
} finally {
    AWSXRay.endSubsegment();      // ⟵ ALWAYS in finally, or you leak the segment context
}
```

---

## 7.3 Instrumenting JDBC and the AWS SDK

```java
package com.insurer.claims.config;

import com.amazonaws.xray.sql.TracingDataSource;
import org.springframework.context.annotation.*;
import javax.sql.DataSource;

@Configuration
public class TracedDataSourceConfig {

    /**
     * ⟵ TALKING POINT: "use the X-Ray-instrumented JDBC INTERCEPTOR to CAPTURE SQL AS
     * SUBSEGMENTS." Every statement becomes a subsegment on the trace with the sanitised
     * SQL, the database type, and the duration. THIS is how you find the 4-second sequential
     * scan from §3.5 without reading a single log file.
     */
    @Bean
    @Primary
    public DataSource tracedDataSource(@Qualifier("primaryDs") DataSource delegate) {
        return TracingDataSource.decorate(delegate);
    }
}
```

AWS SDK v2 clients are wrapped with the `TracingInterceptor` — already shown in §2.1:

```java
SnsClient.builder()
    .overrideConfiguration(ClientOverrideConfiguration.builder()
        .addExecutionInterceptor(new TracingInterceptor())   // ⟵ "wrap AWS SDK clients with
        .build())                                            //    the tracing handler so SQS
    .build();                                                //    and S3 calls appear
                                                             //    AUTOMATICALLY"
```

Outbound HTTP:

```java
@Bean
public RestTemplate tracedRestTemplate(RestTemplateBuilder builder) {
    return builder
            // propagates X-Amzn-Trace-Id downstream AND records a subsegment
            .requestFactory(() -> new TracedHttpComponentsClientHttpRequestFactory())
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
}
```

---

## 7.4 Annotations vs metadata — the favourite trick question

```java
Segment segment = AWSXRay.getCurrentSegment();

// ── ANNOTATIONS: INDEXED. SEARCHABLE. Max 50 per segment. ────────────────────────
segment.putAnnotation("claimId",   "CLM-88231");    // ← identifiers you'll search by
segment.putAnnotation("claimType", "AUTO");         // ← dimensions you'll group by
segment.putAnnotation("channel",   "mobile");
segment.putAnnotation("highValue", true);
// Only String, Number, Boolean are allowed. No objects, no collections.

// ── METADATA: NOT indexed. NOT searchable. Arbitrary JSON. Unlimited-ish. ────────
segment.putMetadata("request", "payload", submitClaimRequest);   // full object, for context
segment.putMetadata("policy",  "coverages", coverageList);
segment.putMetadata("debug",   "hikariActive", hikariPool.getActiveConnections());
```

**The filter expression that makes annotations worth it:**

```
annotation.claimId = "CLM-88231"
```

```
# other expressions worth having
service("claims-service") AND responsetime > 3
service("claims-service") { fault = true }
annotation.claimType = "AUTO" AND annotation.highValue = true AND responsetime > 2
http.url CONTAINS "/claims/submit" AND http.status = 500
```

Console / CLI:

```bash
aws xray get-trace-summaries \
  --start-time 2024-03-11T09:00:00 --end-time 2024-03-11T10:00:00 \
  --filter-expression 'annotation.claimId = "CLM-88231"'
```

> **Say it this way:** *"Annotations are indexed key-value pairs you can filter traces on. Metadata is arbitrary JSON attached to the segment for context, but not indexed and not searchable. You get a limited number of annotations per segment, so use them for identifiers and dimensions you'll actually search by."*
> **And the cost angle:** indexing isn't free, and 50 is a hard cap — a `putAnnotation` in a loop silently drops everything past 50.

---

## 7.5 Sampling — say this if asked about cost

```json
// src/main/resources/xray-sampling-rules.json
{
  "version": 2,
  "rules": [
    {
      "description": "Business-critical: trace EVERY claim submission",
      "service_name": "claims-service",
      "http_method":  "POST",
      "url_path":     "/claims/submit",
      "fixed_target": 1,
      "rate": 1.0,                 // ⟵ 100%. "sample ALL requests to /claims/submit
      "priority": 100              //    BECAUSE IT'S BUSINESS-CRITICAL"
    },
    {
      "description": "Health checks and metrics: never trace",
      "service_name": "*",
      "http_method":  "GET",
      "url_path":     "/actuator/*",
      "fixed_target": 0,
      "rate": 0.0,                 // ⟵ "sample health checks at 0%". The ALB probes every
      "priority": 200              //    10s per target — that is pure noise and pure cost.
    },
    {
      "description": "Search endpoints: light sampling",
      "service_name": "claims-service",
      "url_path":     "/claims/search*",
      "fixed_target": 1,
      "rate": 0.10,
      "priority": 300
    }
  ],
  // ⟵ TALKING POINT: THE DEFAULT RULE. "a RESERVOIR OF 1 REQUEST PER SECOND plus
  //    5% of EVERYTHING ABOVE THAT."
  //    The reservoir guarantees you always have SOME traces even on a quiet service;
  //    the percentage stops a traffic spike from becoming a bill spike.
  "default": { "fixed_target": 1, "rate": 0.05 }
}
```

Centrally-managed rules (preferred in prod — change sampling without a redeploy):

```hcl
resource "aws_xray_sampling_rule" "claims_submit" {
  rule_name      = "claims-submit-full"
  priority       = 100
  version        = 1
  reservoir_size = 1
  fixed_rate     = 1.0
  service_name   = "claims-service"
  service_type   = "*"
  host           = "*"
  http_method    = "POST"
  url_path       = "/claims/submit"
  resource_arn   = "*"
}
```

**The header format, so you can draw it:**

```
X-Amzn-Trace-Id: Root=1-65eea1cc-1a2b3c4d5e6f7a8b9c0d1e2f;Parent=53995c3f42cd8ad8;Sampled=1
                      ↑        ↑                            ↑                    ↑
                      version  8 hex = epoch seconds         parent SEGMENT id    sampling decision
                               24 hex = random               (16 hex)             made ONCE at the
                                                                                  edge and honoured
                                                                                  by every hop
```

> *"The ALB GENERATES IT IF IT'S ABSENT; each service reads it, adds its own segment with the incoming segment as parent, and forwards it on outbound calls."* The `Sampled=1` flag propagating is what makes a trace **complete** — if each service decided independently you'd get traces with holes in them.

---

## 7.6 Trace context across SQS — the subtle bit

```java
package com.insurer.claims.messaging;

/**
 * PRODUCER side. The AWS SDK's TracingInterceptor automatically adds the trace header to
 * the SQS message's SYSTEM ATTRIBUTES (AWSTraceHeader) — you do not have to do it manually
 * for SQS. For SNS→SQS you may need to carry it yourself as a message attribute, because
 * the header does not always survive the SNS hop.
 */
public void publishWithTraceContext(ClaimSubmittedEvent event) {
    String traceHeader = AWSXRay.getCurrentSegmentOptional()
            .map(s -> "Root=" + s.getTraceId() + ";Parent=" + s.getId()
                    + ";Sampled=" + (s.isSampled() ? "1" : "0"))
            .orElse(null);

    PublishRequest.Builder req = PublishRequest.builder()
            .topicArn(topicArn)
            .message(writeJson(event));

    if (traceHeader != null) {
        req.messageAttributes(Map.of("AWSTraceHeader",
                MessageAttributeValue.builder().dataType("String").stringValue(traceHeader).build()));
    }
    sns.publish(req.build());
}
```

```java
/**
 * CONSUMER side: continue the SAME trace.
 */
private void handleWithTrace(Message message) {
    String traceHeader = message.attributes()
            .get(MessageSystemAttributeName.AWS_TRACE_HEADER);

    TraceHeader header = traceHeader != null
            ? TraceHeader.fromString(traceHeader)
            : new TraceHeader();

    Segment segment = AWSXRay.beginSegment("notification-worker",
                                           header.getRootTraceId(),
                                           header.getParentId());
    segment.setSampled(header.getSampled() == TraceHeader.SampleDecision.SAMPLED);
    try {
        segment.putAnnotation("claimId", event.claimId());   // same claimId, both segments
        processor.process(message.body());
    } finally {
        AWSXRay.endSegment();
    }
}
```

> ⟵ **TALKING POINT — be honest about the gap:** *"BUT the producer's segment CLOSES WHEN IT PUBLISHES — the consumer's work appears as a SEPARATE, LINKED segment, NOT NESTED INSIDE. If they push on this, ACKNOWLEDGING THE ASYNC GAP IS A BETTER ANSWER THAN PRETENDING IT'S SEAMLESS."*
>
> Concretely: a synchronous HTTP trace shows a parent span *containing* the child, so you see total elapsed time. Across a queue you get two segments sharing a Root trace ID. The "time spent waiting in the queue" is the gap between them, which X-Ray does not draw for you. That's exactly why the `ApproximateAgeOfOldestMessage` alarm (§6.5) exists — it measures the thing the trace can't.

---

## 7.7 The story that ties it together

> *"We had intermittent 8-second p99 spikes on claim submission. CloudWatch showed elevated latency but not WHERE. The X-Ray SERVICE MAP showed the claims-service segment was mostly ONE SUBSEGMENT — a synchronous call to the document service that was itself waiting on an S3 upload. We moved the document upload out of the request path entirely and onto the SQS queue. p99 came down to under 400 milliseconds."*

**Before (synchronous):**

```java
@PostMapping("/submit")
public ResponseEntity<SubmitClaimResponse> submit(@RequestBody SubmitClaimRequest req) {
    Claim claim = claimService.save(req);
    documentClient.uploadAndIndex(claim.getClaimId(), req.attachments());  // ← 6–7 SECONDS
    notificationClient.sendConfirmation(claim);                            // ← blocking too
    return ResponseEntity.ok(...);
}
```

**After (the upload moved off the request path):**

```java
@PostMapping("/submit")
public ResponseEntity<SubmitClaimResponse> submit(@RequestBody SubmitClaimRequest req) {
    Claim claim = claimService.save(req);           // ~40ms
    // publish → SNS → SQS document queue. The worker does the S3 work off the hot path.
    // Response goes back in ~180ms; the customer sees "received", not a spinner.
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(...);
}
```

### The modern-context sentence that earns credit

> *"X-Ray is AWS-PROPRIETARY, and the industry has moved toward OPENTELEMETRY. ADOT is AWS's OTel distribution, which lets you INSTRUMENT ONCE AND EXPORT to X-Ray, Prometheus, or a vendor."*

```yaml
# k8s/observability/adot-collector.yaml — the direction of travel
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata: { name: adot, namespace: amazon-cloudwatch }
spec:
  mode: daemonset
  serviceAccount: adot-collector
  config: |
    receivers:
      otlp:
        protocols: { grpc: { endpoint: 0.0.0.0:4317 }, http: { endpoint: 0.0.0.0:4318 } }
      awsxray:                      # accepts legacy X-Ray SDK UDP traffic during migration
        endpoint: 0.0.0.0:2000
        transport: udp
    processors:
      batch: { timeout: 5s, send_batch_size: 512 }
      resourcedetection: { detectors: [env, eks, ec2] }
    exporters:
      awsxray:            { region: ap-south-1 }
      awsemf:             { region: ap-south-1, namespace: ClaimsPlatform }
      prometheusremotewrite: { endpoint: "https://aps-workspaces.../api/v1/remote_write" }
    service:
      pipelines:
        traces:  { receivers: [otlp, awsxray], processors: [resourcedetection, batch],
                   exporters: [awsxray] }
        metrics: { receivers: [otlp], processors: [resourcedetection, batch],
                   exporters: [awsemf, prometheusremotewrite] }
```

Java side with OTel (zero code changes — a javaagent):

```dockerfile
ADD https://github.com/aws-observability/aws-otel-java-instrumentation/releases/latest/download/aws-opentelemetry-agent.jar /opt/aws-opentelemetry-agent.jar
ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/aws-opentelemetry-agent.jar $JAVA_TOOL_OPTIONS"
ENV OTEL_SERVICE_NAME=claims-service
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://$(NODE_IP):4317
ENV OTEL_PROPAGATORS=xray,tracecontext,baggage
```

---

# PART 8 — The Java 11 → 17 upgrade

## 8.1 Step 1: find the internal-API usage BEFORE you change anything

```bash
# ⟵ TALKING POINT: "ran jdeps --jdk-internals across the codebase to find internal API usage"
./gradlew clean build -x test

jdeps --jdk-internals --multi-release 17 \
      --class-path "$(find ~/.gradle/caches -name '*.jar' | tr '\n' ':')" \
      build/libs/claims-service.jar > jdeps-report.txt

# what a hit looks like:
#   claims-service.jar -> JDK removed internal API
#      com.thirdparty.serializer.UnsafeAccess -> sun.misc.Unsafe   JDK internal API (JDK removed internal API)
#   Warning: <suggestion> Use java.lang.invoke.VarHandle @since 9
```

```bash
# Same thing, but strict-fail in CI so a new dependency can't reintroduce the problem
jdeps --jdk-internals --multi-release 17 build/libs/claims-service.jar \
  | tee jdeps.txt
! grep -q "JDK internal API" jdeps.txt || { echo "Internal API usage found"; exit 1; }
```

## 8.2 What actually breaks — JEP 403

```java
/**
 * ⟵ TALKING POINT — "THE NUMBER ONE SOURCE OF UPGRADE PAIN."
 *
 * Java 9  → JEP 261: modules, but --illegal-access=permit was the DEFAULT.
 *            Reflecting into JDK internals printed a WARNING and worked.
 * Java 16 → JEP 396: the default flipped to --illegal-access=deny. Still overridable.
 * Java 17 → JEP 403: --illegal-access WAS REMOVED ENTIRELY. There is no escape hatch flag.
 *            Reflecting into a non-exported package now throws InaccessibleObjectException.
 *
 * "This USUALLY SURFACES IN SERIALIZATION OR BYTECODE-MANIPULATION LIBRARIES" — because
 * those are exactly the libraries that need setAccessible(true) on private JDK fields.
 */
public class WhatBreaks {

    // Worked on Java 11 (with a warning). Throws on Java 17.
    void reflectIntoJdkInternals() throws Exception {
        Field f = java.time.LocalDate.class.getDeclaredField("year");
        f.setAccessible(true);
        // java.lang.reflect.InaccessibleObjectException:
        //   Unable to make field private final int java.time.LocalDate.year accessible:
        //   module java.base does not "opens java.time" to unnamed module @0x1b6d3586
    }
}
```

**The two fixes, in order of preference:**

```bash
# PREFERRED: upgrade the library. Someone has already fixed this upstream.
#   Jackson  2.10+   handles it
#   Mockito  4.x + Byte Buddy 1.11+
#   Lombok   1.18.22+  (below this it literally cannot compile on 17)
#   Hibernate 5.6+
#   Spring Boot 2.5+ (we targeted 2.7)
```

```bash
# ESCAPE HATCH, only if you're blocked on an unmaintained dependency.
# ⟵ "with UPGRADING STRONGLY PREFERRED" — --add-opens is a TIME BOMB: it must be repeated
#    on every JVM invocation (app, tests, CLI tools) and it hides a problem that will come
#    back on the next LTS.
java --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.util=ALL-UNNAMED \
     --add-opens java.base/java.time=ALL-UNNAMED \
     -jar app.jar
```

```groovy
// build.gradle — the test JVM needs it too, and this is where people get stuck
tasks.withType(Test).configureEach {
    jvmArgs += [
        '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
        '--add-opens', 'java.base/java.util=ALL-UNNAMED'
    ]
}
```

```
# Or bake it into the jar so you don't have to remember (MANIFEST.MF)
Add-Opens: java.base/java.lang java.base/java.util
```

## 8.3 Removals to name if asked

```java
// ⟵ "Removals: Nashorn JavaScript engine, RMI activation, the applet API.
//     Security Manager DEPRECATED FOR REMOVAL."

// JDK 11 — worked. JDK 15+ — Nashorn removed (JEP 372); this returns NULL, then NPEs.
ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
// Fix: org.openjdk.nashorn:nashorn-core as an external dependency, or GraalVM JS.
// Our hit: a rules engine evaluating claim-eligibility expressions in JS.

// Security Manager: @Deprecated(forRemoval = true) in 17 (JEP 411). Any System.setSecurityManager
// call now prints a terminal warning. Removed in JDK 24.
```

## 8.4 Dependency floors — the compatibility matrix

```groovy
// The bumps that were NOT optional, and WHY:
//   Lombok       1.18.16 → 1.18.30   reflects into com.sun.tools.javac internals; JEP 403 blocks it
//   Byte Buddy   1.10.x  → 1.12.x    ⟵ "any BYTECODE-TOUCHING TOOLING needed bumps because
//   Mockito      3.6     → 4.11        THEY PARSE CLASS FILE VERSIONS" — a 17 class file is
//   ASM          7.x     → 9.x         major version 61; older ASM throws
//                                      IllegalArgumentException: Unsupported class file major version 61
//   Gradle       6.5     → 7.6        ⟵ "Gradle and the compiler plugin needed updating";
//                                      Gradle < 7.3 cannot even RUN on JDK 17
//   Spring Boot  2.3     → 2.7.18     ⟵ "needed to be AT LEAST 2.5, and we targeted 2.7"
//   Hibernate    5.4     → 5.6.15
//   ojdbc8               → ojdbc11
```

```bash
# Prove the matrix rather than guessing at it
./gradlew dependencyUpdates          # com.github.ben-manes.versions plugin
./gradlew dependencyInsight --dependency net.bytebuddy --configuration runtimeClasspath
```

## 8.5 The features you actually adopted

Already used above — point at them:

| Feature | Where in this doc | Replaced |
|---|---|---|
| **Records** | §1.4 `SubmitClaimRequest`, `Coverage`, `ClaimSubmittedEvent` | ~90 lines of Lombok DTO per class |
| **Sealed interfaces** | §1.4 `ClaimStatus` | An enum + a `switch` with a `default` that silently swallowed new cases |
| **Pattern matching for `instanceof`** | §1.4 `isTerminal` | `if (x instanceof Y) { Y y = (Y) x; ... }` |
| **Switch expressions** | §1.4 `customerFacingMessage` | `switch` statements with `break` and fall-through bugs |
| **Text blocks** | §3.6 the JPQL, §7.5 filter expressions | String concatenation with `\n` and escaped quotes |
| **Helpful NPEs** | runtime | `NullPointerException` with no message at all |

```java
// ⟵ "helpful NullPointerException messages that NAME THE EXACT EXPRESSION that was null"
claim.getPolicyHolder().getAddress().getPostcode();

// Java 11:  java.lang.NullPointerException            ← which one was null? good luck.
// Java 17:  java.lang.NullPointerException: Cannot invoke
//           "com.insurer.claims.domain.Address.getPostcode()" because the return value of
//           "com.insurer.claims.domain.PolicyHolder.getAddress()" is null
//
// Enabled by DEFAULT from 15. In 14 it needed -XX:+ShowCodeDetailsInExceptionMessages.
```

## 8.6 GC and the before/after proof

```bash
# ⟵ "Runtime improvements: better G1 throughput, ZGC production-ready with SUB-MILLISECOND
#     PAUSES." We stayed on G1 — ZGC's low pauses cost ~10-15% throughput and extra heap,
#     and our p99 was dominated by I/O, not GC. Knowing WHY you didn't switch is the answer.

# G1 (what we run)
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:MaxRAMPercentage=75

# ZGC (evaluated, not adopted)
-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=70
```

```sql
-- ⟵ "compared GC PAUSE TIME and P99 LATENCY dashboards in CloudWatch BEFORE AND AFTER
--     to confirm NO REGRESSION." The actual query:
fields @timestamp, @message
| filter service = "claims-service"
| stats avg(jvm_gc_pause_seconds_max) as avgMaxPause,
        max(jvm_gc_pause_seconds_max) as worstPause
    by bin(5m), environment
```

## 8.7 The rollout, described as a process

```bash
# 1. jdeps scan (§8.1) → the surprise list
# 2. dependency compatibility matrix (§8.4) → the work list
# 3. feature branch, FULL TEST SUITE AS THE GATE
git checkout -b upgrade/java-17
./gradlew clean build --warning-mode all
# 4. base image change (§1.2): eclipse-temurin:11-jre → 17-jre
# 5. lower environments first
kubectl -n claims-dev set image deploy/claims-service claims-service=...:java17-a1b2c3d
kubectl -n claims-uat set image deploy/claims-service claims-service=...:java17-a1b2c3d
```

```yaml
# 6. ⟵ "CANARIED IN PRODUCTION BEHIND A SMALL PERCENTAGE OF PODS"
#    Simplest credible mechanism: a second Deployment sharing the Service's label selector.
#    1 canary pod alongside 9 stable pods = ~10% of traffic on Java 17.
apiVersion: apps/v1
kind: Deployment
metadata:
  name: claims-service-canary
  namespace: claims
spec:
  replicas: 1                                   # 1 of 10 total → ~10%
  selector:
    matchLabels: { app: claims-service, track: canary }
  template:
    metadata:
      labels:
        app: claims-service        # ← SAME label the Service selects on, so the ALB
        track: canary              #   target group includes it
        jdk: "17"                  # ← lets you slice CloudWatch/X-Ray by JDK version
    spec:
      containers:
        - name: claims-service
          image: 123456789012.dkr.ecr.ap-south-1.amazonaws.com/claims-service:java17-a1b2c3d
```

```bash
# 7. compare, then promote or roll back
kubectl -n claims rollout status deploy/claims-service-canary
# ... watch the p99 and GC dashboards for 24h ...
kubectl -n claims set image deploy/claims-service claims-service=...:java17-a1b2c3d
kubectl -n claims rollout status deploy/claims-service
kubectl -n claims delete deploy/claims-service-canary

# and the rollback, which you should be able to state instantly
kubectl -n claims rollout undo deploy/claims-service
```

---

# PART 9 — Cross-cutting things interviewers reach for

## 9.1 VPC layout and endpoints

```hcl
# terraform/vpc.tf
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  name   = "insurance-prod"
  cidr   = "10.20.0.0/16"

  azs = ["ap-south-1a", "ap-south-1b", "ap-south-1c"]
  # ⟵ /20 private subnets, NOT /24. THE VPC CNI GIVES EVERY POD A REAL IP — a /24 has 251
  #    usable addresses and you run out of IPs before you run out of CPU (§2.1).
  private_subnets = ["10.20.0.0/20",  "10.20.16.0/20", "10.20.32.0/20"]
  public_subnets  = ["10.20.48.0/24", "10.20.49.0/24", "10.20.50.0/24"]
  database_subnets= ["10.20.51.0/24", "10.20.52.0/24", "10.20.53.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = false      # one NAT PER AZ: a NAT is a single point of failure and
  one_nat_gateway_per_az = true   # cross-AZ NAT traffic is charged twice

  enable_dns_hostnames = true
  enable_dns_support   = true

  public_subnet_tags  = { "kubernetes.io/role/elb"          = "1" }   # ← the ALB Controller
  private_subnet_tags = { "kubernetes.io/role/internal-elb" = "1" }   #   discovers subnets
}                                                                      #   by THESE TAGS
```

```hcl
# ⟵ TALKING POINT: "Use VPC ENDPOINTS for S3 (GATEWAY endpoint, FREE) and for SQS, SNS, ECR,
#    Secrets Manager (INTERFACE endpoints) — this KEEPS TRAFFIC ON THE AWS NETWORK instead of
#    TRAVERSING NAT, which is BOTH a SECURITY POSTURE IMPROVEMENT AND A REAL COST SAVING,
#    since NAT GATEWAY DATA PROCESSING CHARGES ADD UP FAST FOR CHATTY SERVICES."
#
#    The arithmetic: NAT processing is ~$0.045/GB. Long polling 3 queues at 20s intervals
#    across 24 pods, plus every ECR image pull, plus X-Ray uploads = terabytes/month.
#    An interface endpoint is ~$0.01/hr + $0.01/GB. It pays for itself almost immediately.

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = module.vpc.vpc_id
  service_name      = "com.amazonaws.ap-south-1.s3"
  vpc_endpoint_type = "Gateway"                       # ⟵ GATEWAY = free, route-table based
  route_table_ids   = module.vpc.private_route_table_ids
}

resource "aws_vpc_endpoint" "interface" {
  for_each = toset([
    "sqs", "sns", "secretsmanager", "kms", "xray", "logs", "monitoring",
    "ecr.api", "ecr.dkr", "sts", "elasticloadbalancing", "eks"
  ])
  vpc_id              = module.vpc.vpc_id
  service_name        = "com.amazonaws.ap-south-1.${each.key}"
  vpc_endpoint_type   = "Interface"                   # ⟵ an ENI in each private subnet
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true    # ← so sqs.ap-south-1.amazonaws.com resolves to the ENI;
}                               #   no code change needed anywhere
```

## 9.2 Security groups — reference, never CIDR

```hcl
# ⟵ TALKING POINT: "the RDS security group only allows port 5432 FROM THE WORKER NODE
#    SECURITY GROUP, NOT FROM A CIDR." A CIDR rule is static and over-broad; a SG reference
#    follows the nodes automatically as the autoscaler adds and removes them, and it stays
#    correct if the subnet CIDR is ever reused.
resource "aws_security_group" "rds" {
  name   = "claims-rds-sg"
  vpc_id = module.vpc.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_nodes" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = module.eks.node_security_group_id   # ← SG reference
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
  description                  = "PostgreSQL from EKS worker nodes only"
}

resource "aws_security_group" "vpc_endpoints" {
  name   = "vpc-endpoints-sg"
  vpc_id = module.vpc.vpc_id
}
resource "aws_vpc_security_group_ingress_rule" "endpoints_from_nodes" {
  security_group_id            = aws_security_group.vpc_endpoints.id
  referenced_security_group_id = module.eks.node_security_group_id
  from_port = 443
  to_port   = 443
  ip_protocol = "tcp"
}
```

### Kubernetes NetworkPolicy — defence in depth inside the cluster

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata: { name: claims-service-default-deny, namespace: claims }
spec:
  podSelector: { matchLabels: { app: claims-service } }
  policyTypes: [Ingress, Egress]
  ingress:
    - from:
        - namespaceSelector: { matchLabels: { name: kube-system } }   # ALB Controller targets
      ports: [{ protocol: TCP, port: 8080 }]
  egress:
    - to: [{ ipBlock: { cidr: 10.20.51.0/24 } }]      # database subnets
      ports: [{ protocol: TCP, port: 5432 }]
    - to: [{ ipBlock: { cidr: 0.0.0.0/0 } }]           # AWS APIs via VPC endpoints
      ports: [{ protocol: TCP, port: 443 }]
    - to: [{ namespaceSelector: {}, podSelector: { matchLabels: { k8s-app: kube-dns } } }]
      ports: [{ protocol: UDP, port: 53 }]
```

### PII / compliance — the insurance angle

```java
/**
 * ⟵ "insurance means PII, so AUDIT LOGGING AND RETENTION POLICIES WERE NON-NEGOTIABLE."
 * Concretely: never log the PAN, Aadhaar, policy holder DOB, or full account numbers.
 * A Logback masking converter is the enforcement mechanism, because "please don't log PII"
 * in a code review is not a control.
 */
public class PiiMaskingConverter extends MessageConverter {

    private static final Pattern PAN     = Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b");
    private static final Pattern AADHAAR = Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b");
    private static final Pattern EMAIL   = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");

    @Override
    public String convert(ILoggingEvent event) {
        String msg = super.convert(event);
        msg = PAN.matcher(msg).replaceAll("[PAN-REDACTED]");
        msg = AADHAAR.matcher(msg).replaceAll("[AADHAAR-REDACTED]");
        msg = EMAIL.matcher(msg).replaceAll(m -> m.group().charAt(0) + "***@***");
        return msg;
    }
}
```

## 9.3 The Jenkinsfile

```groovy
// Jenkinsfile
// ⟵ TALKING POINT: "Jenkins pipeline: build with Gradle, run unit and integration tests,
//    static analysis, build the Docker image, push to ECR WITH THE GIT SHA AS TAG, then
//    helm upgrade or kubectl apply against the target cluster, WITH A MANUAL APPROVAL GATE
//    BEFORE PRODUCTION."
pipeline {
    agent { kubernetes { yamlFile 'jenkins/build-pod.yaml' } }

    environment {
        AWS_REGION   = 'ap-south-1'
        ECR_REGISTRY = '123456789012.dkr.ecr.ap-south-1.amazonaws.com'
        IMAGE_NAME   = 'claims-service'
        GIT_SHA      = "${env.GIT_COMMIT.take(7)}"   // ← the immutable tag. NEVER :latest,
    }                                                //   because :latest makes rollback
                                                     //   ambiguous and breaks image caching

    stages {
        stage('Build & Unit Test') {
            steps { sh './gradlew clean build jacocoTestReport' }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                    jacoco(changeBuildStatus: true, minimumLineCoverage: '75')
                }
            }
        }

        stage('Integration Tests') {
            // Testcontainers: real PostgreSQL + LocalStack (SQS/SNS), not mocks
            steps { sh './gradlew integrationTest' }
        }

        stage('Static Analysis') {
            parallel {
                stage('SonarQube') {
                    steps {
                        withSonarQubeEnv('sonar-prod') { sh './gradlew sonarqube' }
                        timeout(time: 10, unit: 'MINUTES') { waitForQualityGate abortPipeline: true }
                    }
                }
                stage('Dependency CVEs') {
                    steps { sh './gradlew dependencyCheckAnalyze' }
                }
                stage('JDK internals guard') {   // ⟵ the §8.1 check, permanent
                    steps { sh 'jdeps --jdk-internals --multi-release 17 build/libs/*.jar | tee jdeps.txt; ! grep -q "JDK internal API" jdeps.txt' }
                }
            }
        }

        stage('Build & Push Image') {
            steps {
                sh '''
                  aws ecr get-login-password --region $AWS_REGION \
                    | docker login --username AWS --password-stdin $ECR_REGISTRY
                  docker build -t $ECR_REGISTRY/$IMAGE_NAME:$GIT_SHA .
                  docker push $ECR_REGISTRY/$IMAGE_NAME:$GIT_SHA
                '''
                // scan-on-push in ECR; fail the build on CRITICAL findings
                sh '''
                  aws ecr wait image-scan-complete --repository-name $IMAGE_NAME --image-id imageTag=$GIT_SHA
                  CRIT=$(aws ecr describe-image-scan-findings --repository-name $IMAGE_NAME \
                          --image-id imageTag=$GIT_SHA \
                          --query 'imageScanFindings.findingSeverityCounts.CRITICAL' --output text)
                  [ "$CRIT" = "None" ] || [ "$CRIT" = "0" ] || exit 1
                '''
            }
        }

        stage('Deploy DEV')  { steps { deployTo('claims-dev') } }
        stage('Deploy UAT')  { steps { deployTo('claims-uat') } }

        stage('Approve PROD') {
            // ⟵ "WITH A MANUAL APPROVAL GATE BEFORE PRODUCTION"
            when { branch 'main' }
            steps {
                timeout(time: 24, unit: 'HOURS') {
                    input message: "Deploy ${GIT_SHA} to PRODUCTION?", submitter: 'release-managers'
                }
            }
        }

        stage('Deploy PROD') {
            when { branch 'main' }
            steps {
                // migrations FIRST, as a Job (§2.8), and they must be backward compatible
                sh "kubectl -n claims apply -f k8s/base/migration-job.yaml"
                sh "kubectl -n claims wait --for=condition=complete job/claims-db-migrate-$GIT_SHA --timeout=600s"

                sh """
                  helm upgrade --install claims-service ./helm/claims-service \
                    --namespace claims \
                    --set image.tag=$GIT_SHA \
                    --values ./helm/claims-service/values-prod.yaml \
                    --atomic --timeout 10m
                """
                // --atomic = auto-rollback if the release fails to become healthy
                sh "kubectl -n claims rollout status deploy/claims-service --timeout=600s"
            }
        }
    }

    post {
        failure { slackSend channel: '#claims-alerts', color: 'danger',
                            message: "Build ${env.BUILD_NUMBER} failed on ${env.BRANCH_NAME}" }
    }
}

void deployTo(String namespace) {
    sh "helm upgrade --install claims-service ./helm/claims-service " +
       "--namespace ${namespace} --set image.tag=${env.GIT_SHA} " +
       "--values ./helm/claims-service/values-${namespace.replace('claims-','')}.yaml --atomic"
}
```

## 9.4 Resilience4j — the cascading-failure story, coded

> The incident: *"a downstream service slowed, our thread pool filled waiting on it, health checks started failing, pods restarted, load shifted to remaining pods, they fell over too."*

```java
package com.insurer.claims.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class DocumentServiceClient {

    private final RestTemplate restTemplate;

    /**
     * ⟵ THE THREE FIXES, and what each one actually does:
     *
     * @TimeLimiter  — caps how LONG one call can take. Without it, a downstream that never
     *                 responds pins a thread until the socket timeout (potentially minutes).
     *
     * @Bulkhead     — caps HOW MANY threads can be in this call at once. This is the direct
     *                 fix for "our thread pool FILLED waiting on it". 10 concurrent calls max;
     *                 the 11th fails fast instead of queueing. The rest of the Tomcat pool
     *                 stays free to serve requests that DON'T touch the document service.
     *
     * @CircuitBreaker — after 50% of the last 50 calls fail, STOP CALLING for 30 seconds.
     *                 This is what breaks the CASCADE: we stop hammering a service that is
     *                 already struggling, giving it room to recover, and we fail instantly
     *                 rather than slowly.
     */
    @CircuitBreaker(name = "documentService", fallbackMethod = "queueForLater")
    @Bulkhead(name = "documentService", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "documentService")
    public CompletableFuture<DocumentRef> uploadDocument(String claimId, byte[] content) {
        return CompletableFuture.supplyAsync(() ->
                restTemplate.postForObject("/documents", new UploadRequest(claimId, content),
                                           DocumentRef.class));
    }

    /**
     * ⟵ THE ARCHITECTURAL FIX, not just the tactical one: "plus MOVING THE NON-CRITICAL CALL
     * OFF THE REQUEST PATH ONTO SQS." The fallback doesn't fail the claim submission — the
     * document upload is not on the critical path for accepting a claim. It goes on a queue.
     */
    private CompletableFuture<DocumentRef> queueForLater(String claimId, byte[] content,
                                                         Throwable t) {
        log.warn("Document service unavailable, deferring upload for claim {}", claimId, t);
        sqsClient.sendMessage(m -> m.queueUrl(documentQueueUrl)
                                    .messageBody(toJson(new DeferredUpload(claimId, content))));
        return CompletableFuture.completedFuture(DocumentRef.pending(claimId));
    }
}
```

**And the probe fix that stops the restart loop** (this is the part people miss):

```yaml
# ⟵ The incident was made WORSE by liveness probes that checked downstream health.
#    The permanent fix is in §2.2: liveness includes ONLY livenessState. A dependency being
#    slow must degrade the service, NOT kill it. Restarting a pod does not fix someone
#    else's slow database — it just removes capacity at the worst possible moment.
management.endpoint.health.group.liveness.include: livenessState
management.endpoint.health.group.readiness.include: readinessState,db
```

Circuit-breaker state is a metric worth alarming on:

```java
@Bean
public MeterBinder circuitBreakerMetrics(CircuitBreakerRegistry registry) {
    return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
}
// → resilience4j_circuitbreaker_state{name="documentService",state="open"} == 1
//   Alarm on this: an OPEN breaker is a customer-visible symptom, not a cause.
```

## 9.5 Integration testing with Testcontainers + LocalStack

```java
package com.insurer.claims;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.*;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

@SpringBootTest
@Testcontainers
class ClaimSubmissionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
                    .withServices(SNS, SQS, SECRETSMANAGER);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled",   () -> true);   // exercise the migrations!
        registry.add("app.sns.claims-topic-arn",   () -> topicArn);
    }

    /**
     * ⟵ The test that proves the §4.3 idempotency claim, rather than asserting it verbally.
     */
    @Test
    void duplicateEventDeliveryProducesExactlyOneNotification() {
        ClaimSubmittedEvent event = anEvent("evt-123", "CLM-88231");

        processor.process(event);
        processor.process(event);   // at-least-once: the SAME event, twice

        assertThat(auditRepository.countByClaimId("CLM-88231")).isEqualTo(1);
        verify(emailGateway, times(1)).sendClaimConfirmation(any(), any(), any());
    }

    /**
     * ⟵ Proves the §5.1 filter policy without deploying anything.
     */
    @Test
    void healthClaimsNeverReachTheFraudQueue() {
        publisher.publishClaimSubmitted(anEvent().withClaimType("HEALTH"));

        await().during(Duration.ofSeconds(3)).untilAsserted(() ->
            assertThat(receiveAll(fraudQueueUrl)).isEmpty());
    }
}
```

---

# PART 10 — Code → talking point index

Use this the night before. Each row is a claim in the explanation and the file that proves it.

| The claim in the explanation | Section | The artefact |
|---|---|---|
| ALB created by the AWS LB Controller from an Ingress | §2.4 | `ingress.yaml` annotations |
| Pod IPs registered directly as targets (IP mode) | §2.4 | `target-type: ip` |
| Liveness only asks "is the JVM alive" | §1.3, §2.2 | `health.group.liveness.include: livenessState` |
| Readiness can ask "can I serve" | §1.3, §2.2 | `include: readinessState,db` |
| preStop sleep ~5s or you get 502s | §2.2 | `lifecycle.preStop` |
| Graceful shutdown | §1.3 | `server.shutdown: graceful` + `timeout-per-shutdown-phase` |
| `-XX:MaxRAMPercentage=75`, not `-Xmx` | §1.2 | `Dockerfile` `JAVA_TOOL_OPTIONS` |
| IRSA: no static keys | §2.1 | SA annotation + trust policy `sub` condition |
| Spread replicas across AZs | §2.2 | `topologySpreadConstraints` |
| Zero-downtime deploy | §2.2, §2.6 | `maxUnavailable: 0` + PDB |
| VPC CNI IP exhaustion | §2.1, §9.1 | prefix delegation + /20 subnets |
| Multi-AZ ≠ read replica | §3.1 | two Terraform resources, side by side |
| JVM DNS TTL bites on failover | §3.2 | `networkaddress.cache.ttl=5` |
| Socket timeout so threads aren't pinned | §1.3 | `data-source-properties.socketTimeout` |
| Connection-pool arithmetic | §1.3, §3.3 | `maximum-pool-size: 10` + HPA `maxReplicas` |
| RDS Proxy multiplexes & speeds failover | §3.2 | `aws_db_proxy` |
| Oracle PL/SQL via `SimpleJdbcCall` | §3.7 | `PolicyGateway` |
| Expand/contract migrations | §3.4 | changesets 002–005 |
| `DATABASECHANGELOG` | §3.4 | prose + the Job in §2.8 |
| Composite index `(policy_number, claim_date DESC)` | §3.5 | changeset 006 + EXPLAIN before/after |
| N+1 fixed with `JOIN FETCH` | §3.6 | `findWithClaimants` |
| Visibility timeout & the double-processing failure | §4.1, §4.2 | `visibility_timeout_seconds` + heartbeat |
| Long polling | §4.1, §4.2 | `waitTimeSeconds(20)` |
| DLQ + `maxReceiveCount` | §4.1 | `redrive_policy` |
| DLQ alarm so someone gets paged | §6.5 | `dlq_not_empty` |
| Idempotency via unique constraint | §4.3 | `processed_events` + `NotificationProcessor` |
| SQS vs Kafka | §4.5 | comparison table |
| SNS filter policies | §5.1 | `filter_policy` + publisher attributes |
| Raw message delivery & the envelope | §5.1, §5.3 | `raw_message_delivery` + `SnsEnvelope` |
| Queue policy allowing `sns.amazonaws.com` | §4.1 | `aws_sqs_queue_policy` |
| SNS→SQS for durability & back-pressure | §5.4 | comparison table |
| Fluent Bit DaemonSet tails node stdout | §6.1 | `fluent-bit.conf` |
| Structured JSON + MDC correlation ID | §6.2 | `logback-spring.xml` + `CorrelationIdFilter` |
| The Logs Insights query | §6.3 | verbatim |
| Log retention costs money | §6.1 | `log_retention_days` |
| High-cardinality dimensions = spectacular bill | §6.4 | `ClaimMetrics` comment block |
| EMF is the cheap alternative | §6.4 | `EmfMetricEmitter` |
| Alarm on symptoms, not causes | §6.5 | 5xx & p99 alarms |
| `ApproximateAgeOfOldestMessage`, not depth | §6.5 | `queue_age` alarm |
| Composite alarms suppress noise | §6.5 | `aws_cloudwatch_composite_alarm` |
| X-Ray daemon on UDP 2000 | §7.1, §2.2 | DaemonSet + `AWS_XRAY_DAEMON_ADDRESS` |
| `AWSXRayServletFilter` | §7.2 | `XRayConfig` |
| JDBC subsegments | §7.3 | `TracingDataSource` |
| Annotations indexed, metadata not | §7.4 | side-by-side code + filter expression |
| Sampling: 1/sec reservoir + 5% | §7.5 | `xray-sampling-rules.json` |
| Trace context across SQS, and the async gap | §7.6 | producer/consumer + the honest caveat |
| ADOT / OpenTelemetry currency | §7.7 | collector config |
| `jdeps --jdk-internals` | §8.1 | command + CI guard |
| JEP 403 `InaccessibleObjectException` | §8.2 | code + `--add-opens` |
| Nashorn / RMI activation removed | §8.3 | code comment |
| Lombok, Mockito, Byte Buddy floors | §8.4 | the matrix |
| Canary behind a % of pods | §8.7 | `claims-service-canary` |
| VPC endpoints: gateway free, interface paid | §9.1 | `aws_vpc_endpoint` |
| SG reference, not CIDR | §9.2 | `referenced_security_group_id` |
| Git SHA image tag + manual prod gate | §9.3 | `Jenkinsfile` |
| Circuit breakers and bulkheads | §9.4 | `DocumentServiceClient` |

---

## Three closing reminders

1. **Speak at the level of architecture and reasoning.** Every code block here exists so you understand the *shape* of the thing. If you recite exact Terraform module versions and precise cost figures you invite drilling into places you haven't been.
2. **"I worked on X, I was adjacent to Y"** costs almost nothing. The platform team owned cluster provisioning and Terraform; you owned the application side — manifests, probes, SQS consumers, tracing instrumentation. That's a normal division of labour and a completely credible answer.
3. **Never say "exactly once" about Standard SQS.** It is at-least-once. And Multi-AZ is not a read replica.
