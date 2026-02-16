"use client";

import Link from "next/link";
import { NavbarSnapshot } from "../types";
import { NotificationBell } from "./NotificationBell";

interface NavbarProps {
  snapshot: NavbarSnapshot;
}

export function Navbar({ snapshot }: NavbarProps) {
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

          <span className="text-sm text-muted-foreground">
            ${snapshot.walletBalance.toFixed(2)}
          </span>

          <NotificationBell unreadCount={snapshot.unreadNotifications} />

          <span className="text-sm">{snapshot.userEmail}</span>
        </nav>
      </div>
    </header>
  );
}
