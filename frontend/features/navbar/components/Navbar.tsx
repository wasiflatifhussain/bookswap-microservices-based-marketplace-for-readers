"use client";

import { auth } from "@/lib/firebase";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

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

  const [notifOpen, setNotifOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const [unread, setUnread] = useState(snapshot.unreadNotificationCount);
  const [loading, setLoading] = useState(false);
  const [notifications, setNotifications] = useState<NavbarNotification[]>([]);

  async function handleToggleNotifications() {
    const nextOpen = !notifOpen;
    setNotifOpen(nextOpen);

    if (!nextOpen) return;

    try {
      setLoading(true);
      const list = await fetchNavbarNotifications();
      setNotifications(list);
    } finally {
      setLoading(false);
    }
  }

  async function handleMarkOneRead(notificationId: string) {
    await markNotificationsRead([notificationId]);

    setNotifications((prev) =>
      prev.filter((n) => n.notificationId !== notificationId),
    );

    setUnread((prev) => Math.max(prev - 1, 0));
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

  /* lock scroll ONLY for mobile drawer */
  useEffect(() => {
    document.body.style.overflow = mobileOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileOpen]);

  return (
    <header className="relative z-40 border-b bg-background">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        {/* LEFT */}
        <Link href="/" className="text-lg font-semibold">
          BookSwap
        </Link>

        {/* RIGHT */}
        <div className="flex items-center gap-4">
          {/* DESKTOP NAV */}
          <nav className="hidden items-center gap-6 md:flex">
            <Link href="/library" className="text-sm font-medium">
              My Library
            </Link>

            <Link href="/swap" className="text-sm font-medium">
              Swap Center
            </Link>

            <span className="flex items-center gap-1 text-sm text-muted-foreground">
              <Image
                src="/bc-logo.png"
                alt="BC"
                width={24}
                height={24}
                unoptimized
              />
              {snapshot.walletAvailableAmount.toFixed(2)}
            </span>

            {/* DESKTOP NOTIFICATION */}
            <div className="relative">
              <NotificationBell
                unreadCount={unread}
                open={notifOpen}
                onToggle={handleToggleNotifications}
              />

              {notifOpen && (
                <NotificationList
                  loading={loading}
                  notifications={notifications}
                  onClose={() => setNotifOpen(false)}
                  onMarkRead={handleMarkOneRead}
                />
              )}
            </div>

            <span className="text-sm">{snapshot.userEmail}</span>

            <button
              onClick={handleLogout}
              className="text-sm text-red-500 hover:underline"
            >
              Logout
            </button>
          </nav>

          {/* MOBILE ICONS */}
          <div className="flex items-center gap-3 md:hidden">
            <span className="flex items-center gap-1 text-sm text-muted-foreground">
              <Image
                src="/bc-logo.png"
                alt="BC"
                width={22}
                height={22}
                unoptimized
              />
              {snapshot.walletAvailableAmount.toFixed(2)}
            </span>

            {/* MOBILE NOTIFICATION */}
            <div className="relative">
              <NotificationBell
                unreadCount={unread}
                open={notifOpen}
                onToggle={handleToggleNotifications}
              />

              {notifOpen && (
                <NotificationList
                  loading={loading}
                  notifications={notifications}
                  onClose={() => setNotifOpen(false)}
                  onMarkRead={handleMarkOneRead}
                />
              )}
            </div>

            <button
              onClick={() => setMobileOpen(true)}
              className="rounded-md p-2 hover:bg-accent"
            >
              ☰
            </button>
          </div>
        </div>
      </div>

      {/* MOBILE DRAWER */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div
            className="absolute inset-0 bg-black/30"
            onClick={() => setMobileOpen(false)}
          />

          <div className="absolute right-0 top-0 h-full w-64 bg-background shadow-lg">
            <div className="space-y-4 px-4 py-6 text-sm">
              <Link
                href="/library"
                onClick={() => setMobileOpen(false)}
                className="block font-medium"
              >
                My Library
              </Link>

              <Link
                href="/swap"
                onClick={() => setMobileOpen(false)}
                className="block font-medium"
              >
                Swap Center
              </Link>

              <div className="pt-4 text-muted-foreground">
                {snapshot.userEmail}
              </div>

              <button
                onClick={handleLogout}
                className="block text-left text-red-500 hover:underline"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
