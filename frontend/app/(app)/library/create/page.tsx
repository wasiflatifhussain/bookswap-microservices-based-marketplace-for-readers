import { PageContainer } from "@/components/layout/PageContainer";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { CreateBookForm } from "@/features/library/components/CreateBookForm";

export default function CreateBookPage() {
  return (
    <PageContainer className="space-y-6 py-6">
      <SectionHeader
        title="Create Book Listing"
        subtitle="Add details, upload images, and publish your listing to start receiving swap requests."
      />
      <CreateBookForm />
    </PageContainer>
  );
}
