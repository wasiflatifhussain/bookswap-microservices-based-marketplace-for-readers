const BFF_BASE_URL = process.env.NEXT_PUBLIC_BFF_URL ?? "";

export async function bffFetch<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  const res = await fetch(`${BFF_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
    cache: "no-store",
  });

  // TODO: Uncomment after Auth integration and handle 401 globally
  // if (!res.ok) {
  //   throw new Error(`BFF request failed: ${res.status}`);
  // }

  // TODO: Remove after Auth integration
  if (!res.ok) {
    // DEV fallback only
    if (process.env.NODE_ENV === "development") {
      console.warn(`[DEV] BFF request failed: ${path}`);
      throw new Error("DEV_BFF_UNAVAILABLE");
    }

    throw new Error(`BFF request failed: ${res.status}`);
  }

  return res.json();
}
