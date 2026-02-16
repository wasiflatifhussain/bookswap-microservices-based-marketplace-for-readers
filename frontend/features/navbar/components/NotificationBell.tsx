"use client";

import { Badge } from "@/components/ui/badge";
import { Bell } from "lucide-react";
import { useState } from "react";
import { NotificationList } from "./NotificationList";

interface Props {
  unreadCount: number;
}

export function NotificationBell({ unreadCount }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="relative rounded-full p-2 hover:bg-accent"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <Badge
            className="absolute -right-1 -top-1 px-1 text-xs"
            variant="destructive"
          >
            {unreadCount}
          </Badge>
        )}
      </button>

      {open && <NotificationList onClose={() => setOpen(false)} />}
    </div>
  );
}
