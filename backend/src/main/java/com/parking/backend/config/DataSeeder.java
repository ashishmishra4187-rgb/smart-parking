package com.parking.backend.config;

import com.parking.backend.parking.ParkingLot;
import com.parking.backend.parking.ParkingLotRepository;
import com.parking.backend.parking.ParkingSection;
import com.parking.backend.parking.ParkingSectionRepository;
import com.parking.backend.parking.ParkingSlot;
import com.parking.backend.parking.ParkingSlotRepository;
import com.parking.backend.parking.SlotType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ParkingLotRepository lots;
    private final ParkingSectionRepository sections;
    private final ParkingSlotRepository slots;

    public DataSeeder(ParkingLotRepository lots,
                      ParkingSectionRepository sections,
                      ParkingSlotRepository slots) {
        this.lots = lots;
        this.sections = sections;
        this.slots = slots;
    }

    @Override
    public void run(String... args) {
        if (lots.count() > 0) {
            return; // already seeded, so don't duplicate
        }

        ParkingLot lot = new ParkingLot();
        lot.setName("Main Campus Lot");
        lot.setAddress("Gate 1");
        lot.setEntranceX(0);
        lot.setEntranceY(0);
        lot = lots.save(lot);

        String[] codes = {"A", "B", "C"};
        for (int s = 0; s < codes.length; s++) {
            ParkingSection section = new ParkingSection();
            section.setLot(lot);
            section.setCode(codes[s]);
            section.setCapacity(8);
            section = sections.save(section);

            for (int n = 1; n <= 8; n++) {
                ParkingSlot slot = new ParkingSlot();
                slot.setSection(section);
                slot.setCode(codes[s] + n);
                slot.setX(n * 10);
                slot.setY((s + 1) * 20);
                if (codes[s].equals("A") && n == 1) slot.setSlotType(SlotType.DISABLED_ACCESSIBLE);
                if (codes[s].equals("B") && n >= 7) slot.setSlotType(SlotType.BIKE);
                if (codes[s].equals("C") && n >= 7) slot.setSlotType(SlotType.EV);
                slots.save(slot);
            }
        }

    }
}