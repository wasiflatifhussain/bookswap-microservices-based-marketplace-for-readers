import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { StatPill } from "@/components/ui/stat-pill";
import { FeedItem } from "@/features/home/types";
import { LibraryBook } from "@/features/library/types";
import Image from "next/image";
import Link from "next/link";

type CardItem = FeedItem | LibraryBook;

interface Props {
  item: CardItem;
  currentUserId?: string;
  mode?: "feed" | "library";
  onDelete?: (bookId: string) => void;
  deleting?: boolean;
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

function getStatusPillClass(bookStatus: string | null | undefined): string {
  const status = (bookStatus || "").toUpperCase();
  if (status === "AVAILABLE") {
    return "bg-emerald-100 text-emerald-800";
  }
  if (status === "RESERVED") {
    return "bg-yellow-100 text-yellow-800";
  }
  if (status === "SWAPPED") {
    return "bg-red-100 text-red-800";
  }
  return "bg-muted text-foreground";
}

export function BookCard({
  item,
  currentUserId,
  mode = "feed",
  onDelete,
  deleting = false,
}: Props) {
  const isFeedItem = "ownerUserId" in item;
  const isOwnedByUser =
    isFeedItem && currentUserId ? item.ownerUserId === currentUserId : false;
  const isValuationPending =
    mode === "library" &&
    (typeof item.valuation !== "number" || item.valuation <= 0.01);
  const isImagePending = mode === "library" && !item.thumbnailUrl;

  return (
    <Card className="surface-card rounded-md p-4 md:p-6">
      {/* OUTER GRID */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-[30%_70%] md:gap-6">
        {/* IMAGE */}
        <div className="relative aspect-[4/3] w-full overflow-hidden rounded-md bg-muted md:aspect-[1]">
          {item.thumbnailUrl ? (
            <Image
              src={item.thumbnailUrl}
              alt={item.title}
              fill
              unoptimized
              className="object-cover"
              sizes="(max-width: 768px) 100vw, 30vw"
              loading="lazy"
              quality={75}
            />
          ) : isImagePending ? (
            <div className="flex h-full w-full flex-col items-center justify-center gap-1 text-sm text-muted-foreground">
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-foreground" />
              <span>Loading image...</span>
            </div>
          ) : (
            <div className="flex h-full w-full items-center justify-center text-sm text-muted-foreground">
              No Image
            </div>
          )}
        </div>

        {/* CONTENT */}
        <div className="grid grid-rows-[1fr_auto] p-2 md:p-4">
          {/* TEXT */}
          <div className="space-y-2 text-sm">
            <div>
              <span className="font-medium text-primary/90">Title:</span>{" "}
              {item.title}
            </div>
            <div>
              <span className="font-medium text-primary/90">Author:</span>{" "}
              {item.author}
            </div>
            <div>
              <span className="font-medium text-primary/90">Genre:</span>{" "}
              {item.genre}
            </div>
            <div>
              <span className="font-medium text-primary/90">Condition:</span>{" "}
              {item.bookCondition}
            </div>
            {mode === "library" && "bookStatus" in item ? (
              <div>
                <span className="font-medium text-primary/90">Status:</span>{" "}
                <StatPill className={getStatusPillClass(item.bookStatus)}>
                  {formatEnumLabel(item.bookStatus)}
                </StatPill>
              </div>
            ) : null}
            <div>
              <span className="font-medium text-primary/90">Value:</span>{" "}
              {isValuationPending ? (
                <StatPill className="bg-muted text-muted-foreground">
                  Loading...
                </StatPill>
              ) : (
                <StatPill>{formatCoins(item.valuation)} BookCoins</StatPill>
              )}
              {isValuationPending ? (
                <span className="ml-2 text-xs text-muted-foreground">
                  (AI valuation pending)
                </span>
              ) : null}
            </div>
            <div className="text-muted-foreground line-clamp-3">
              <span className="font-medium text-foreground">Description:</span>{" "}
              {item.description}
            </div>
          </div>

          {/* BUTTONS */}
          <div className="mt-4 flex flex-col gap-2 md:flex-row md:justify-end">
            <Button asChild variant="secondary" className="w-full md:w-auto">
              <Link href={`/book/${item.bookId}`}>View Book</Link>
            </Button>

            {mode === "library" ? (
              <Button
                variant="destructive"
                onClick={() => onDelete?.(item.bookId)}
                disabled={deleting}
                className="w-full md:w-auto"
              >
                {deleting ? "Deleting..." : "Delete Listing"}
              </Button>
            ) : (
              isOwnedByUser ? (
                <Button
                  disabled
                  title="You cannot swap your own book"
                  className="w-full md:w-auto"
                >
                  Send Swap Request
                </Button>
              ) : (
                <Button asChild className="w-full md:w-auto" title="Send swap request">
                  <Link href={`/swap/create?targetBookId=${item.bookId}`}>
                    Send Swap Request
                  </Link>
                </Button>
              )
            )}
          </div>
        </div>
      </div>
    </Card>
  );
}
