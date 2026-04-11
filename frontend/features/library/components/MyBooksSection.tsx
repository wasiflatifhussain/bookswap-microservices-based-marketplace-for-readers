"use client";

import { EmptyState } from "@/components/states/EmptyState";
import { BookCard } from "@/features/catalog/components/BookCard";
import { LibraryBook } from "@/features/library/types";
import { useState } from "react";

interface MyBooksSectionProps {
  initialBooks: LibraryBook[];
}

export function MyBooksSection({ initialBooks }: MyBooksSectionProps) {
  const [books, setBooks] = useState(initialBooks);
  const [deletingBookId, setDeletingBookId] = useState<string | null>(null);

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
    <div className="space-y-4">
      {books.map((book) => (
        <BookCard
          key={book.bookId}
          item={book}
          mode="library"
          onDelete={handleDelete}
          deleting={deletingBookId === book.bookId}
        />
      ))}
    </div>
  );
}
