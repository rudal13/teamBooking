package show;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import show.config.kafka.KafkaProcessor;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{

    @Autowired
    BookingRepository bookingRepository;

    @StreamListener(KafkaProcessor.INPUT1)
    public void wheneverTicketQtyChanged_BookingStatusChange(@Payload TicketQtyChanged ticketQtyChanged){

        if(ticketQtyChanged.isMe()){
            if ( "F".equals(ticketQtyChanged.getResultCode()) ) {
                Booking booking = new Booking();
                booking.setId(ticketQtyChanged.getBookId());
                booking.setBookStatus("BookingFailed");
                bookingRepository.save(booking);
            }
            else {
               bookingRepository.findById(ticketQtyChanged.getBookId()).ifPresent(booking -> {
                   ObjectMapper objectMapper = new ObjectMapper();
                   String json = null;

                   try {
                       json = objectMapper.writeValueAsString(booking);
                   } catch (JsonProcessingException e) {
                       e.printStackTrace();
                   }

                   KafkaProcessor kafkaProcessor = Application.applicationContext.getBean(KafkaProcessor.class);
                   MessageChannel outputChannel = kafkaProcessor.outboundTopic3();

                   outputChannel.send(MessageBuilder
                           .withPayload(json)
                           .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                           .build()
                   );
               });
            }
            System.out.println("##### listener BookingStatusChange : " + ticketQtyChanged.toJson());
        }
    }
}
