export interface NavbarSnapshot {
  userId: string;
  userEmail: string | null;

  walletAvailableAmount: number;
  walletReservedAmount: number;

  unreadNotificationCount: number;

  status: "OK" | "PARTIAL" | "FAILED";
  message: string;
}

export interface NavbarNotification {
  notificationId: string;
  userId: string;
  notificationType: string;
  title: string;
  description: string;
  readStatus: "READ" | "UNREAD";
}
