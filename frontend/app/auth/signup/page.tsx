"use client";

import { auth } from "@/lib/firebase";
import { isValidPassword } from "@/lib/password";
import { createUserWithEmailAndPassword } from "firebase/auth";
import { Eye, EyeOff } from "lucide-react";
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

      await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${idToken}`,
        },
      });

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
        <h1 className="text-2xl font-semibold">Sign Up</h1>

        {error && <p className="text-sm text-red-500">{error}</p>}

        <input
          className="w-full rounded border px-3 py-2"
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <div className="relative">
          <input
            className="w-full rounded border px-3 py-2 pr-10"
            type={showPassword ? "text" : "password"}
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-2 top-2.5 text-muted-foreground"
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>

        <input
          className="w-full rounded border px-3 py-2"
          type={showPassword ? "text" : "password"}
          placeholder="Confirm password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
        />

        <button
          disabled={loading}
          onClick={handleSignup}
          className="w-full rounded bg-black py-2 text-white disabled:opacity-50"
        >
          Create account
        </button>

        <p className="text-sm">
          Already have an account?{" "}
          <a href="/auth/login" className="underline">
            Login
          </a>
        </p>
      </div>
    </main>
  );
}
