import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "./auth";

export default function Layout() {
  const { user, logout } = useAuth();

  return (
    <>
      <header className="topbar">
        <strong>Smart Parking</strong>
        <nav>
          <NavLink to="/lots">Lots</NavLink>
          <NavLink to="/vehicles">My vehicles</NavLink>
        </nav>
        <span className="spacer" />
        <span>{user?.name}</span>
        <button className="small" onClick={logout}>Log out</button>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}