import { auth } from "@/lib/firebase";

export async function apiFetch(
  input: RequestInfo,
  init?: RequestInit,
): Promise<Response> {
  const res = await fetch(input, {
    ...init,
    credentials: "include", // For send cookies
  });

  if (res.status !== 401) {
    return res;
  }

  // 401 handling
  const user = auth.currentUser;
  if (!user) {
    throw new Error("Not authenticated");
  }

  // If res did not return, force refresh Firebase ID token
  const freshIdToken = await user.getIdToken(true);

  // Re-login to BFF for session update with new ID token
  await fetch("/api/auth/login", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${freshIdToken}`,
    },
    credentials: "include",
  });

  // Retry original request once
  return fetch(input, {
    ...init,
    credentials: "include",
  });
}
