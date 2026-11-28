package com.parking.backend.parking;

import com.parking.backend.common.NotFoundException;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestController
public class BlueprintController {
    public record Node(@NotBlank @Size(max=80) String id, @NotNull @Min(-100000) @Max(100000) Double x,
                       @NotNull @Min(-100000) @Max(100000) Double y,
                       @NotNull @Pattern(regexp="entrance|junction|checkpoint|bay") String kind) {}
    public record Plan(@NotEmpty @Size(max=2000) List<@NotNull @Valid Node> nodes,
                       @NotEmpty @Size(max=6000) List<@NotNull @Size(min=2,max=2) List<@NotBlank String>> lanes,
                       @DecimalMin("-90") @DecimalMax("90") Double gateLatitude,
                       @DecimalMin("-180") @DecimalMax("180") Double gateLongitude) {}
    public record View(String lotName, Plan blueprint) {}

    private final ParkingLotRepository lots;
    private final ParkingSlotRepository slots;
    private final ObjectMapper mapper;
    public BlueprintController(ParkingLotRepository lots, ParkingSlotRepository slots, ObjectMapper mapper) {
        this.lots=lots; this.slots=slots; this.mapper=mapper;
    }
    private ParkingLot lot(long id) { return lots.findById(id).orElseThrow(()->new NotFoundException("Lot not found")); }
    private static void require(boolean condition,String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);
    }
    @GetMapping("/api/parking/lots/{id}/blueprint")
    public View read(@PathVariable long id) {
        var lot=lot(id);
        return new View(lot.getName(),lot.getBlueprintJson()==null?null:mapper.readValue(lot.getBlueprintJson(),Plan.class));
    }
    @Transactional
    @PutMapping("/api/manager/lots/{id}/blueprint")
    public View publish(@PathVariable long id,@Valid @RequestBody Plan plan) {
        var lot=lot(id);
        Map<String,Set<String>> graph=new HashMap<>();
        Set<String> locations=new HashSet<>(), bays=new HashSet<>();
        String entrance=null;
        for (Node n:plan.nodes()) {
            require(graph.put(n.id(),new HashSet<>())==null,"Duplicate point ID");
            require(locations.add(n.x()+":"+n.y()),"Points must have distinct coordinates");
            if(n.kind().equals("entrance")) { require(entrance==null,"Provide one entrance"); entrance=n.id(); }
            if(n.kind().equals("bay")) bays.add(n.id());
        }
        require(entrance!=null,"Provide an entrance");
        Set<String> actual=new HashSet<>();
        slots.findAllByLotId(id).forEach(slot->actual.add(slot.getCode()));
        require(!actual.isEmpty() && actual.equals(bays),"Bay IDs must match the lot's existing bay codes");
        for(var edge:plan.lanes()) {
            String a=edge.get(0), b=edge.get(1);
            require(!a.equals(b) && graph.containsKey(a) && graph.containsKey(b),"Invalid lane endpoints");
            graph.get(a).add(b); graph.get(b).add(a);
        }
        for(String bay:bays) require(graph.get(bay).size()==1,"A bay must end a lane");
        var reached=new HashSet<String>(); var queue=new ArrayDeque<String>(); queue.add(entrance);
        while(!queue.isEmpty()) {String n=queue.remove(); if(reached.add(n)) queue.addAll(graph.get(n));}
        require(reached.size()==graph.size(),"Every point must be reachable from the entrance");
        require((plan.gateLatitude()==null)==(plan.gateLongitude()==null),"Provide both GPS coordinates");
        if(plan.gateLatitude()!=null) lot.setGateLocation(plan.gateLatitude(),plan.gateLongitude());
        lot.setBlueprintJson(mapper.writeValueAsString(plan));
        return new View(lot.getName(),plan);
    }
}
