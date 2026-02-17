"use client";

interface Props {
  onClose: () => void;
}

export function NotificationList({ onClose }: Props) {
  return (
    <div className="absolute right-0 mt-2 w-80 rounded-md border bg-background shadow">
      <div className="p-4 text-sm">
        <p className="font-medium">Notifications</p>
        <p className="mt-2 text-muted-foreground">
          Notification feed coming soon.
        </p>
        <button
          onClick={onClose}
          className="mt-4 text-xs text-muted-foreground underline"
        >
          Close
        </button>
      </div>
    </div>
  );
}
