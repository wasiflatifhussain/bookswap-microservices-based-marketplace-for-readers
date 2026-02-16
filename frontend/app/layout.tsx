import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import type { ReactNode } from "react";
import "./globals.css";

import { Navbar } from "@/features/navbar/components/Navbar";
import { getNavbarSnapshot } from "@/features/navbar/server/navbar.api";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "BookSwap",
  description: "Trade books with other readers",
};

export default async function RootLayout({
  children,
}: {
  children: ReactNode;
}) {
  // TODO: Uncomment when Auth integration is done and handle unauthenticated state globally
  // const snapshot = await getNavbarSnapshot();

  // TODO: Remove snapshot below after Auth is integrated
  let snapshot;

  try {
    snapshot = await getNavbarSnapshot();
  } catch {
    snapshot = {
      userEmail: "dev@bookswap.local",
      walletBalance: 0,
      unreadNotifications: 0,
    };
  }

  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} min-h-screen bg-background text-foreground antialiased`}
      >
        <Navbar snapshot={snapshot} />
        {children}
      </body>
    </html>
  );
}
