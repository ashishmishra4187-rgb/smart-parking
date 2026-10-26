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