"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { auth } from "@/lib/firebase";
import { isValidPassword } from "@/lib/password";
import { createUserWithEmailAndPassword } from "firebase/auth";
import { Eye, EyeOff } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function SignupPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const passwordsMatch = password === confirmPassword;
  const passwordValid = isValidPassword(password);

  async function handleSignup() {
    setError(null);

    if (!passwordValid) {
      setError(
        "Password must be at least 8 characters and include a number and special character.",
      );
      return;
    }

    if (!passwordsMatch) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);

    try {
      const cred = await createUserWithEmailAndPassword(auth, email, password);
      const idToken = await cred.user.getIdToken();

      await fetch("/api/bff/auth/login", {
        method: "POST",
        headers: { Authorization: `Bearer ${idToken}` },
        credentials: "include",
      });

      router.replace("/");
    } catch (e: unknown) {
      if (e instanceof Error) setError(e.message);
      else setError("Signup failed");
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
            title="Create your account"
            subtitle="Start listing and swapping books in minutes."
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

          <input
            className="input-field"
            type={showPassword ? "text" : "password"}
            placeholder="Confirm password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={loading}
          />
        </div>

        <Button
          disabled={loading}
          onClick={handleSignup}
          size="lg"
          className="mt-5 w-full"
        >
          {loading ? "Creating account..." : "Create account"}
        </Button>

        <p className="mt-4 text-sm text-muted-foreground">
          Already have an account?{" "}
          <Link href="/auth/login" className="font-medium text-foreground underline">
            Login
          </Link>
        </p>
      </Card>
    </main>
  );
}
