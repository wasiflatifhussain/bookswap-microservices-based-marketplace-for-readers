import { headers } from "next/headers";

export async function bffFetch<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  let url = path;
  let cookieHeader: string | undefined;

  // Server-side: build absolute URL + forward cookies
  if (typeof window === "undefined") {
    const hdrs = await headers();
    const host = hdrs.get("host");
    const protocol = process.env.NODE_ENV === "production" ? "https" : "http";

    if (!host) {
      throw new Error("Cannot determine host for server-side fetch");
    }

    url = `${protocol}://${host}${path}`;
    cookieHeader = hdrs.get("cookie") ?? undefined;
  }

  // Since server-side fetch does not automatically include cookies, forward them manually
  const res = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(cookieHeader ? { Cookie: cookieHeader } : {}),
      ...options?.headers,
    },
    cache: "no-store",
  });

  if (res.status === 401) {
    // Let layouts/pages decide what to do
    throw new Error("UNAUTHENTICATED");
  }

  if (!res.ok) {
    throw new Error(`BFF request failed: ${res.status}`);
  }

  return res.json();
}
