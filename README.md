# CafeBackendProject

스프링 부트, Redis, Kafka를 활용한 카페 주문 백엔드 API 서버입니다.

***

# 프로젝트 개요

목적 : 포인트 결제 및 실시간 데이터 전송을 보장하는 백엔드 주문 API 서버 구축

### 핵심 목표
- Redis 분산 락을 이용한 결제 동시성 제어
- Kafka를 활용한 주문 데이터 비동기 전송

### 기술 스택

| 분류 | 기술 |
|---|---|
| Framework | Spring Boot 3.3.5, Java 17 |
| Database | MySQL 8.0 |
| Cache / Lock | Redis 7.2 + Redisson |
| Message Broker | Apache Kafka 3.7 (KRaft) |
| Auth | Spring Security + JWT |
| Build | Gradle |

### 폴더 구조

```
src/main/java/com/example/cafebackendproject/
├── auth/                  # 인증 (컨트롤러, 서비스, DTO)
├── common/
│   ├── entity/            # 공통 Base 엔티티 (CreatableEntity 등)
│   ├── exception/         # 전역 예외 처리
│   ├── lock/              # @DistributedLock AOP
│   └── response/          # 공통 응답 래퍼 ApiResponse
├── config/                # Redis, Kafka, Security 설정
├── domain/                # 엔티티 및 리포지토리 (도메인 레이어)
│   ├── menu/
│   ├── order/
│   ├── point/
│   └── user/
├── menu/                  # 메뉴 API (컨트롤러, 서비스, DTO)
├── order/                 # 주문 API (컨트롤러, 서비스, DTO, 이벤트)
├── point/                 # 포인트 API (컨트롤러, 서비스, DTO)
└── security/              # JWT 필터, UserDetails
```

***

# 1. 문제 해결 전략

## 왜 Redis인가?

서버가 다수의 인스턴스로 스케일 아웃된 환경에서는 동일한 사용자가 여러 기기에서 동시에 결제나 충전을 시도할 때 데이터 손실이 발생할 수 있습니다.

**비관적 락의 한계:** DB 레벨에서 락을 점유하므로 정합성은 확실하나, 트래픽 집중 시 DB 커넥션 풀 고갈 및 전체 시스템 병목을 유발합니다.

**낙관적 락의 한계:** 충돌 발생 시 애플리케이션 단에서 재시도 로직을 처리해야 하므로, 결제가 빈번한 시스템에서는 리소스 낭비가 심해집니다.

**선택 이유 (Redis 분산 락):** 인메모리 기반으로 I/O 속도가 압도적으로 빠른 Redis를 도입하여 메인 DB의 부하를 최소화했습니다. Redisson의 `tryLock()`을 사용해 락 획득, TTL, 재진입성을 한 줄로 처리합니다.

## 왜 Kafka인가?

**HTTP 동기 통신의 한계:** 주문 로직 내부에서 HTTP 통신을 사용할 경우, 외부 서버의 응답 지연이나 장애가 우리 서버의 주문 실패 및 스레드 고갈로 직결됩니다.

**Spring 내부 이벤트의 한계:** Spring ApplicationEvent는 같은 JVM 안에서만 동작합니다. 다중 서버 환경에서는 이벤트를 발행한 인스턴스에서만 처리가 일어나고, 나머지 인스턴스는 이벤트를 받지 못합니다.

**선택 이유 (Kafka):** Kafka 브로커가 이벤트를 중앙에서 관리하기 때문에 어느 인스턴스에서 결제가 완료되든 Consumer Group이 브로커에서 이벤트를 가져가 처리할 수 있습니다. 향후 마케팅·알림 등 타 도메인에서 결제 데이터가 필요할 때 기존 로직 수정 없이 토픽만 구독하면 되므로 확장성이 뛰어납니다.

***

# 2. ERD

![img_1.png](img_1.png)

***

# 3. API 명세서

### Auth API

| API명 | 메서드 | Endpoint | 설명 |
|---|---|---|---|
| 회원가입 | POST | `/auth/signup` | 신규 사용자 등록 |
| 로그인 | POST | `/auth/login` | 이메일/비밀번호 검증 후 JWT 발급 |
| 로그아웃 | POST | `/auth/logout` | 현재 토큰 만료 처리 |

### Point API

| API명 | 메서드 | Endpoint | 설명 |
|---|---|---|---|
| 포인트 조회 | GET | `/users/point` | 현재 보유 포인트 잔액 조회 |
| 포인트 충전 | PATCH | `/users/point/charge` | 포인트 충전 (Redis 분산 락 적용) |
| 포인트 내역 | GET | `/users/point/histories` | 충전/차감 이력 조회 |

### Menu API

| API명 | 메서드 | Endpoint | 권한 | 설명 |
|---|---|---|---|---|
| 메뉴 등록 | POST | `/menus` | ADMIN | 새 메뉴 등록 |
| 메뉴 목록 조회 | GET | `/menus` | 누구나 | 판매 중인 메뉴 전체 조회 |
| 전체 메뉴 조회 | GET | `/menus/all` | ADMIN | 품절 포함 전체 조회 |
| 메뉴 단건 조회 | GET | `/menus/{menuId}` | 누구나 | 특정 메뉴 조회 |
| 인기 메뉴 조회 | GET | `/menus/popular` | 누구나 | 최근 7일 인기 메뉴 상위 3개 |
| 메뉴 수정 | PUT | `/menus/{menuId}` | ADMIN | 메뉴 정보 수정 |
| 메뉴 삭제 | DELETE | `/menus/{menuId}` | ADMIN | Soft Delete 처리 |

### Order API

| API명 | 메서드 | Endpoint | 권한 | 설명 |
|---|---|---|---|---|
| 주문 생성 | POST | `/orders` | 회원 | 선택한 메뉴로 주문 생성 |
| 전체 주문 조회 | GET | `/orders` | ADMIN | 모든 사용자의 주문 내역 |
| 주문 상태 변경 | PATCH | `/orders/{orderId}/status` | ADMIN | 주문 상태 업데이트 |
| 내 주문 내역 | GET | `/users/orders` | 회원 | 나의 주문 내역 조회 |
| 결제 | POST | `/orders/payment` | 회원 | 포인트 차감 결제 (Redis 분산 락 + Kafka 이벤트 발행) |

***

# 4. 기술적 판단

## 분산 락 AOP — @Order(0)으로 트랜잭션 외부에서 실행

### 결정

`@DistributedLock` AOP에 `@Order(0)`을 부여해 Spring 트랜잭션 AOP보다 먼저 실행되도록 설정했습니다.

### 근거

Spring의 `@Transactional` AOP는 기본 order가 `Integer.MAX_VALUE`입니다.
`@Order(0)`으로 설정하면 실행 순서가 다음과 같이 보장됩니다.

```
락 획득 → 트랜잭션 시작 → DB 작업 → 트랜잭션 커밋 → 락 해제
```

순서가 반대라면 트랜잭션 커밋 이전에 락이 해제되어, 다른 스레드가 아직 커밋되지 않은 데이터를 읽는 Race Condition이 발생합니다.

## SpEL 기반 동적 락 키 설계

### 결정

`@DistributedLock`의 `key` 속성에 SpEL 표현식을 사용해 메서드 파라미터에서 동적으로 락 키를 추출합니다.

```java
// 사용 측
@DistributedLock(key = "#userId")
public PointResponse charge(Long userId, PointChargeRequest request) { ... }

// AOP 내부 키 추출
private String resolveKey(String expression, String[] paramNames, Object[] args) {
    StandardEvaluationContext context = new StandardEvaluationContext();
    for (int i = 0; i < paramNames.length; i++) {
        context.setVariable(paramNames[i], args[i]);
    }
    return parser.parseExpression(expression).getValue(context, String.class);
}
```

### 근거

하드코딩된 키를 사용하면 어노테이션을 메서드마다 따로 만들어야 합니다.
SpEL로 파라미터 값을 꺼내면 단일 어노테이션으로 `#userId`, `#orderId` 등 다양한 키를 처리할 수 있습니다.
실제 락 키는 `lock:{userId}` 형태로 AOP 내부에서 prefix를 붙여 Redis 키 네임스페이스를 통일합니다.

## Optional로 Kafka 프로듀서 주입 — 테스트 환경 안전성

### 결정

`OrderService`에서 `OrderEventProducer`를 `Optional`로 주입받습니다.

```java
private final Optional<OrderEventProducer> orderEventProducer;

// 결제 완료 후
orderEventProducer.ifPresent(producer -> producer.sendOrderPaidEvent(OrderEventPayload.from(order)));
```

### 근거

`OrderEventProducer`는 `@Profile("!test")`로 테스트 환경에서 빈이 등록되지 않습니다.
일반 의존성 주입으로 받으면 테스트 컨텍스트 로딩 시 `NoSuchBeanDefinitionException`이 발생합니다.
`Optional`로 받으면 빈이 없을 때 `Optional.empty()`가 되어 `ifPresent`에서 자연스럽게 스킵됩니다.
별도의 Mock 설정 없이 테스트 환경에서 Kafka 발행 코드를 무해하게 통과시킬 수 있습니다.

## OrderItem 가격 스냅샷

### 결정

`OrderItem`에 주문 시점의 가격을 별도로 저장합니다.

```java
@Column(nullable = false)
private BigDecimal price; // 주문 시점 가격 (메뉴 가격 * 수량)

public static OrderItem of(Order order, Menu menu, int quantity) {
    return OrderItem.builder()
            .price(menu.getPrice().multiply(BigDecimal.valueOf(quantity)))
            // ...
            .build();
}
```

### 근거

`Menu.price`만 참조하면 메뉴 가격이 변경된 이후에 기존 주문 내역을 조회할 때 금액이 달라집니다.
주문 생성 시점의 가격을 `OrderItem`에 스냅샷으로 저장함으로써 가격 변경과 무관하게 주문 이력의 정확성을 보장합니다.

## 인기 메뉴 — 주문 건수가 아닌 수량 합산 기준

### 결정

최근 7일 인기 메뉴를 집계할 때 주문 건수(`COUNT`) 대신 수량 합산(`SUM(quantity)`)을 기준으로 합니다.

```java
@Query("SELECT oi.menu FROM OrderItem oi JOIN oi.order o " +
        "WHERE o.createdAt >= :since " +
        "GROUP BY oi.menu " +
        "ORDER BY SUM(oi.quantity) DESC")
List<Menu> findPopularMenus(@Param("since") LocalDateTime since, Pageable pageable);
```

### 근거

주문 건수 기준으로 집계하면 아메리카노 1잔짜리 주문 5건과 아메리카노 5잔짜리 주문 1건이 동일하게 취급됩니다.
수량 합산이 실제 판매량을 더 정확하게 반영하므로 인기도 지표로 적합합니다.

## Soft Delete — 주문 이력 정합성 보장

### 결정

메뉴 삭제 시 DB에서 실제로 제거하지 않고 `deleted = true`, `deletedAt` 기록으로 처리합니다.

### 근거

`OrderItem`은 주문 시점의 `menu_id`를 FK로 참조합니다.
메뉴를 물리 삭제하면 해당 메뉴를 포함한 기존 주문 내역 조회 시 참조 무결성 오류가 발생합니다.
Soft Delete로 데이터를 보존하면 과거 주문 이력을 정확하게 조회할 수 있습니다.

## 테스트 환경 인프라 빈 분리 전략

### 배경

`RedissonClient`(분산 락)와 `KafkaTemplate`(이벤트 발행)은 테스트 환경에서 실제 브로커 연결을 시도합니다.
CI 환경에 별도 인프라 없이 `contextLoads()` 테스트가 통과해야 했습니다.

### 결정

실제 인프라 빈(`RedissonConfig`, `KafkaProducerConfig`)에 `@Profile("!test")`를 부여하고, 테스트 환경에서는 각각 대체합니다.

| 빈 | 실제 환경 | 테스트 환경 |
|---|---|---|
| `RedissonClient` | `RedissonConfig` | `TestRedissonConfig` (Mockito Mock) |
| `KafkaTemplate` | `KafkaProducerConfig` | `KafkaAutoConfiguration` exclude |
| `OrderEventProducer` | `@Profile("!test")` 빈 등록 | `Optional.empty()` |

***

# 5. Dockerfile

빌드 스테이지(JDK)와 실행 스테이지(JRE)를 분리한 멀티스테이지 빌드를 사용합니다.
최종 이미지에 JRE만 포함해 이미지 크기를 줄이고, `--platform=linux/amd64`를 고정해 Apple Silicon 로컬 환경과 GitHub Actions CI(amd64) 간 아키텍처 불일치를 방지합니다.

의존성 레이어(`build.gradle` 복사 후 `dependencies`)를 소스 코드 복사보다 먼저 실행해, 소스만 변경된 경우 의존성 재다운로드 없이 캐시를 재사용합니다.

```dockerfile
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

***

# 6. 커밋 컨벤션

### 타입

| 타입 | 설명 |
|---|---|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| refactor | 기능 변경 없는 코드 개선 |
| test | 테스트 코드 추가 및 수정 |
| docs | 문서 수정 |
| chore | 빌드, 설정 파일 수정 |

### 규칙

- 타입은 영어, 제목은 한국어로 작성
- 제목은 마침표 없이 50자 이내

### 예시

```
feat:포인트 충전 API 구현
feat:Redis 분산 락 적용 (포인트 충전/결제 동시성 제어)
fix:테스트 환경 RedissonClient 빈 충돌 해결
chore:Dockerfile 멀티스테이지 빌드 구성
```