import { PageContainer } from "@/components/layout/PageContainer";
import { Card } from "@/components/ui/card";

export default function SwapLoading() {
  return (
    <PageContainer className="space-y-6 py-6">
      <div className="space-y-1">
        <h1 className="section-title">Swap Center</h1>
        <p className="section-subtitle">Loading your swap requests...</p>
      </div>

      <Card className="surface-card rounded-md p-4">
        <div className="flex items-center gap-3">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-foreground" />
          <p className="text-sm text-muted-foreground">
            Fetching sent and received requests...
          </p>
        </div>
      </Card>
    </PageContainer>
  );
}
