# -구현 (실행)
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

- DDD의 적용
1) entity
package show;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Entity
@Table(name="Booking_table")
public class Booking {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long showId;
    private String showName;
    private Integer qty;
    private Integer amount;
    private String bookStatus;

    @PostPersist
    public void onPostPersist(){
        Booked booked = new Booked();
        BeanUtils.copyProperties(this, booked);
        booked.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        BookingCanceled bookingCanceled = new BookingCanceled();
        BeanUtils.copyProperties(this, bookingCanceled);
        bookingCanceled.publishAfterCommit();
    }

    public Long getId() { return id; }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getShowId() {
        return showId;
    }
    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public String getShowName() {
        return showName;
    }
    public void setShowName(String showName) { this.showName = showName;  }

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

2) Repository 예시
package show;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface BookingRepository extends PagingAndSortingRepository<Booking, Long>{

}

3) 적용 후 API 테스트 결과


- 동기식 호출 과 Fallback 처리
1) 예매취소 - 결제취소 - 발권취소
booking - Booking.java
@PostUpdate
    public void onPostUpdate(){
        BookingCancelled bookingCanceled = new BookingCancelled();
        BeanUtils.copyProperties(this, bookingCanceled);
        bookingCanceled.publishAfterCommit();
    }

pay - PolicyHandler.java
@StreamListener(KafkaProcessor.INPUT_2)
    public void wheneverBookingCancelled_PaymentCancel(@Payload BookingCancelled bookingCancelled){

        if(bookingCancelled.isMe()) {
        	Payment paymentCanceled = paymentRepository.findByBookId(bookingCancelled.getId());
        	paymentCanceled.setStatus("Cancelled");
        	paymentRepository.save(paymentCanceled);
        	
        }
    }

pay - Payment.java
 @PreUpdate
    public void onPreUpdate() {

        TicketIssuance ticketIssuance = new TicketIssuance();
    	ticketIssuance.setBookId(this.bookId);
        ticketIssuance.setIssueStatus(this.status);

    	// mappings goes here
    	Application.applicationContext.getBean(TicketIssuanceService.class)
    	.ticketIssue(this.bookId, ticketIssuance);
    }

pay - TicketIssuanceService.java
@FeignClient(name="ticketIssuance", url="http://localhost:8088")
public interface TicketIssuanceService {

    @RequestMapping(method= RequestMethod.PUT, value="/ticketIssuances/{bookId}" )
    public void ticketIssue(@PathVariable("bookId") final Long bookId, @RequestBody TicketIssuance ticketIssuance);

}

ticketIssuance - TicketIssuance.java
@PostUpdate
    public void onPostUpdate() {
        IssueStatusChanged issueStatusChanged = new IssueStatusChanged();
        issueStatusChanged.setId(this.getId());
        issueStatusChanged.setBookId(this.getBookId());
        issueStatusChanged.setIssueStatus(this.getIssueStatus());

        ObjectMapper objectMapper = new ObjectMapper();
        String json = null;

        try {
            json = objectMapper.writeValueAsString(issueStatusChanged);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON format exception", e);
        }

        KafkaProcessor processor = Application.applicationContext.getBean(KafkaProcessor.class);
        MessageChannel outputChannel = processor.outboundTopic();

        outputChannel.send(MessageBuilder
                .withPayload(json)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build());
    }


- 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
1) 예매 - 결제 - 발권상태
booking - Booking.java
@PostPersist
    public void onPostPersist(){
        Booked booked = new Booked();
        BeanUtils.copyProperties(this, booked);
        booked.publishAfterCommit();
    }

pay - PolicyHandler.java
public void wheneverBooked_PaymentRequest(@Payload Booked booked){
        if(booked.isMe()){
        	
        	Payment payment = new Payment();
        	payment.setBookId(booked.getId());
        	payment.setStatus("PAYED");
        	
        	paymentRepository.save(payment);
            
        }
    }

ticketIssuance - PolicyHandler.java
@StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayed_CreateIssue(@Payload Payed payed){
        if(payed.isMe()){
            System.out.println("##### listener CreateIssue : " + payed.toJson());
            TicketIssuance ticketIssuance = new TicketIssuance();
            ticketIssuance.setBookId(payed.getBookId());
            ticketIssuance.setIssueStatus("Issuable");
            ticketIssuanceRepository.save(ticketIssuance);
            System.out.println("Your ticket is now " + ticketIssuance.getIssueStatus());
        }
    }


2) 예매 - 잔여좌석수변경 - 예매실패
booking - Booking.java
@PostPersist
    public void onPostPersist(){
        Booked booked = new Booked();
        BeanUtils.copyProperties(this, booked);
        booked.publishAfterCommit();
    }

show - PolicyHandler.java
@StreamListener(KafkaProcessor.INPUT_1)
    public void wheneverBooked_TicketQtyChange(@Payload Booked booked){
        String resultCode = "S";
        if ("Booked".equals(booked.getEventType())) {
            if(booked.isMe()){
                try {
                    Optional<Show> showOption = showRepository.findById(booked.getShowId());

                    if (showOption.isPresent()) {
                        if (showOption.get().getRemainCount() >= booked.getQty()){
                            showOption.get().setRemainCount(showOption.get().getRemainCount() - booked.getQty());
                            showRepository.save(showOption.get());
                        } else {
                            resultCode = "F";
                        }
                    } else {
                        resultCode = "F";
                    }
                } catch (Exception e) {
                    resultCode = "F";
                } finally {
                    if ("F".equals(resultCode)) {
                       TicketQtyChanged ticketQtyChanged = new TicketQtyChanged();
                       ticketQtyChanged.setBookId(booked.getId());
                       ticketQtyChanged.setResultCode(resultCode);

                       ObjectMapper objectMapper = new ObjectMapper();
                       String json = null;

                       try {
                           json = objectMapper.writeValueAsString(ticketQtyChanged);
                       } catch (JsonProcessingException e) {
                            e.printStackTrace();
                       }

                       KafkaProcessor kafkaProcessor = Application.applicationContext.getBean(KafkaProcessor.class);
                       MessageChannel outputChannel = kafkaProcessor.outboundTopic1();

                       outputChannel.send(MessageBuilder
                               .withPayload(json)
                               .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                               .build()
                       );
                    }
                }
            }
        }
    }

booking - PolicyHandler.java
@StreamListener(KafkaProcessor.INPUT1)
    public void wheneverTicketQtyChanged_BookingStatusChange(@Payload TicketQtyChanged ticketQtyChanged){

        if(ticketQtyChanged.isMe()){
            if ( "F".equals(ticketQtyChanged.getResultCode()) ) {
                Optional<Booking> bookingOptional = bookingRepository.findById(ticketQtyChanged.getBookId());
                bookingOptional.get().setBookStatus("Failed");
                bookingRepository.save(bookingOptional.get());
            }
            System.out.println("##### listener BookingStatusChange : " + ticketQtyChanged.toJson());
        }
    }


