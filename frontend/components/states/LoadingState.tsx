import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface LoadingStateProps {
  message?: string;
  className?: string;
}

export function LoadingState({
  message = "Loading content...",
  className,
}: LoadingStateProps) {
  return (
    <Card
      className={cn(
        "surface-card rounded-md px-6 py-10 text-center",
        className,
      )}
    >
      <div className="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-primary" />
      <p className="mt-3 text-sm text-muted-foreground">{message}</p>
    </Card>
  );
}
