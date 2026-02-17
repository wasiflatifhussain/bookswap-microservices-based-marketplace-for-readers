export interface NavbarSnapshot {
  userId: string;
  userEmail: string | null;

  walletAvailableAmount: number;
  walletReservedAmount: number;

  unreadNotificationCount: number;

  status: "OK" | "PARTIAL" | "FAILED";
  message: string;
}
