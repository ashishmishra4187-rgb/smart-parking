import { useEffect, useState } from "react";
import "./status.css";

export default function OfflineBanner() {
  const [online, setOnline] = useState(() => navigator.onLine);

  useEffect(() => {
    const goOnline = () => setOnline(true);
    const goOffline = () => setOnline(false);
    window.addEventListener("online", goOnline);
    window.addEventListener("offline", goOffline);
    return () => {
      window.removeEventListener("online", goOnline);
      window.removeEventListener("offline", goOffline);
    };
  }, []);

  if (online) return null;
  return (
    <div className="offline-banner" role="status">
      You are offline. Nothing can be saved until the connection comes back.
    </div>
  );
}