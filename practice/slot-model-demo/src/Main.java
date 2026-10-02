import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<ParkingSlot> slots = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            slots.add(new ParkingSlot("A" + i));
        }

        slots.get(2).setStatus(SlotStatus.RESERVED);
        slots.get(4).setStatus(SlotStatus.OCCUPIED);
        slots.get(7).setStatus(SlotStatus.MAINTENANCE);

        System.out.println("All slots:");
        for (ParkingSlot slot : slots) {
            System.out.println(slot);
        }

        System.out.println("Available slots:");
        for (ParkingSlot slot : slots) {
            if (slot.getStatus() == SlotStatus.AVAILABLE) {
                System.out.println(slot);
            }
        }
    }
}