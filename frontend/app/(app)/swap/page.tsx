import { PageContainer } from "@/components/layout/PageContainer";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { Button } from "@/components/ui/button";
import { SwapTabs } from "@/features/swap/components/SwapTabs";
import {
  getMyReceivedSwaps,
  getMySentSwaps,
} from "@/features/swap/server/swap.api";
import Link from "next/link";

export default async function SwapPage() {
  const [sent, received] = await Promise.all([getMySentSwaps(), getMyReceivedSwaps()]);

  return (
    <PageContainer className="space-y-6 py-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <SectionHeader
          title="Swap Center"
          subtitle="Track your sent and received requests, then accept or cancel when needed."
        />
        <Button asChild>
          <Link href="/swap/create">Create Swap Request</Link>
        </Button>
      </div>

      <SwapTabs sent={sent} received={received} />
    </PageContainer>
  );
}
