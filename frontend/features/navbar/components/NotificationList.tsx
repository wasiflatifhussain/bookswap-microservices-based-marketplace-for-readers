"use client";

import { CircleCheck } from "lucide-react";
import { NavbarNotification } from "../types";

interface Props {
  onClose: () => void;
  loading: boolean;
  notifications: NavbarNotification[];
  onMarkRead: (id: string) => void;
}

export function NotificationList({
  onClose,
  loading,
  notifications,
  onMarkRead,
}: Props) {
  return (
    <div
      className="
        absolute
        top-full
        mt-2
        z-50
        w-[80vw]
        max-w-md
        rounded-md
        border border-border
        bg-card
        right-0
        translate-x-[10%]
        max-h-[60vh]
        overflow-y-auto
      "
    >
      {/* STICKY HEADER */}
      <div
        className="
          sticky
          top-0
          z-10
          flex
          items-center
          justify-between
          border-b border-border/70
          bg-card
          px-4
          py-3
          text-sm
        "
      >
        <p className="font-medium">Unread Notifications</p>
        <button
          onClick={onClose}
          className="text-xs text-muted-foreground transition-colors hover:text-foreground hover:underline"
        >
          Close
        </button>
      </div>

      {/* CONTENT */}
      <div className="p-4 pt-3 text-sm">
        {loading ? (
          <p className="text-muted-foreground">Loading…</p>
        ) : notifications.length === 0 ? (
          <p className="text-muted-foreground">All caught up!</p>
        ) : (
          <ul className="space-y-3">
            {notifications.map((n) => (
              <li
                key={n.notificationId}
                className="flex items-start justify-between gap-3 rounded-sm border border-border bg-muted/45 px-3 py-2"
              >
                <div>
                  <p className="font-medium leading-snug">{n.title}</p>
                  {n.description && (
                    <p className="mt-1 text-muted-foreground">
                      {n.description}
                    </p>
                  )}
                </div>

                <button
                  onClick={() => onMarkRead(n.notificationId)}
                  className="mt-1 rounded-sm p-1 text-muted-foreground hover:bg-gray-300 hover:text-foreground"
                  title="Mark as read"
                >
                  <CircleCheck className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
