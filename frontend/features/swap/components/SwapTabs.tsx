"use client";

import { EmptyState } from "@/components/states/EmptyState";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { SwapItem } from "@/features/swap/types";
import { useMemo, useState } from "react";
import { SwapCard } from "./SwapCard";

interface SwapTabsProps {
  sent: SwapItem[];
  received: SwapItem[];
}

export function SwapTabs({ sent, received }: SwapTabsProps) {
  const [sentSwaps, setSentSwaps] = useState(sent);
  const [receivedSwaps, setReceivedSwaps] = useState(received);

  const defaultTab = useMemo(
    () => (receivedSwaps.length > 0 ? "received" : "sent"),
    [receivedSwaps.length],
  );

  return (
    <Tabs defaultValue={defaultTab}>
      <TabsList variant="line" className="w-full justify-start border-b border-border p-0">
        <TabsTrigger value="received" className="flex-none px-4 py-2 text-sm">
          Received ({receivedSwaps.length})
        </TabsTrigger>
        <TabsTrigger value="sent" className="flex-none px-4 py-2 text-sm">
          Sent ({sentSwaps.length})
        </TabsTrigger>
      </TabsList>

      <TabsContent value="received" className="mt-4 space-y-4">
        {receivedSwaps.length === 0 ? (
          <EmptyState
            title="No received requests"
            message="Incoming swap requests for your books will show up here."
          />
        ) : (
          receivedSwaps.map((item) => (
            <SwapCard
              key={item.swapId}
              item={item}
              mode="received"
              onDone={(swapId) =>
                setReceivedSwaps((prev) => prev.filter((s) => s.swapId !== swapId))
              }
            />
          ))
        )}
      </TabsContent>

      <TabsContent value="sent" className="mt-4 space-y-4">
        {sentSwaps.length === 0 ? (
          <EmptyState
            title="No sent requests"
            message="Requests you send to other readers will show up here."
          />
        ) : (
          sentSwaps.map((item) => (
            <SwapCard
              key={item.swapId}
              item={item}
              mode="sent"
              onDone={(swapId) =>
                setSentSwaps((prev) => prev.filter((s) => s.swapId !== swapId))
              }
            />
          ))
        )}
      </TabsContent>
    </Tabs>
  );
}
