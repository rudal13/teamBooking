package show.config.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface KafkaProcessor {

    String INPUT1 = "event-in1";
    String OUTPUT1 = "event-out1";
    String OUTPUT2 = "event-out2";
    String OUTPUT3 = "event-out3";

    @Input(INPUT1)
    SubscribableChannel inboundTopic1();

    @Output(OUTPUT1)
    MessageChannel outboundTopic1();

    @Output(OUTPUT2)
    MessageChannel outboundTopic2();

    @Output(OUTPUT3)
    MessageChannel outboundTopic3();

}
