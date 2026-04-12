"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { StatPill } from "@/components/ui/stat-pill";
import { SwapItem } from "@/features/swap/types";
import { ArrowRightLeft } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useState } from "react";

interface SwapCardProps {
  item: SwapItem;
  mode: "sent" | "received";
  onDone: (swapId: string) => void;
}

function formatCoins(value: number | null | undefined): string {
  return typeof value === "number" ? value.toFixed(2) : "0.00";
}

function looksLikeUrl(value: string | null | undefined): boolean {
  if (!value) return false;
  return value.startsWith("http://") || value.startsWith("https://");
}

function BookMini({
  bookId,
  title,
  author,
  valuation,
  media,
  label,
}: {
  bookId?: string | null;
  title: string;
  author: string;
  valuation: number | null | undefined;
  media: string | null | undefined;
  label: string;
}) {
  return (
    <div className="grid grid-cols-[96px_1fr_auto] items-center gap-4 border border-border bg-card px-3 py-3 md:px-4">
      <div className="relative h-[96px] w-[96px] overflow-hidden bg-muted">
        {looksLikeUrl(media) ? (
          <Image
            src={media as string}
            alt={title}
            fill
            unoptimized
            className="object-cover"
            sizes="96px"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
            No image
          </div>
        )}
      </div>

      <div className="min-w-0 space-y-1.5 text-sm">
        <p className="text-[11px] font-semibold uppercase tracking-[0.11em] text-muted-foreground">
          {label}
        </p>
        <p className="line-clamp-1 text-sm font-semibold">{title || "Untitled"}</p>
        <p className="line-clamp-1 text-muted-foreground">{author || "Unknown author"}</p>
        <StatPill>{formatCoins(valuation)} BookCoins</StatPill>
      </div>

      <div className="justify-self-end">
        {bookId ? (
          <Button asChild size="sm" variant="outline" className="h-9 px-4">
            <Link href={`/book/${bookId}`}>View Book</Link>
          </Button>
        ) : (
          <Button size="sm" variant="outline" className="h-9 px-4" disabled>
            View Book
          </Button>
        )}
      </div>
    </div>
  );
}

export function SwapCard({ item, mode, onDone }: SwapCardProps) {
  const [loading, setLoading] = useState(false);

  const requestedBook = mode === "sent" ? item.responderBook : item.requesterBook;
  const offeredBook = mode === "sent" ? item.requesterBook : item.responderBook;

  async function performAction() {
    const endpoint =
      mode === "sent"
        ? `/api/bff/swap/cancel/${item.swapId}`
        : `/api/bff/swap/accept/${item.swapId}`;

    const confirmText =
      mode === "sent"
        ? "Cancel this swap request?"
        : "Accept this swap request?";

    if (!window.confirm(confirmText)) return;

    setLoading(true);
    try {
      const res = await fetch(endpoint, {
        method: "POST",
        credentials: "include",
      });

      if (!res.ok) {
        throw new Error(`Request failed (${res.status})`);
      }

      onDone(item.swapId);
    } catch (error) {
      console.error(error);
      window.alert("Action failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="surface-card rounded-md border border-border p-4 md:p-5">
      <div className="space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="space-y-1">
            <p className="text-sm font-semibold tracking-tight">
              Swap #{item.swapId?.slice(0, 8) || "N/A"}
            </p>
            <p className="text-xs text-muted-foreground">
              {mode === "sent"
                ? "You requested this trade"
                : "This trade was sent to you"}
            </p>
          </div>
          <StatPill className="bg-muted text-foreground">
            {mode === "sent" ? "Sent Request" : "Received Request"}
          </StatPill>
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_auto_1fr] md:items-stretch">
          <BookMini
            bookId={offeredBook?.bookId}
            title={offeredBook?.title || "Your offered book"}
            author={offeredBook?.author || ""}
            valuation={offeredBook?.valuation}
            media={offeredBook?.thumbnailUrl || offeredBook?.primaryMediaId}
            label={mode === "sent" ? "You Offer" : "You Give"}
          />

          <div className="flex items-center justify-center text-muted-foreground">
            <ArrowRightLeft className="h-5 w-5" />
          </div>

          <BookMini
            bookId={requestedBook?.bookId}
            title={requestedBook?.title || "Requested book"}
            author={requestedBook?.author || ""}
            valuation={requestedBook?.valuation}
            media={requestedBook?.thumbnailUrl || requestedBook?.primaryMediaId}
            label={mode === "sent" ? "You Request" : "You Receive"}
          />
        </div>

        <div className="flex justify-end border-t border-border pt-3">
          <Button
            onClick={performAction}
            disabled={loading}
            variant={mode === "sent" ? "outline" : "default"}
          >
            {loading
              ? "Processing..."
              : mode === "sent"
                ? "Cancel Request"
                : "Accept Request"}
          </Button>
        </div>
      </div>
    </Card>
  );
}
