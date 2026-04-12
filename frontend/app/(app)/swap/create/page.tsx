import { PageContainer } from "@/components/layout/PageContainer";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { getBookById } from "@/features/catalog/server/catalog.api";
import { getHomeFeed } from "@/features/home/server/home.api";
import { getMyBooks } from "@/features/library/server/library.api";
import { getNavbarSnapshot } from "@/features/navbar/server/navbar.api";
import { SwapCreateFlow } from "@/features/swap/components/SwapCreateFlow";

interface CreateSwapPageProps {
  searchParams: Promise<{ targetBookId?: string }>;
}

export default async function CreateSwapPage({ searchParams }: CreateSwapPageProps) {
  const { targetBookId } = await searchParams;
  const [myBooks, recentBooks, snapshot, targetBook] = await Promise.all([
    getMyBooks(),
    getHomeFeed(10),
    getNavbarSnapshot(),
    targetBookId ? getBookById(targetBookId) : Promise.resolve(undefined),
  ]);

  const validMyBooks = myBooks.filter((book) => Boolean(book?.bookId));
  const validRecentBooks = recentBooks.filter((book) => Boolean(book?.bookId));

  return (
    <PageContainer className="space-y-6 py-6">
      <SectionHeader
        title="Create Swap Request"
        subtitle="Pick a target book from recent listings, then choose one of your available books to offer."
      />
      <SwapCreateFlow
        targetBook={targetBook}
        recentBooks={validRecentBooks}
        currentUserId={snapshot.userId}
        myBooks={validMyBooks}
      />
    </PageContainer>
  );
}
