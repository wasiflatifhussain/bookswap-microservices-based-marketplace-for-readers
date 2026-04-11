import { cn } from "@/lib/utils";

interface SectionHeaderProps {
  title: string;
  subtitle?: string;
  className?: string;
}

export function SectionHeader({
  title,
  subtitle,
  className,
}: SectionHeaderProps) {
  return (
    <section className={cn("space-y-1", className)}>
      <h1 className="section-title">{title}</h1>
      {subtitle ? <p className="section-subtitle">{subtitle}</p> : null}
    </section>
  );
}
