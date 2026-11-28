import { NavLink, Outlet, useLocation } from "react-router-dom";
import ArrivalPrompt from "./components/ArrivalPrompt";
import NotificationBell from "./components/NotificationBell";
import OfflineBanner from "./components/OfflineBanner";
import { useAuth } from "./auth";
import { useNotifications } from "./useNotifications";
import "./components/reserve.css";

export default function Layout() {
  const { pathname } = useLocation();
  const { user, logout } = useAuth();
  const { items, unread, markRead } = useNotifications();
  const isManager = user?.role === "MANAGER" || user?.role === "ADMIN";

  return (
    <div className={pathname.startsWith("/manager") ? "" : "night-garage"}>
      <OfflineBanner />
      <header className="topbar">
        <strong className="brand">Smart Parking</strong>
        <nav aria-label="Main navigation">
          <NavLink to="/lots">Lots</NavLink>
          <NavLink to="/reservations">My reservations</NavLink>
          <NavLink to="/visits">My visit</NavLink>
          <NavLink to="/vehicles">My vehicles</NavLink>
          {isManager && <NavLink to="/manager" end>Manager</NavLink>}
          {isManager && <NavLink to="/manager/cashier">Cashier</NavLink>}
          {isManager && <NavLink to="/manager/gate">Gate and reviews</NavLink>}
        </nav>
        <div className="account-controls">
          <NotificationBell items={items} unread={unread} onRead={markRead} />
          <span className="account-name" title={user?.name}>{user?.name}</span>
          <button className="small" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="page">
        {!pathname.startsWith("/manager") && pathname !== "/visits" && <ArrivalPrompt />}
        <Outlet />
      </main>
    </div>
  );
}