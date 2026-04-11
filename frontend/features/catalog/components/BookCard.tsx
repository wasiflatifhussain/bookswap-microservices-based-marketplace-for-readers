import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { StatPill } from "@/components/ui/stat-pill";
import { FeedItem } from "@/features/home/types";
import Image from "next/image";
import Link from "next/link";

interface Props {
  item: FeedItem;
  currentUserId: string;
}

export function BookCard({ item, currentUserId }: Props) {
  const isOwnedByUser = item.ownerUserId === currentUserId;

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
              className="object-cover"
              sizes="(max-width: 768px) 100vw, 30vw"
              loading="lazy"
              quality={75}
            />
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
            <div>
              <span className="font-medium text-primary/90">Value:</span>{" "}
              <StatPill>
                {item.valuation.toFixed(2)} BookCoins
              </StatPill>
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

            <Button
              disabled={isOwnedByUser}
              title={
                isOwnedByUser
                  ? "You cannot swap your own book"
                  : "Send swap request"
              }
              className="w-full md:w-auto"
            >
              Send Swap Request
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );
}
