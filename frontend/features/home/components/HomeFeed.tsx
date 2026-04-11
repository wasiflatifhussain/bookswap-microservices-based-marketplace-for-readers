import { PageContainer } from "@/components/layout/PageContainer";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { EmptyState } from "@/components/states/EmptyState";
import { BookCard } from "@/features/catalog/components/BookCard";
import { FeedItem } from "../types";

interface Props {
  books: FeedItem[];
  currentUserId: string;
}

export function HomeFeed({ books, currentUserId }: Props) {
  if (books.length === 0) {
    return (
      <PageContainer className="py-10">
        <EmptyState message="No books available right now." />
      </PageContainer>
    );
  }

  return (
    <PageContainer className="space-y-6 py-6">
      <SectionHeader
        title="Discover Books"
        subtitle="Curated listings from the BookSwap community."
      />

      {books.map((book) => (
        <BookCard key={book.bookId} item={book} currentUserId={currentUserId} />
      ))}
    </PageContainer>
  );
}
