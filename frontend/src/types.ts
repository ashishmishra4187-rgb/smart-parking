export interface User {
  name: string;
  email: string;
  role: string;
}

// mirrors your Java AuthResponse
export interface AuthResponse {
  token: string;
  name: string;
  email: string;
  role: string;
}

export type VehicleType = "CAR" | "BIKE" | "EV";
export type SlotType = "CAR" | "BIKE" | "EV" | "DISABLED_ACCESSIBLE" | "STAFF" | "VISITOR";
export type SlotStatus = "AVAILABLE" | "RESERVED" | "OCCUPIED" | "MAINTENANCE";
export type ReservationStatus = "ACTIVE" | "COMPLETED" | "EXPIRED" | "CANCELLED" | "NO_SHOW";

// mirrors your Java LotDto
export interface Lot {
  id: number;
  name: string;
  address: string | null;
  reservationMinutes: number;
  total: number;
  available: number;
  reserved: number;
  occupied: number;
  maintenance: number;
}

// mirrors your Java SlotDto
export interface Slot {
  id: number;
  code: string;
  section: string;
  slotType: SlotType;
  status: SlotStatus;
  x: number;
  y: number;
}

// mirrors your Java SlotEvent (sent on /topic/slots)
export interface SlotEvent {
  slotId: number;
  code: string;
  status: SlotStatus;
  updatedAt: string;
}

// mirrors your Java VehicleDto
export interface Vehicle {
  id: number;
  plateNumber: string;
  type: VehicleType;
  primaryVehicle: boolean;
}

// mirrors your Java RecommendationDto (Day 14)
export interface Recommendation {
  slotId: number;
  code: string;
  section: string;
  slotType: string;
  score: number;
  reasons: string[];
}

// mirrors your Java ReservationDto
export interface Reservation {
  id: number;
  publicCode: string;
  slotCode: string;
  plateNumber: string;
  status: ReservationStatus;
  createdAt: string;
  expiresAt: string;
}

// mirrors your Java NotificationDto. "Notification" is already a browser type, so we use another name
export interface AppNotification {
  id: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
}