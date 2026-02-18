"use client";

import { NavbarNotification } from "../types";

interface Props {
  onClose: () => void;
  loading: boolean;
  notifications: NavbarNotification[];
}

export function NotificationList({ onClose, loading, notifications }: Props) {
  return (
    <div className="absolute right-0 mt-2 w-96 rounded-md border bg-background shadow">
      <div className="p-4 text-sm">
        <div className="mb-3 flex items-center justify-between">
          <p className="font-medium">Notifications</p>
          <button
            onClick={onClose}
            className="text-xs text-muted-foreground hover:underline"
          >
            Close
          </button>
        </div>

        {loading ? (
          <p className="text-muted-foreground">Loading…</p>
        ) : notifications.length === 0 ? (
          <p className="text-muted-foreground">No notifications.</p>
        ) : (
          <ul className="space-y-4">
            {notifications.map((n) => (
              <li
                key={n.notificationId}
                className={`rounded-md px-3 py-2 ${
                  n.readStatus === "UNREAD" ? "bg-muted/50" : ""
                }`}
              >
                <p className="text-sm font-medium leading-snug">{n.title}</p>

                {n.description && (
                  <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
                    {n.description}
                  </p>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
