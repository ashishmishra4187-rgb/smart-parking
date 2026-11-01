import { useState } from "react";
import type { AppNotification } from "../types";

interface Props {
  items: AppNotification[];
  unread: number;
  onRead: (id: number) => void;
}

export default function NotificationBell({ items, unread, onRead }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <div className="bell-wrap">
      <button
        type="button"
        className="small bell"
        aria-expanded={open}
        aria-label={`Notifications, ${unread} unread`}
        onClick={() => setOpen((o) => !o)}
      >
        Notifications
        {unread > 0 && <span className="bell-count">{unread}</span>}
      </button>

      {open && (
        <div className="bell-panel" role="region" aria-label="Notifications">
          {items.length === 0 && <p className="muted">No notifications yet.</p>}
          <ul>
            {items.slice(0, 10).map((n) => (
              <li key={n.id} className={n.read ? "read" : "unread"}>
                <div>{n.message}</div>
                <div className="muted">
                  {new Date(n.createdAt).toLocaleString()}
                  {!n.read && (
                    <button type="button" className="small link" onClick={() => onRead(n.id)}>
                      Mark read
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}