import { EmptyState } from "@/components/states/EmptyState";
import { Card } from "@/components/ui/card";
import { StatPill } from "@/components/ui/stat-pill";
import { BookMatchCard } from "@/features/catalog/types";
import Image from "next/image";
import Link from "next/link";

interface BookMatchesProps {
  items: BookMatchCard[];
}

function formatCoins(value: number | null | undefined): string {
  return typeof value === "number" ? value.toFixed(2) : "0.00";
}

export function BookMatches({ items }: BookMatchesProps) {
  if (items.length === 0) {
    return (
      <EmptyState
        title="No close matches yet"
        message="Try again later or broaden match tolerance in future iterations."
      />
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
      {items.map((item) => (
        <Card key={item.bookId} className="surface-card rounded-md p-4">
          <div className="grid grid-cols-[88px_1fr] gap-3">
            <div className="relative h-[88px] w-[88px] overflow-hidden border border-border bg-muted">
              {item.thumbnailUrl ? (
                <Image
                  src={item.thumbnailUrl}
                  alt={item.title}
                  fill
                  className="object-cover"
                  sizes="88px"
                />
              ) : (
                <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
                  No image
                </div>
              )}
            </div>

            <div className="space-y-2 text-sm">
              <div className="line-clamp-1 font-medium">{item.title}</div>
              <div className="line-clamp-1 text-muted-foreground">{item.author}</div>
              <div>
                <StatPill>{formatCoins(item.valuation)} BookCoins</StatPill>
              </div>
              <Link
                href={`/book/${item.bookId}`}
                className="inline-block text-sm font-medium text-primary underline-offset-4 hover:underline"
              >
                View Book
              </Link>
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}
