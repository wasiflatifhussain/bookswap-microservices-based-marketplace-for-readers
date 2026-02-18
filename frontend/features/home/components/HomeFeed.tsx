import { BookCard } from "@/features/catalog/components/BookCard";
import { FeedItem } from "../types";

interface Props {
  books: FeedItem[];
  currentUserId: string;
}

export function HomeFeed({ books, currentUserId }: Props) {
  if (books.length === 0) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-6">
        <p className="text-sm text-muted-foreground">
          No books available right now.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-6 space-y-4">
      {books.map((book) => (
        <BookCard key={book.bookId} item={book} currentUserId={currentUserId} />
      ))}
    </div>
  );
}
