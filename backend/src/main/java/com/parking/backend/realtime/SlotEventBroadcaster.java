package com.parking.backend.realtime;

import com.parking.backend.parking.SlotEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SlotEventBroadcaster {

    private final SimpMessagingTemplate messaging;

    public SlotEventBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSlotChanged(SlotEvent event) {
        messaging.convertAndSend("/topic/slots", event);
    }
}