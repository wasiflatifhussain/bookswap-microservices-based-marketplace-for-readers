"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import { StatPill } from "@/components/ui/stat-pill";
import { LibraryBook } from "@/features/library/types";
import { cn } from "@/lib/utils";
import Image from "next/image";

interface SwapBookSelectorProps {
  books: LibraryBook[];
  selectedBookId: string | null;
  onSelect: (bookId: string) => void;
}

function formatCoins(value: number | null | undefined): string {
  return typeof value === "number" ? value.toFixed(2) : "0.00";
}

export function SwapBookSelector({
  books,
  selectedBookId,
  onSelect,
}: SwapBookSelectorProps) {
  return (
    <Carousel className="w-full" opts={{ align: "start" }}>
      <CarouselContent>
        {books.map((book) => (
          <CarouselItem
            key={book.bookId}
            className="basis-[88%] sm:basis-[60%] lg:basis-[50%]"
          >
            <Card
              className={cn(
                "surface-card rounded-md p-3 transition-colors",
                selectedBookId === book.bookId
                  ? "border-primary bg-primary/5"
                  : "border-border",
              )}
            >
              <div className="grid grid-cols-[96px_1fr] gap-3">
                <div className="relative h-24 w-24 overflow-hidden border border-border bg-muted">
                  {book.thumbnailUrl ? (
                    <Image
                      src={book.thumbnailUrl}
                      alt={book.title}
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

                <div className="space-y-2">
                  <p className="line-clamp-1 text-sm font-medium">{book.title}</p>
                  <p className="line-clamp-1 text-xs text-muted-foreground">{book.author}</p>
                  <StatPill>{formatCoins(book.valuation)} BookCoins</StatPill>
                  <div>
                    <Button
                      type="button"
                      size="sm"
                      variant={selectedBookId === book.bookId ? "secondary" : "outline"}
                      onClick={() => onSelect(book.bookId)}
                    >
                      {selectedBookId === book.bookId ? "Selected" : "Select this book"}
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          </CarouselItem>
        ))}
      </CarouselContent>

      {books.length > 1 ? (
        <>
          <CarouselPrevious className="-left-2" />
          <CarouselNext className="-right-2" />
        </>
      ) : null}
    </Carousel>
  );
}
