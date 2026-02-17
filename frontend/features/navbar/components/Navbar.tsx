"use client";

import { auth } from "@/lib/firebase";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { useState } from "react";
import {
  fetchNavbarNotifications,
  markNotificationsRead,
} from "../client/navbar.client";
import { NavbarNotification, NavbarSnapshot } from "../types";
import { NotificationBell } from "./NotificationBell";
import { NotificationList } from "./NotificationList";

interface NavbarProps {
  snapshot: NavbarSnapshot;
}

export function Navbar({ snapshot }: NavbarProps) {
  const router = useRouter();

  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(snapshot.unreadNotificationCount);
  const [loading, setLoading] = useState(false);
  const [notifications, setNotifications] = useState<NavbarNotification[]>([]);

  async function handleToggleNotifications() {
    const nextOpen = !open;
    setOpen(nextOpen);

    if (!nextOpen) return; // closing

    // opening
    try {
      setLoading(true);

      const list = await fetchNavbarNotifications();
      setNotifications(list);

      if (unread > 0 && notifications.length > 0) {
        const unreadIds = notifications
          .filter((n) => n.readStatus === "UNREAD")
          .map((n) => n.notificationId);

        if (unreadIds.length > 0) {
          await markNotificationsRead(unreadIds);
          setUnread(0);
        }
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleLogout() {
    try {
      await fetch("/api/bff/auth/logout", {
        method: "POST",
        credentials: "include",
      });

      await auth.signOut();
      router.replace("/auth/login");
    } catch {
      console.error("Logout failed");
    }
  }

  return (
    <header className="border-b">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link href="/" className="text-lg font-semibold">
          BookSwap
        </Link>

        <nav className="flex items-center gap-6">
          <Link href="/library" className="text-sm font-medium">
            My Library
          </Link>

          <Link href="/swap" className="text-sm font-medium">
            Swap Center
          </Link>

          <span className="flex items-center gap-1 text-sm text-muted-foreground">
            <Image
              src="/bc-logo.png"
              alt="Bc"
              width={32}
              height={32}
              unoptimized
            />
            {snapshot.walletAvailableAmount.toFixed(2)}
          </span>

          <div className="relative">
            <NotificationBell
              unreadCount={unread}
              open={open}
              onToggle={handleToggleNotifications}
            />

            {open && (
              <NotificationList
                loading={loading}
                notifications={notifications}
                onClose={() => setOpen(false)}
              />
            )}
          </div>

          {snapshot.userEmail && (
            <>
              <span className="text-sm">{snapshot.userEmail}</span>
              <button
                onClick={handleLogout}
                className="text-sm text-muted-foreground hover:underline"
              >
                Logout
              </button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
