"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { auth } from "@/lib/firebase";
import { signInWithEmailAndPassword } from "firebase/auth";
import { Eye, EyeOff } from "lucide-react";
import Link from "next/link";
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
    <main className="w-full max-w-md">
      <Card className="surface-card rounded-md p-8">
        <div className="mb-8">
          <p className="text-xs font-medium uppercase tracking-[0.2em] text-primary/80">
            BookSwap
          </p>
          <SectionHeader
            className="mt-2"
            title="Welcome back"
            subtitle="Sign in to continue trading with the community."
          />
        </div>

        {error && (
          <p className="mb-4 rounded-sm border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {error}
          </p>
        )}

        <div className="space-y-4">
          <input
            className="input-field"
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
          />

          <div className="relative">
            <input
              className="input-field pr-10"
              type={showPassword ? "text" : "password"}
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              className="absolute right-2 top-2.5 rounded p-1 text-muted-foreground hover:bg-accent/70"
              disabled={loading}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </div>

        <Button
          disabled={loading}
          onClick={handleLogin}
          size="lg"
          className="mt-5 w-full"
        >
          {loading ? "Signing in..." : "Login"}
        </Button>

        <p className="mt-4 text-sm text-muted-foreground">
          Don’t have an account?{" "}
          <Link href="/auth/signup" className="font-medium text-foreground underline">
            Sign up
          </Link>
        </p>
      </Card>
    </main>
  );
}
