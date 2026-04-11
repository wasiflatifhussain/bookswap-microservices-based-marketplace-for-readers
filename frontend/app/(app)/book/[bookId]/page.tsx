import { PageContainer } from "@/components/layout/PageContainer";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { Card } from "@/components/ui/card";
import { StatPill } from "@/components/ui/stat-pill";
import { getNavbarSnapshot } from "@/features/navbar/server/navbar.api";
import { notFound } from "next/navigation";
import { BookDetailActions } from "@/features/catalog/components/BookDetailActions";
import { BookImageCarousel } from "@/features/catalog/components/BookImageCarousel";
import { BookMatches } from "@/features/catalog/components/BookMatches";
import {
  getBookById,
  getBookMatches,
} from "@/features/catalog/server/catalog.api";

interface BookDetailPageProps {
  params: Promise<{ bookId: string }>;
}

function formatCoins(value: number | null | undefined): string {
  return typeof value === "number" ? value.toFixed(2) : "0.00";
}

function prettyDate(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "-";
  return parsed.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export default async function BookDetailPage({ params }: BookDetailPageProps) {
  const { bookId } = await params;

  const [book, matches, snapshot] = await Promise.all([
    getBookById(bookId),
    getBookMatches(bookId),
    getNavbarSnapshot(),
  ]);

  if (!book?.bookId) {
    notFound();
  }

  const filteredMatches = matches.filter((match) => match.bookId !== book.bookId);

  return (
    <PageContainer className="space-y-8 py-6">
      <SectionHeader
        title={book.title}
        subtitle={`Listed by ${book.ownerUserId === snapshot.userId ? "you" : "another reader"}`}
      />

      <Card className="surface-card rounded-md p-5 md:p-6">
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-[45%_55%]">
          <BookImageCarousel title={book.title} mediaUrls={book.mediaUrls ?? []} />

          <div className="space-y-4 pr-1 md:pr-3">
            <div className="flex flex-wrap items-center gap-2">
              <StatPill>{formatCoins(book.valuation)} BookCoins</StatPill>
              <StatPill className="bg-muted">{book.bookStatus || "UNKNOWN"}</StatPill>
            </div>

            <dl className="grid grid-cols-[110px_1fr] gap-y-2 text-sm">
              <dt className="font-medium text-primary/90">Author</dt>
              <dd>{book.author || "-"}</dd>
              <dt className="font-medium text-primary/90">Genre</dt>
              <dd>{book.genre || "-"}</dd>
              <dt className="font-medium text-primary/90">Condition</dt>
              <dd>{book.bookCondition || "-"}</dd>
              <dt className="font-medium text-primary/90">Book ID</dt>
              <dd className="truncate">{book.bookId}</dd>
              <dt className="font-medium text-primary/90">Created</dt>
              <dd>{prettyDate(book.createdAt)}</dd>
            </dl>

            <div className="border-t border-border pt-4">
              <p className="mb-2 text-sm font-medium text-primary/90">Description</p>
              <p className="pr-1 text-sm leading-relaxed text-muted-foreground">
                {book.description || "No description available."}
              </p>
            </div>

            <div className="pt-2">
              <BookDetailActions
                bookId={book.bookId}
                ownerUserId={book.ownerUserId}
                currentUserId={snapshot.userId}
              />
            </div>
          </div>
        </div>
      </Card>

      <section className="space-y-4">
        <SectionHeader
          title="Related Matches"
          subtitle="Books with similar valuation that might be good swap targets."
        />
        <BookMatches items={filteredMatches} />
      </section>
    </PageContainer>
  );
}
