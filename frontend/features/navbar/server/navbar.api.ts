import { bffFetch } from "@/lib/bff-client";
import { NavbarSnapshot } from "../types";

export async function getNavbarSnapshot(): Promise<NavbarSnapshot> {
  return bffFetch<NavbarSnapshot>("/navbar/snapshot");
}
