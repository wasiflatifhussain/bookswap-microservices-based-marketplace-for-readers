"use client";

import { auth } from "@/lib/firebase";
import { signInWithEmailAndPassword } from "firebase/auth";
import { Eye, EyeOff } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Silent session bootstrap (runs on mount)
  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (!user) return;

      setLoading(true);
      try {
        const idToken = await user.getIdToken(true);

        const res = await fetch("/api/bff/auth/login", {
          method: "POST",
          headers: {
            Authorization: `Bearer ${idToken}`,
          },
          credentials: "include",
        });

        if (res.ok) {
          router.replace("/");
        }
      } catch (err) {
        console.error("Silent session bootstrap failed", err);
      } finally {
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, [router]);

  // Manual login (email + password)
  async function handleLogin() {
    setError(null);
    setLoading(true);

    try {
      const cred = await signInWithEmailAndPassword(auth, email, password);
      const idToken = await cred.user.getIdToken(true);

      const res = await fetch("/api/bff/auth/login", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${idToken}`,
        },
        credentials: "include",
      });

      if (!res.ok) {
        throw new Error("Failed to establish session");
      }

      router.replace("/");
    } catch (e: unknown) {
      if (e instanceof Error) {
        setError(e.message);
      } else {
        setError("Login failed");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center">
      <div className="w-full max-w-sm space-y-6">
        <h1 className="text-2xl font-semibold">Login</h1>

        {error && <p className="text-sm text-red-500">{error}</p>}

        <input
          className="w-full rounded border px-3 py-2"
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          disabled={loading}
        />

        <div className="relative">
          <input
            className="w-full rounded border px-3 py-2 pr-10"
            type={showPassword ? "text" : "password"}
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-2 top-2.5 text-muted-foreground"
            disabled={loading}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>

        <button
          disabled={loading}
          onClick={handleLogin}
          className="w-full rounded bg-black py-2 text-white disabled:opacity-50"
        >
          {loading ? "Signing in..." : "Login"}
        </button>

        <p className="text-sm">
          Don’t have an account?{" "}
          <a href="/auth/signup" className="underline">
            Sign up
          </a>
        </p>
      </div>
    </main>
  );
}
