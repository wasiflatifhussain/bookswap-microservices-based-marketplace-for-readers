import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

interface StatPillProps {
  children: ReactNode;
  className?: string;
}

export function StatPill({ children, className }: StatPillProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-sm border border-border bg-accent/45 px-2 py-0.5 text-xs font-semibold tracking-wide",
        className,
      )}
    >
      {children}
    </span>
  );
}
