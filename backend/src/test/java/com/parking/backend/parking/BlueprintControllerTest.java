package com.parking.backend.parking;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlueprintControllerTest {
    @Test void publishedPlanSurvivesRejectedReplacement() {
        var lots=mock(ParkingLotRepository.class);
        var slots=mock(ParkingSlotRepository.class);
        var lot=new ParkingLot();
        var slot=new ParkingSlot();
        slot.setCode("A1");
        when(lots.findById(1L)).thenReturn(Optional.of(lot));
        when(slots.findAllByLotId(1L)).thenReturn(List.of(slot));
        var controller=new BlueprintController(lots,slots,new ObjectMapper());
        var nodes=List.of(new BlueprintController.Node("Gate",0d,0d,"entrance"),
                new BlueprintController.Node("West",10d,2d,"checkpoint"),
                new BlueprintController.Node("A1",12d,2d,"bay"));
        var valid=new BlueprintController.Plan(nodes,List.of(List.of("Gate","West"),List.of("West","A1")),12d,77d);
        controller.publish(1,valid);
        assertEquals(valid,controller.read(1).blueprint());
        var disconnected=new BlueprintController.Plan(nodes,List.of(List.of("West","A1")),null,null);
        assertThrows(ResponseStatusException.class,()->controller.publish(1,disconnected));
        assertEquals(valid,controller.read(1).blueprint());
    }
}
