# CafeBackendProject

***

위 프로그램은 스프링 부트, 레디스, 카프카를 활용하여 구축된 백엔드 API 서버입니다.

***

# 프로젝트 개요

목적 : 포인트 결제 및 실시간 데이터 전송을 보장하는 백엔드 주문 API 서버 구축

### 핵심 목표
- Redis를 이용한 결제 동시성 제어
- Kafka를 활용한 주문 데이터 비동기 전송

### 기술 스택 
- Framework : Spring Boot 4.0.5
- Database : MySQL
- MessagePassing : Kafka

### 폴더 구조

***

# 1. 문제 해결 전략 수립 (보충 예정)

## 왜 Redis인가?

현재 서버는 다수의 인스턴스 환경에서 동작하는 것을 가정하고 만들예정입니다. 동일한 클라이언트가 서비스에 동시에 접근을 할 경우에 데이터의 손실이 발생할 수 있습니다. 이를 위해서 분산 환경을 관리해주기 위한 Lock이 필수적인 상황 입니다.

비관적 락이 안되는 이유 : 데이터베이스의 병목 가능성과 트래픽일 몰릴경우에 생길 성능저하

낙관적 락이 안되는 이유 : 충돌이 많은 상황을 가정해보았을 때의 DB 리소스 낭비

## 왜 Kafka인가? 

Http 방식 대신 Kafka를 사용한 이유 : Http를 사용할 경우 Http는 동기 통신이기 때문에 트래픽이 몰렸을 경우, 응답지연으로 이어질 가능성이 높다. 하지만, Kafaka는 비동기 통신이기 때문에 트래픽이 몰려 외부 서버가 작동 하지 않더라도 전달된 작업 내용은 Kafka로 전달되기 때문에 결합도를 낮추기 위해서 사용 

향후 예를 들어 결제 내역을 필요로하는 다른 기능이 추가될 경우, 기존의 로직 수정 없이 Kafka의 토픽을 사용하는 것으로 기능을 구현 할 수 있기에 기능 확장에 매우 용이하다.

***

# 2. ERD

![img_1.png](img_1.png)

# 3. API 명세서

### [Auth API]

회원가입 API

설명 : ```신규 사용자의 정보를 받아 데이터베이스에 등록합니다.```

Method: ```POST```

Endpoint: ```/auth/signup```

로그인 API

설명: ```이메일과 비밀번호를 검증한 후, 인증된 사용자에게 다른 API 호출 시 사용할 수 있는 토큰을 발급합니다.```

Method: ```POST```

Endpoint: ```/auth/login```

로그아웃 API

설명: ```사용자의 현재 토큰을 만료시켜 더 이상 해당 토큰으로 API를 호출할 수 없도록 막습니다.```

Method: ```POST```

Endpoint: ```/auth/logout```

***

### [Point API]

내 포인트 확인 API

설명 : ```나의 현재 보유 포인트 잔액을 조회합니다.```

Method: ```GET```

Endpoint: ```/users/point```

내 포인트 충전 API

설명 : ```특정 사용자의 포인트를 충전하고 결제 금액만큼 잔액을 증가시킵니다.```

Method: ```PATCH```

Endpoint: ```/users/point/charge```

내 포인트 내역 확인 API

설명 : ```특정 사용자의 포인트 충전과 차감 이력을 조회합니다.```

Method: ```GET```

Endpoint: ```/users/point/histories```

***

### [Menu API]

커피 메뉴 등록 API

설명 : ```새로운 커피 메뉴의 이름과 가격 정보를 시스템에 등록합니다.```

Method: ```POST```

Endpoint: ```/menus```

커피 메뉴 조회 API

설명 : ```전체 커피 메뉴 목록과 가격 정보를 조회합니다.```

Method: ```GET```

Endpoint: ```/menus```

커피 메뉴 수정 API

설명 : ```등록된 특정 커피 메뉴의 정보(이름, 가격 등)를 수정합니다.```

Method: ```PUT```

Endpoint: ```/menus/{menuId}```

커피 메뉴 삭제 API

설명 : ```특정 커피 메뉴를 시스템에서 판매 중지 처리합니다. Soft Delete 예정```

Method: ```DELETE```

Endpoint: ```/menus/{menuId}```

인기 메뉴 확인 API

설명 : ```최근 주문 횟수가 가장 많은 인기 메뉴 목록을 조회합니다.```

Method: GET

Endpoint: ```/menus/populer```

***

### [Order API]

커피 주문 생성 API

설명 : ```사용자가 선택한 커피 메뉴들로 새로운 주문을 생성합니다.```

Method: ```POST```

Endpoint: ```/orders```

커피 주문 전체 조회 API

설명 : ```모든 사용자의 전체 주문 내역을 조회합니다. ```

Method: ```GET```

Endpoint: ```/orders```

주문 상태 변경 API

설명 : ```특정 주문의 진행 상태(결제 완료, 제조 중, 제조 완료 등)를 업데이트합니다.```

Method: ```PATCH```

Endpoint: ```/orders/{orderId}/status```

내 주문 내역 조회 API

설명 : ```나의 과거 주문 내역 및 결제한 상세 메뉴 정보를 조회합니다.```

Method: ```GET```

Endpoint: ```/users/orders```

결제 API

설명 : ```주문 금액만큼 나의 포인트를 차감하여 결제를 진행하고, 성공 시 주문 내역을 데이터 플랫폼(Kafka)으로 실시간 전송합니다.```

Method: ```POST```

Endpoint: ```/orders/payment```

