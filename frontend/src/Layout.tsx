import { NavLink, Outlet } from "react-router-dom";
import NotificationBell from "./components/NotificationBell";
import OfflineBanner from "./components/OfflineBanner";
import { useAuth } from "./auth";
import { useNotifications } from "./useNotifications";
import "./components/reserve.css";

export default function Layout() {
  const { user, logout } = useAuth();
  const { items, unread, markRead } = useNotifications();

  return (
    <>
      <OfflineBanner />
      <header className="topbar">
        <strong>Smart Parking</strong>
        <nav>
          <NavLink to="/lots">Lots</NavLink>
          <NavLink to="/reservations">My reservations</NavLink>
          <NavLink to="/vehicles">My vehicles</NavLink>
        </nav>
        <span className="spacer" />
        <NotificationBell items={items} unread={unread} onRead={markRead} />
        <span>{user?.name}</span>
        <button className="small" onClick={logout}>Log out</button>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}