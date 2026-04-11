import { Navbar } from "@/features/navbar/components/Navbar";
import { getNavbarSnapshot } from "@/features/navbar/server/navbar.api";
import { redirect } from "next/navigation";
import type { ReactNode } from "react";

export default async function AppLayout({ children }: { children: ReactNode }) {
  let snapshot;

  try {
    snapshot = await getNavbarSnapshot();
  } catch (e: unknown) {
    if (e instanceof Error && e.message === "UNAUTHENTICATED") {
      redirect("/auth/login");
    }
    throw e;
  }

  return (
    <>
      <Navbar snapshot={snapshot} />
      <main className="pb-10 pt-4">{children}</main>
    </>
  );
}
