export interface User { name: string; email: string; role: string; }
export interface AuthResponse { token: string; name: string; email: string; role: string; }

export type VehicleType = "CAR" | "BIKE" | "EV";
export type SlotType = "CAR" | "BIKE" | "EV" | "DISABLED_ACCESSIBLE" | "STAFF" | "VISITOR";
export type SlotStatus = "AVAILABLE" | "RESERVED" | "OCCUPIED" | "MAINTENANCE";
export type ReservationStatus = "ACTIVE" | "COMPLETED" | "EXPIRED" | "CANCELLED" | "NO_SHOW";
export type GateDecision = "APPROVED" | "DENIED" | "RETRY";

// mirrors Java LotDto
export interface Lot {
  id: number; name: string; address: string | null; reservationMinutes: number;
  total: number; available: number; reserved: number; occupied: number; maintenance: number;
}
// mirrors Java SlotDto
export interface Slot {
  id: number; code: string; section: string;
  slotType: SlotType; status: SlotStatus; x: number; y: number;
}
// mirrors Java SlotEvent (sent on /topic/slots)
export interface SlotEvent { slotId: number; code: string; status: SlotStatus; updatedAt: string; }
// mirrors Java VehicleDto
export interface Vehicle { id: number; plateNumber: string; type: VehicleType; primaryVehicle: boolean; }
// mirrors Java RecommendationDto
export interface Recommendation {
  slotId: number; code: string; section: string; slotType: string; score: number; reasons: string[];
}
// mirrors Java ReservationDto
export interface Reservation {
  id: number; publicCode: string; slotCode: string; plateNumber: string;
  status: ReservationStatus; createdAt: string; expiresAt: string;
}
// mirrors Java NotificationDto ("Notification" is already a browser type, so another name)
export interface AppNotification { id: number; type: string; message: string; read: boolean; createdAt: string; }

// ---- manager area: mirrors ManagerDtos in Java ----
export interface ManagerOverview {
  totalSlots: number; available: number; reserved: number; occupied: number; maintenance: number;
  activeReservations: number; carsInside: number;
  entriesLast24h: number; deniedLast24h: number; retriesLast24h: number;
  pendingReviews: number; generatedAt: string;
}
export interface ActiveSessionRow { id: number; plate: string; slotCode: string; entryTime: string; minutesInside: number; }
export interface ActiveReservationRow { id: number; publicCode: string; slotCode: string; plate: string; expiresAt: string; }
export interface DetectionRow {
  id: number; plateRaw: string; plateClean: string | null; confidence: number;
  direction: "ENTRY" | "EXIT"; decision: GateDecision; reason: string;
  needsManualReview: boolean; createdAt: string;
}
export interface ReviewOutcome {
  detectionId: number; status: "APPROVED" | "DENIED"; plate: string | null;
  gateDecision: GateDecision | null; gateReason: string | null;
}