import { PageContainer } from "@/components/layout/PageContainer";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { MyBooksSection } from "@/features/library/components/MyBooksSection";
import { getMyBooks } from "@/features/library/server/library.api";

export default async function LibraryPage() {
  const books = await getMyBooks();

  return (
    <PageContainer className="space-y-6 py-6">
      <SectionHeader
        title="My Library"
        subtitle="Manage your listed books and remove any listing you no longer want to offer."
      />

      <MyBooksSection initialBooks={books} />
    </PageContainer>
  );
}
