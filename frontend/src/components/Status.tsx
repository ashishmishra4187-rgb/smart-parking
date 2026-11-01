import type { ReactNode } from "react";
import "./status.css";

export function LoadingBox({ text = "Loading..." }: { text?: string }) {
  return <p className="status-box" role="status">{text}</p>;
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="error" role="alert">
      {message}
      {onRetry && (
        <button type="button" className="small" onClick={onRetry}>Try again</button>
      )}
    </div>
  );
}

export function EmptyBox({ children }: { children: ReactNode }) {
  return <p className="empty-box">{children}</p>;
}