package com.parking.backend.config;

import com.parking.backend.parking.ParkingLot;
import com.parking.backend.parking.ParkingLotRepository;
import com.parking.backend.parking.ParkingSection;
import com.parking.backend.parking.ParkingSectionRepository;
import com.parking.backend.parking.ParkingSlot;
import com.parking.backend.parking.ParkingSlotRepository;
import com.parking.backend.parking.SlotType;
import com.parking.backend.user.Role;
import com.parking.backend.user.User;
import com.parking.backend.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ParkingLotRepository lots;
    private final ParkingSectionRepository sections;
    private final ParkingSlotRepository slots;
    private final UserRepository users;

    public DataSeeder(ParkingLotRepository lots,
                      ParkingSectionRepository sections,
                      ParkingSlotRepository slots,
                      UserRepository users) {
        this.lots = lots;
        this.sections = sections;
        this.slots = slots;
        this.users = users;
    }

    @Override
    public void run(String... args) {
        // temporary demo user (replaced by real login on Day 8-9)
        if (users.count() == 0) {
            User demo = new User();
            demo.setName("Demo Driver");
            demo.setEmail("demo@parking.local");
            demo.setPasswordHash("temporary-placeholder");
            demo.setRole(Role.DRIVER);
            users.save(demo);
        }

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