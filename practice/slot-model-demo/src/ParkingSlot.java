public class ParkingSlot {
    private String code;
    private SlotStatus status;

    public ParkingSlot(String code) {
        this.code = code;
        this.status = SlotStatus.AVAILABLE;
    }

    public String getCode() {
        return code;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
    public boolean reserve() {
        if (status != SlotStatus.AVAILABLE) {
            return false;
        }
        status = SlotStatus.RESERVED;
        return true;
    }
    @Override
    public String toString() {
        return code + " [" + status + "]";
    }
}