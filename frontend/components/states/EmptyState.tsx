import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  title?: string;
  message: string;
  className?: string;
}

export function EmptyState({
  title = "Nothing here yet",
  message,
  className,
}: EmptyStateProps) {
  return (
    <Card className={cn("surface-card rounded-md px-6 py-10 text-center", className)}>
      <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
      <p className="mt-2 text-sm text-muted-foreground">{message}</p>
    </Card>
  );
}
