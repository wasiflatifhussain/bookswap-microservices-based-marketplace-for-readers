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
    <header className="sticky top-0 z-40 border-b border-border/70 bg-card/90 backdrop-blur-md">
      <div className="page-wrap flex h-16 items-center justify-between">
        {/* LEFT */}
        <Link href="/" className="text-lg font-semibold tracking-tight">
          BookSwap <span className="text-primary">Marketplace</span>
        </Link>

        {/* RIGHT */}
        <div className="flex items-center gap-4">
          {/* DESKTOP NAV */}
          <nav className="hidden items-center gap-6 md:flex">
            <Link
              href="/library"
              className="text-sm font-medium text-foreground/85 transition-colors hover:text-foreground"
            >
              My Library
            </Link>

            <Link
              href="/swap"
              className="text-sm font-medium text-foreground/85 transition-colors hover:text-foreground"
            >
              Swap Center
            </Link>

            <span className="flex items-center gap-1 rounded-sm bg-muted/70 px-2.5 py-1 text-sm text-muted-foreground">
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
              className="text-sm text-destructive transition-colors hover:underline"
            >
              Logout
            </button>
          </nav>

          {/* MOBILE ICONS */}
          <div className="flex items-center gap-3 md:hidden">
            <span className="flex items-center gap-1 rounded-sm bg-muted/70 px-2 py-1 text-sm text-muted-foreground">
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
              className="rounded-sm p-2 transition-colors hover:bg-accent"
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
            className="absolute inset-0 bg-black/25 backdrop-blur-[1px]"
            onClick={() => setMobileOpen(false)}
          />

          <div className="absolute right-0 top-0 h-full w-72 border-l border-border/70 bg-card">
            <div className="space-y-4 px-4 py-6 text-sm">
              <Link
                href="/library"
                onClick={() => setMobileOpen(false)}
                className="block rounded-sm px-2 py-2 font-medium hover:bg-accent/50"
              >
                My Library
              </Link>

              <Link
                href="/swap"
                onClick={() => setMobileOpen(false)}
                className="block rounded-sm px-2 py-2 font-medium hover:bg-accent/50"
              >
                Swap Center
              </Link>

              <div className="pt-4 text-muted-foreground">
                {snapshot.userEmail}
              </div>

              <button
                onClick={handleLogout}
                className="block text-left text-destructive hover:underline"
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
