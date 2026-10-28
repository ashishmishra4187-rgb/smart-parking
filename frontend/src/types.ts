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

// mirrors your Java VehicleDto
export interface Vehicle {
  id: number;
  plateNumber: string;
  type: VehicleType;
  primaryVehicle: boolean;
}