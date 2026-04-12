import { PageContainer } from "@/components/layout/PageContainer";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { Button } from "@/components/ui/button";
import { MyBooksSection } from "@/features/library/components/MyBooksSection";
import { getMyBooks } from "@/features/library/server/library.api";
import Link from "next/link";

export default async function LibraryPage() {
  const books = await getMyBooks();

  return (
    <PageContainer className="space-y-6 py-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <SectionHeader
          title="My Library"
          subtitle="Manage your listed books and remove any listing you no longer want to offer."
        />
        <Button asChild>
          <Link href="/library/create">Create Book</Link>
        </Button>
      </div>

      <MyBooksSection initialBooks={books} />
    </PageContainer>
  );
}
