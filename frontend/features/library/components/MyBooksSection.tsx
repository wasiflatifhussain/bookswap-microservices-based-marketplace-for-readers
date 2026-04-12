"use client";

import { EmptyState } from "@/components/states/EmptyState";
import { Button } from "@/components/ui/button";
import { BookCard } from "@/features/catalog/components/BookCard";
import { LibraryBook } from "@/features/library/types";
import { useEffect, useMemo, useState } from "react";

interface MyBooksSectionProps {
  initialBooks: LibraryBook[];
}

function normalizeBooks(input: LibraryBook[]): LibraryBook[] {
  return input
    .filter((book) => Boolean(book?.bookId))
    .map((book) => {
      const valuation =
        typeof book.valuation === "string"
          ? Number(book.valuation)
          : book.valuation;

      return {
        ...book,
        valuation: Number.isFinite(valuation as number)
          ? (valuation as number)
          : 0.01,
        thumbnailUrl: book.thumbnailUrl || null,
      };
    });
}

export function MyBooksSection({ initialBooks }: MyBooksSectionProps) {
  const [books, setBooks] = useState(normalizeBooks(initialBooks));
  const [deletingBookId, setDeletingBookId] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false);

  useEffect(() => {
    setBooks(normalizeBooks(initialBooks));
  }, [initialBooks]);

  const hasPendingBooks = useMemo(
    () =>
      books.some(
        (book) =>
          (typeof book.valuation !== "number" || book.valuation <= 0.01) ||
          !book.thumbnailUrl,
      ),
    [books],
  );

  const availableBooks = useMemo(
    () => books.filter((book) => (book.bookStatus || "").toUpperCase() === "AVAILABLE"),
    [books],
  );

  const unavailableBooks = useMemo(
    () => books.filter((book) => (book.bookStatus || "").toUpperCase() !== "AVAILABLE"),
    [books],
  );

  async function refreshBooks(showSpinner = false) {
    if (showSpinner) setSyncing(true);
    try {
      const res = await fetch("/api/bff/books/me/get", {
        method: "GET",
        credentials: "include",
        cache: "no-store",
        headers: {
          Pragma: "no-cache",
          "Cache-Control": "no-cache",
        },
      });

      if (!res.ok) return;
      const latest = (await res.json()) as LibraryBook[];
      setBooks(normalizeBooks(latest));
    } catch (error) {
      console.error(error);
    } finally {
      if (showSpinner) setSyncing(false);
    }
  }

  useEffect(() => {
    if (!hasPendingBooks) return;

    const startedAt = Date.now();
    const maxDurationMs = 300_000;
    const intervalMs = 2500;

    const timer = window.setInterval(async () => {
      if (Date.now() - startedAt > maxDurationMs) {
        window.clearInterval(timer);
        return;
      }
      await refreshBooks(false);
    }, intervalMs);

    return () => window.clearInterval(timer);
  }, [hasPendingBooks]);

  async function handleDelete(bookId: string) {
    const confirmed = window.confirm(
      "Delete this listing from your library? This cannot be undone.",
    );
    if (!confirmed) return;

    setDeletingBookId(bookId);
    try {
      const res = await fetch(`/api/bff/books/me/delete/${bookId}`, {
        method: "DELETE",
        credentials: "include",
      });

      if (!res.ok) {
        throw new Error(`Failed to delete (${res.status})`);
      }

      setBooks((prev) => prev.filter((book) => book.bookId !== bookId));
    } catch (error) {
      console.error(error);
      window.alert("Could not delete this listing right now.");
    } finally {
      setDeletingBookId(null);
    }
  }

  if (books.length === 0) {
    return (
      <EmptyState
        title="Your library is empty"
        message="Add your first listing to start receiving swap requests."
      />
    );
  }

  return (
    <div className="space-y-6">
      {hasPendingBooks ? (
        <div className="flex flex-wrap items-center justify-between gap-2 border border-border bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
          <p>
            Some listings are still processing image sync or AI valuation. Auto-refresh is on.
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refreshBooks(true)}
            disabled={syncing}
          >
            {syncing ? "Refreshing..." : "Refresh now"}
          </Button>
        </div>
      ) : null}

      <section className="space-y-4 border border-border bg-card/30 p-4 md:p-5">
        <div className="flex items-center justify-between border-b border-border pb-3">
          <h3 className="text-sm font-semibold uppercase tracking-[0.12em] text-muted-foreground">
            Available Books
          </h3>
          <span className="text-xs text-muted-foreground">{availableBooks.length}</span>
        </div>

        {availableBooks.length === 0 ? (
          <div className="border border-dashed border-border bg-muted/30 px-4 py-5 text-sm text-muted-foreground">
            No available books right now.
          </div>
        ) : (
          availableBooks.map((book) => (
            <BookCard
              key={book.bookId}
              item={book}
              mode="library"
              onDelete={handleDelete}
              deleting={deletingBookId === book.bookId}
            />
          ))
        )}
      </section>

      <section className="space-y-4 border border-border bg-card/30 p-4 md:p-5">
        <div className="flex items-center justify-between border-b border-border pb-3">
          <h3 className="text-sm font-semibold uppercase tracking-[0.12em] text-muted-foreground">
            Reserved / Unavailable
          </h3>
          <span className="text-xs text-muted-foreground">{unavailableBooks.length}</span>
        </div>

        {unavailableBooks.length === 0 ? (
          <div className="border border-dashed border-border bg-muted/30 px-4 py-5 text-sm text-muted-foreground">
            No reserved or unavailable books.
          </div>
        ) : (
          unavailableBooks.map((book) => (
            <BookCard
              key={book.bookId}
              item={book}
              mode="library"
              onDelete={handleDelete}
              deleting={deletingBookId === book.bookId}
            />
          ))
        )}
      </section>
    </div>
  );
}
