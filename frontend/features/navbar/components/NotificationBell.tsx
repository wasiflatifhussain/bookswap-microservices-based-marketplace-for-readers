"use client";

import { Badge } from "@/components/ui/badge";
import { Bell } from "lucide-react";

interface Props {
  unreadCount: number;
  open: boolean;
  onToggle: () => void;
}

export function NotificationBell({ unreadCount, onToggle }: Props) {
  return (
    <button
      onClick={onToggle}
      className="relative rounded-full p-2 hover:bg-accent"
    >
      <Bell className="h-5 w-5" />

      {unreadCount === 0 && (
        <Badge
          className="absolute -right-1 -top-1 px-1 text-xs"
          variant="secondary"
        >
          0
        </Badge>
      )}

      {unreadCount > 0 && (
        <Badge
          className="absolute -right-1 -top-1 px-1 text-xs"
          variant="destructive"
        >
          {unreadCount}
        </Badge>
      )}
    </button>
  );
}
