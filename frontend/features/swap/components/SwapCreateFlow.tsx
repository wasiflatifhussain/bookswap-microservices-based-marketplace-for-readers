"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  type CarouselApi,
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { StatPill } from "@/components/ui/stat-pill";
import { BookDetail } from "@/features/catalog/types";
import { FeedItem } from "@/features/home/types";
import { LibraryBook } from "@/features/library/types";
import Image from "next/image";
import { useEffect, useMemo, useState } from "react";

interface SwapCreateFlowProps {
  targetBook?: BookDetail;
  recentBooks: FeedItem[];
  currentUserId: string;
  myBooks: LibraryBook[];
}

interface TargetBookOption {
  bookId: string;
  title: string;
  description: string;
  genre: string;
  author: string;
  bookCondition: string;
  valuation: number;
  bookStatus: string;
  thumbnailUrl: string | null;
  ownerUserId: string;
}

function formatCoins(value: number | null | undefined): string {
  return typeof value === "number" ? value.toFixed(2) : "0.00";
}

function formatEnumLabel(value: string | null | undefined): string {
  if (!value) return "Unknown";
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function SelectionCard({
  title,
  author,
  valuation,
  imageUrl,
  selected,
  onSelect,
  buttonText,
  size = "compact",
}: {
  title: string;
  author: string;
  valuation: number | null | undefined;
  imageUrl?: string | null;
  selected?: boolean;
  onSelect: () => void;
  buttonText: string;
  size?: "compact" | "modal";
}) {
  const isModal = size === "modal";

  return (
    <Card className={isModal ? "surface-card rounded-md p-4" : "surface-card rounded-md p-3"}>
      <div className={isModal ? "grid grid-cols-[124px_1fr] gap-4" : "grid grid-cols-[92px_1fr] gap-3"}>
        <div
          className={
            isModal
              ? "relative h-[124px] w-[124px] overflow-hidden border border-border bg-muted"
              : "relative h-[92px] w-[92px] overflow-hidden border border-border bg-muted"
          }
        >
          {imageUrl ? (
            <Image
              src={imageUrl}
              alt={title}
              fill
              unoptimized
              className="object-cover"
              sizes={isModal ? "124px" : "92px"}
            />
          ) : (
            <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
              No image
            </div>
          )}
        </div>

        <div className={isModal ? "space-y-3" : "space-y-2"}>
          <p className={isModal ? "line-clamp-2 text-base font-semibold tracking-tight" : "line-clamp-1 text-sm font-medium"}>{title}</p>
          <p className={isModal ? "line-clamp-1 text-sm text-muted-foreground" : "line-clamp-1 text-xs text-muted-foreground"}>{author}</p>
          <StatPill>{formatCoins(valuation)} BookCoins</StatPill>
          <div>
            <Button
              type="button"
              size="sm"
              variant={selected ? "secondary" : "outline"}
              onClick={onSelect}
              className={isModal ? "h-10 px-4" : ""}
            >
              {selected ? "Selected" : buttonText}
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );
}

interface ModalPickerBook {
  bookId: string;
  title: string;
  description: string;
  genre: string;
  author: string;
  bookCondition: string;
  valuation: number;
  thumbnailUrl: string | null;
  bookStatus?: string;
}

function ModalCarouselPicker({
  books,
  selectedBookId,
  onSelect,
  actionLabel,
}: {
  books: ModalPickerBook[];
  selectedBookId: string | null;
  onSelect: (bookId: string) => void;
  actionLabel: string;
}) {
  const [api, setApi] = useState<CarouselApi>();
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (!api) return;

    const onChange = () => {
      setCurrentIndex(api.selectedScrollSnap());
    };

    onChange();
    api.on("select", onChange);
    api.on("reInit", onChange);

    return () => {
      api.off("select", onChange);
      api.off("reInit", onChange);
    };
  }, [api]);

  return (
    <div className="space-y-4">
      <Carousel setApi={setApi} className="w-full" opts={{ align: "center" }}>
        <CarouselContent>
          {books.map((book) => (
            <CarouselItem key={book.bookId} className="basis-full">
              <Card className="surface-card rounded-md p-5 md:p-6">
                <div className="grid grid-cols-1 gap-6 md:grid-cols-[45%_55%]">
                  <div className="relative aspect-[4/3] w-full overflow-hidden rounded-md bg-muted md:aspect-[1]">
                    {book.thumbnailUrl ? (
                      <Image
                        src={book.thumbnailUrl}
                        alt={book.title}
                        fill
                        unoptimized
                        className="object-cover"
                        sizes="(max-width: 768px) 100vw, 45vw"
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                        No image
                      </div>
                    )}
                  </div>

                  <div className="space-y-4 pr-1 md:pr-3">
                    <p className="line-clamp-2 text-xl font-semibold tracking-tight">
                      {book.title}
                    </p>
                    <dl className="grid grid-cols-[110px_1fr] gap-y-2 text-sm">
                      <dt className="font-medium text-primary/90">Author</dt>
                      <dd>{book.author || "-"}</dd>
                      <dt className="font-medium text-primary/90">Genre</dt>
                      <dd>{formatEnumLabel(book.genre)}</dd>
                      <dt className="font-medium text-primary/90">Condition</dt>
                      <dd>{formatEnumLabel(book.bookCondition)}</dd>
                      <dt className="font-medium text-primary/90">Status</dt>
                      <dd>{formatEnumLabel(book.bookStatus)}</dd>
                      <dt className="font-medium text-primary/90">Value</dt>
                      <dd>
                        <StatPill>{formatCoins(book.valuation)} BookCoins</StatPill>
                      </dd>
                    </dl>
                    <div className="border-t border-border pt-4">
                      <p className="mb-1 text-sm font-medium text-primary/90">
                        Description
                      </p>
                      <p className="line-clamp-4 text-sm leading-relaxed text-muted-foreground">
                        {book.description || "No description provided."}
                      </p>
                    </div>
                    <Button
                      type="button"
                      variant={
                        selectedBookId === book.bookId ? "secondary" : "outline"
                      }
                      className="h-10 px-4"
                      onClick={() => onSelect(book.bookId)}
                    >
                      {selectedBookId === book.bookId ? "Selected" : actionLabel}
                    </Button>
                  </div>
                </div>
              </Card>
            </CarouselItem>
          ))}
        </CarouselContent>

        {books.length > 1 ? (
          <>
            <CarouselPrevious className="-left-2 top-1/2 h-10 w-10 -translate-y-1/2 border-border bg-card" />
            <CarouselNext className="-right-2 top-1/2 h-10 w-10 -translate-y-1/2 border-border bg-card" />
          </>
        ) : null}
      </Carousel>

      {books.length > 1 ? (
        <div className="flex items-center justify-center gap-2">
          {books.map((book, index) => (
            <button
              key={`dot-${book.bookId}`}
              type="button"
              onClick={() => api?.scrollTo(index)}
              aria-label={`Go to slide ${index + 1}`}
              className={
                index === currentIndex
                  ? "h-2.5 w-2.5 rounded-full bg-foreground"
                  : "h-2.5 w-2.5 rounded-full bg-muted-foreground/30"
              }
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

export function SwapCreateFlow({
  targetBook,
  recentBooks,
  currentUserId,
  myBooks,
}: SwapCreateFlowProps) {
  const initialTarget = targetBook
    ? {
        bookId: targetBook.bookId,
        title: targetBook.title,
        author: targetBook.author,
        valuation: targetBook.valuation,
        bookStatus: targetBook.bookStatus,
        thumbnailUrl: targetBook.mediaUrls?.[0] ?? null,
        ownerUserId: targetBook.ownerUserId,
      }
    : null;

  const [selectedTarget, setSelectedTarget] = useState<TargetBookOption | null>(initialTarget);
  const [selectedBookId, setSelectedBookId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [targetModalOpen, setTargetModalOpen] = useState(false);
  const [offeredModalOpen, setOfferedModalOpen] = useState(false);

  const targetOptions = useMemo(
    () =>
      recentBooks
        .filter(
          (book) => book.ownerUserId !== currentUserId && book.bookStatus === "AVAILABLE",
        )
        .map((book) => ({
          bookId: book.bookId,
          title: book.title,
          description: book.description,
          genre: book.genre,
          author: book.author,
          bookCondition: book.bookCondition,
          valuation: book.valuation,
          bookStatus: book.bookStatus,
          thumbnailUrl: book.thumbnailUrl,
          ownerUserId: book.ownerUserId,
        })),
    [recentBooks, currentUserId],
  );

  const availableBooks = useMemo(
    () =>
      myBooks.filter(
        (book) =>
          Boolean(book.bookId) &&
          book.bookStatus === "AVAILABLE" &&
          book.bookId !== selectedTarget?.bookId,
      ),
    [myBooks, selectedTarget?.bookId],
  );

  const selectedOfferedBook = useMemo(
    () => availableBooks.find((book) => book.bookId === selectedBookId) ?? null,
    [availableBooks, selectedBookId],
  );

  async function handleCreateSwap() {
    if (!selectedBookId || !selectedTarget?.bookId) return;

    setError(null);
    setSubmitting(true);
    try {
      const res = await fetch("/api/bff/swap/create", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          requesterBookId: selectedBookId,
          responderBookId: selectedTarget.bookId,
        }),
      });

      if (!res.ok) {
        throw new Error(`Failed to create swap (${res.status})`);
      }

      const payload = await res.json();
      if (payload?.message && payload.message.toLowerCase().includes("failed")) {
        throw new Error(payload.message);
      }

      window.location.assign("/swap");
    } catch (e: unknown) {
      if (e instanceof Error) setError(e.message);
      else setError("Failed to create swap request.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      {error ? (
        <p className="rounded-sm border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </p>
      ) : null}

      <Card className="surface-card rounded-md p-4 md:p-5">
        <p className="mb-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Requested Book (Target)
        </p>

        {selectedTarget ? (
          <div className="grid grid-cols-[112px_1fr] gap-4">
            <div className="relative h-28 w-28 overflow-hidden border border-border bg-muted">
              {selectedTarget.thumbnailUrl ? (
                <Image
                  src={selectedTarget.thumbnailUrl}
                  alt={selectedTarget.title}
                  fill
                  unoptimized
                  className="object-cover"
                  sizes="112px"
                />
              ) : (
                <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
                  No image
                </div>
              )}
            </div>

            <div className="space-y-2">
              <p className="text-base font-semibold tracking-tight">{selectedTarget.title}</p>
              <p className="text-sm text-muted-foreground">{selectedTarget.author}</p>
              <div className="flex flex-wrap items-center gap-2">
                <StatPill>{formatCoins(selectedTarget.valuation)} BookCoins</StatPill>
                <StatPill className="bg-muted">
                  {formatEnumLabel(selectedTarget.bookStatus)}
                </StatPill>
              </div>
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">No target book selected yet.</p>
        )}

        <div className="mt-4">
          <Dialog open={targetModalOpen} onOpenChange={setTargetModalOpen}>
            <DialogTrigger asChild>
              <Button type="button" variant="outline">
                Choose from recent books
              </Button>
            </DialogTrigger>
            <DialogContent className="h-[92vh] w-[99.5vw] max-w-[2200px] p-0">
              <div className="border-b border-border px-6 py-5 sm:px-7">
                <DialogTitle className="text-xl tracking-tight">Select Target Book</DialogTitle>
                <p className="mt-1 text-sm text-muted-foreground">
                  Pick a recent listing you want to request.
                </p>
              </div>
              <div className="max-h-[80vh] space-y-4 overflow-y-auto px-6 py-5 pr-4 sm:px-7">
                {targetOptions.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    No recent books available to request right now.
                  </p>
                ) : (
                  <ModalCarouselPicker
                    books={targetOptions}
                    selectedBookId={selectedTarget?.bookId ?? null}
                    actionLabel="Select target book"
                    onSelect={(bookId) => {
                      const next = targetOptions.find((item) => item.bookId === bookId);
                      if (!next) return;
                      setSelectedTarget(next);
                      setSelectedBookId(null);
                      setTargetModalOpen(false);
                    }}
                  />
                )}
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </Card>

      <Card className="surface-card rounded-md p-4 md:p-5">
        <p className="mb-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Your Offered Book
        </p>

        {selectedOfferedBook ? (
          <SelectionCard
            title={selectedOfferedBook.title}
            author={selectedOfferedBook.author}
            valuation={selectedOfferedBook.valuation}
            imageUrl={selectedOfferedBook.thumbnailUrl}
            selected
            buttonText="Select this book"
            onSelect={() => setOfferedModalOpen(true)}
          />
        ) : (
          <p className="text-sm text-muted-foreground">No offered book selected yet.</p>
        )}

        <div className="mt-4">
          <Dialog open={offeredModalOpen} onOpenChange={setOfferedModalOpen}>
            <DialogTrigger asChild>
              <Button
                type="button"
                variant="outline"
                disabled={availableBooks.length === 0}
              >
                Choose from all my books
              </Button>
            </DialogTrigger>
            <DialogContent className="h-[92vh] w-[99.5vw] max-w-[2200px] p-0">
              <div className="border-b border-border px-6 py-5 sm:px-7">
                <DialogTitle className="text-xl tracking-tight">Select Offered Book</DialogTitle>
                <p className="mt-1 text-sm text-muted-foreground">
                  Choose which of your available books you want to offer.
                </p>
              </div>
              <div className="max-h-[80vh] space-y-4 overflow-y-auto px-6 py-5 pr-4 sm:px-7">
                {availableBooks.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    You do not have any available books to offer right now.
                  </p>
                ) : (
                  <ModalCarouselPicker
                    books={availableBooks}
                    selectedBookId={selectedBookId}
                    actionLabel="Select this book"
                    onSelect={(bookId) => {
                      setSelectedBookId(bookId);
                      setOfferedModalOpen(false);
                    }}
                  />
                )}
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </Card>

      <div className="flex justify-end">
        <Button
          type="button"
          onClick={handleCreateSwap}
          disabled={
            !selectedBookId || !selectedTarget?.bookId || submitting || availableBooks.length === 0
          }
        >
          {submitting ? "Creating Request..." : "Confirm Swap Request"}
        </Button>
      </div>
    </div>
  );
}
