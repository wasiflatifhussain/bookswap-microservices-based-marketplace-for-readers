import { NavbarNotification } from "../types";

export async function fetchNavbarNotifications(): Promise<
  NavbarNotification[]
> {
  const res = await fetch("/api/bff/navbar/notifications?unreadOnly=true", {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });

  if (!res.ok) throw new Error(`notifications failed: ${res.status}`);
  return res.json();
}

export async function markNotificationsRead(
  notificationIds: string[],
): Promise<void> {
  if (notificationIds.length === 0) return;

  const res = await fetch("/api/bff/navbar/notifications/read", {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(notificationIds),
  });

  if (!res.ok) {
    throw new Error(`mark read failed: ${res.status}`);
  }
}
