package show;

import show.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{

    @Autowired
    BookingRepository bookingRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverTicketQtyChanged_BookingStatusChange(@Payload TicketQtyChanged ticketQtyChanged){

        if(ticketQtyChanged.isMe()){
            if ( "F".equals(ticketQtyChanged.getResultCode()) ) {
                Booking booking = new Booking();
                booking.setId(ticketQtyChanged.getBookId());
                booking.setBookStatus("BookingFailed");
                bookingRepository.save(booking);
            }
            System.out.println("##### listener BookingStatusChange : " + ticketQtyChanged.toJson());
        }
    }

}
