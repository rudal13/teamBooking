# 공연예매

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다.
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [공연](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

기능적 요구사항
1. 고객이 티켓을 예매한다.
1. 고객이 결제한다.
1. 티켓이 예매되면 예매수량만큼 공연의 잔여좌석수가 감소한다.
1. 결제가 되면 티켓이 발권가능하다.
1. 고객은 예매를 취소할 수 있다.
1. 예매를 취소하면 결제가 취소되고 예매수량만큼 공연의 잔여좌석수가 증가한다.
1. 결제가 취소되면 티켓 발권이 불가능 상태로 변경된다.
1. 고객은 티켓을 발권한다.

비기능적 요구사항
1. 트랜잭션
   1. 티켓 발권은 결제가 취소된 예매건은 불가해야한다.  Sync 호출 

1. 장애격리
   1. 결제 시스템이 수행되지 않더라도 예매는 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
   1. 발권시스템이 과중되면 결제를 잠시동안 받지 않고 결제취소를 잠시후에 하도록 유도한다  Circuit breaker, fallback
1. 성능
   1. 고객이 마이페이지에서 예매 내역, 결제 상태, 발권 상태 등을 확인할 수 있다.  CQRS



# 체크포인트

- 분석 설계

  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  
http://msaez.io/#/storming/TtlBXy3jjVQGoUCtUfiFniZJdmA3/mine/fb4e809d0e505571ac65733d0aba82ce/-M90v2UtSiD5s2DyYuPH

### 이벤트 도출
![image](https://user-images.githubusercontent.com/19758188/84964189-6ba4a200-b146-11ea-95cb-1eaba4416f26.png)

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/19758188/84964216-824af900-b146-11ea-9711-0e9d0b8ed0b1.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/19758188/84964248-95f65f80-b146-11ea-9731-1d2a1d2e5a62.png)

    - 예매, 공연, 결제, 발권 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/19758188/84964279-ac042000-b146-11ea-81f5-5112ec86ce94.png)

    - 도메인 서열 분리 
        - 예매 : 고객 예매 오류를 최소화 한다. (Core)
        - 결제 : 결제 오류를 최소화 한다. (Supporting)
        - 공연 : 공연 잔여 좌석수 오류를 최소화 한다. (Supporting)
        - 발권 : 고객 발권 오류를 최소화 한다. (Supporting)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)
![image](https://user-images.githubusercontent.com/19758188/84964304-bd4d2c80-b146-11ea-9edb-31ddce1f3788.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![image](https://user-images.githubusercontent.com/19758188/84964323-cd650c00-b146-11ea-9136-7deb4c760109.png)

### 완성된 1차 모형
![image](https://user-images.githubusercontent.com/19758188/84964335-d81fa100-b146-11ea-8778-7e9920348535.png)

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증

# 시나리오1 Coverage Check
![image](https://user-images.githubusercontent.com/19758188/84965203-36e61a00-b149-11ea-833b-f0a289250210.png)

    - 고객이 공연을 예매한다
    - 공연 잔여좌석이 차감된다
        - 잔여 좌석이 예매 수량보다 적을 경우?
    - 예매 성공으로 상태 변경
    - 예매를 결제한다
    - 해당 금액 결제 시 발권가능 상태 티켓이 생성된다
    - 해당 티켓 발권 시 티켓이 발권됨 상태로 변경된다
        - 티켓 발권 주체?
      
# 시나리오2 Coverage Check
![image](https://user-images.githubusercontent.com/19758188/84965203-36e61a00-b149-11ea-833b-f0a289250210.png)

    - 고객이 마이페이지에서 예매 현황을 조회한다
    - 고객이 예매를 취소한다
    - 잔여 좌석수가 증가한다
    - 결제가 취소된다
    - 티켓이 취소됨 상태로 변경된다

### 모델 수정 - 요구사항 커버 확인
![image](https://user-images.githubusercontent.com/19758188/84965244-52512500-b149-11ea-8f0a-5c8a9d741d37.png)

    - 고객이 공연을 예매한다
        - 잔여석 < 예매수 예매 실패로 상태 변경
        - 잔여석 > 예매수
    - 예매 확정 상태 변경
    - 확정된 예매 건 결제 요청
    - 고객이 티켓을 발권한다

### 비기능 요구사항에 대한 검증
![image](https://user-images.githubusercontent.com/19758188/84965258-6137d780-b149-11ea-83f5-ff41709598a8.png)

    - 예매/공연 서비스를 결제 서비스와 격리하여 결제 서비스 장애 시에도 예매할 수 있도록 함
    - 공연 잔여 좌석수가 예매 수량보다 적을 경우, 예매 확정 및 결제가 불가하도록 함
    - 결제 취소 예매 건은 즉시 티켓 발권이 불가 하도록 함

## 헥사고날 아키텍처 다이어그램 도출    
![image](https://user-images.githubusercontent.com/59593156/84855670-9e478f80-b09f-11ea-8ddd-64e8828bed97.png)

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 파이선으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd booking
mvn spring-boot:run

cd dashboard
mvn spring-boot:run

cd pay
mvn spring-boot:run

cd show
mvn spring-boot:run

cd ticketIssuance
mvn spring-boot:run
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 pay 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 하지만, 일부 구현에 있어서 영문이 아닌 경우는 실행이 불가능한 경우가 있기 때문에 계속 사용할 방법은 아닌것 같다. (Maven pom.xml, Kafka의 topic id, FeignClient 의 서비스 id 등은 한글로 식별자를 사용하는 경우 오류가 발생하는 것을 확인하였다)

```
package booking;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Booking_table")
public class Booking {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long showId;
    private Integer qty;
    private Integer amount;
    private String bookStatus;
    private String showName;

    @PostPersist
    public void onPostPersist(){
        Booked booked = new Booked();
        BeanUtils.copyProperties(this, booked);
        booked.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        if ( "BookCancelled".equals(this.getBookStatus()) ) {
            BookingCancelled bookingCanceled = new BookingCancelled();
            BeanUtils.copyProperties(this, bookingCanceled);
            bookingCanceled.publishAfterCommit();
        }
    }

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getShowId() {
        return showId;
    }
    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Integer getQty() {
        return qty;
    }
    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Integer getAmount() {
        return amount;
    }
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getBookStatus() {
        return bookStatus;
    }
    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }

}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package booking;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface BookingRepository extends PagingAndSortingRepository<Booking, Long>{

}

```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 결제취소 -> 발권취소 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (ticketIssuance) TicketIssuanceService.java

package pay.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import pay.Cancelled;

@FeignClient(name="ticketIssuance", url="http://localhost:8088")
public interface TicketIssuanceService {

    @RequestMapping(method= RequestMethod.PUT, value="/ticketIssuances/{bookId}" )
    public void ticketIssue(@PathVariable("bookId") final Long bookId, @RequestBody TicketIssuance ticketIssuance);

}
```

- 결제취소되기 전(@PreUpdate) 발권취소를 요청하도록 처리
```
# Payment.java

 @PreUpdate
    public void onPreUpdate() {

        TicketIssuance ticketIssuance = new TicketIssuance();
    	ticketIssuance.setBookId(this.bookId);
        ticketIssuance.setIssueStatus(this.status);

    	// mappings goes here
    	Application.applicationContext.getBean(TicketIssuanceService.class)
    	.ticketIssue(this.bookId, ticketIssuance);
    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 발권 시스템이 장애가 나면 결제도 못받는다는 것을 확인:


```
# 발권 (ticketIssuance) 서비스를 잠시 내려놓음

#예매처리
http patch localhost:8081/bookings/1 bookStatus="Cancelled"   #Fail
http patch localhost:8081/bookings/2 bookStatus="Cancelled"   #Fail

#발권서비스 재기동
cd ticketIssuance
mvn spring-boot:run

#예매처리
http patch localhost:8081/bookings/1 bookStatus="Cancelled"   #Success
http patch localhost:8081/bookings/2 bookStatus="Cancelled"   #Success
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)



## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


예매가 이루어진 후에 공연시스템으로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 공연 시스템의 처리를 위하여 예매주문이 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 예매 기록을 남긴 후에 곧바로 예매 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
booking - Booking.java
@PostPersist
    public void onPostPersist(){
        Booked booked = new Booked();
        BeanUtils.copyProperties(this, booked);
        booked.publishAfterCommit();
    }
```
- 결제 서비스에서는 예매됨 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
pay - PolicyHandler.java
public void wheneverBooked_PaymentRequest(@Payload Booked booked){
        if(booked.isMe()){
        	
        	Payment payment = new Payment();
        	payment.setBookId(booked.getId());
        	payment.setStatus("PAYED");
        	
        	paymentRepository.save(payment);
            
        }
    }

```

결제 시스템은 예매와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 결제시스템이 유지보수로 인해 잠시 내려간 상태라도 예매를 받는데 문제가 없다:
```

# 결제 서비스 (payment) 를 잠시 내려놓음

#예매처리
http post localhost:8081/bookings showId=1 qty=10 amount=30000 showName="showName1" bookStatus="Booked"     #Success
http post localhost:8081/bookings showId=1 qty=1000 amount=30000 showName="showName1" bookStatus="Booked"   #Success

#예매상태 확인
http get localhost:8081/bookings    # 예매상태는 모두 "Booked"

#공연 서비스 기동
cd show
mvn spring-boot:run

#결제상태 확인
http get localhost:8083/payments    # 결제상태는 모두 "Payed"

```


# AWS

<b>AWS IAM User Access Key 생성</b>

IAM > 액세스 관리 > 사용자 > 보안 자격 증명

액세스 키 만들기 > Access Key, Private Key 별도 보관

----

<b>AWS ECR 생성</b>

ECR > 리포지토리 생성

서비스 별 리포지토리 생성

----

<b>클러스터 생성</b>

eksctl create cluster --name (Cluster-Name) --version 1.15 --nodegroup-name standard-workers --node-type t3.medium --nodes 3 --nodes-min 1 --nodes-max 3

Access Key, Private Key 입력

Default Region: ap-northeast-2

----

<b>클러스터 토큰 가져오기</b>

aws eks --region ap-northeast-2 update-kubeconfig --name (Cluster-Name)

----

<b>Maven 빌드</b>

mvn package -Dmaven.test.skip=true

또는 IntelliJ > Maven > Lifecycle > clean & package

----

<b>도커라이징</b>

docker build -t 271153858532.dkr.ecr.ap-northeast-2.amazonaws.com/(IMAGE_NAME):v1 .

----

<b>ECR 로그인</b>

aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 271153858532.dkr.ecr.ap-northeast-2.amazonaws.com

----

<b>ECR 도커 이미지 푸시</b>

docker push 271153858532.dkr.ecr.ap-northeast-2.amazonaws.com/(IMAGE_NAME):v1

----

<b>컨테이너라이징</b>

디플로이 생성

kubectl create deploy (NAME) --image=271153858532.dkr.ecr.ap-northeast-2.amazonaws.com/(IMAGE_NAME):v1

서비스 생성

kubectl expose deploy (NAME) --type=ClusterIP --port=8080

kubectl create -f deployment.yaml
kubectl replace -f deployment.yaml
kubectl apply -f autoscaling.yaml

----

<b>Pod 테스트</b>

----

https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/5c459b8ad974

----

<b>Cluster에 Kafka 설치</b>

https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/d3169f4b644e

----

<b>시연 테스트</b>

<b>운영 :  서비스 매시 - Circuit Breaking, Retry</b>

https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/377f767d80a6

----

<b>CodeBuild CI/CD</b>

https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/82ae852f57a6

----

<b>Autoscale</b>

https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/d68be193997d

----

<b>부하테스트</b>

----

<b>liveness/readiness probe</b>

https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/556f4336a381

----

<b>Contract Test</b>

https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/7d8e9f30c083

----
