"use client";

import { Button } from "@/components/ui/button";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

interface BookDetailActionsProps {
  bookId: string;
  ownerUserId: string;
  currentUserId: string;
}

export function BookDetailActions({
  bookId,
  ownerUserId,
  currentUserId,
}: BookDetailActionsProps) {
  const router = useRouter();
  const [deleting, setDeleting] = useState(false);
  const isOwner = currentUserId === ownerUserId;

  async function handleDelete() {
    const confirmed = window.confirm(
      "Are you sure you want to delete this book listing?",
    );
    if (!confirmed) return;

    setDeleting(true);
    try {
      const res = await fetch(`/api/bff/books/me/delete/${bookId}`, {
        method: "DELETE",
        credentials: "include",
      });

      if (!res.ok) {
        throw new Error(`Delete failed (${res.status})`);
      }

      router.push("/");
      router.refresh();
    } catch (error) {
      console.error(error);
      window.alert("Unable to delete this listing right now.");
    } finally {
      setDeleting(false);
    }
  }

  if (isOwner) {
    return (
      <Button
        variant="destructive"
        onClick={handleDelete}
        disabled={deleting}
        className="w-full sm:w-auto"
      >
        {deleting ? "Deleting..." : "Delete Listing"}
      </Button>
    );
  }

  return (
    <Button asChild className="w-full sm:w-auto">
      <Link href={`/swap/create?targetBookId=${bookId}`}>Send Swap Request</Link>
    </Button>
  );
}
