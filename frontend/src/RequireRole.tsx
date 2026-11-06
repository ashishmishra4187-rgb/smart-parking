import { Outlet } from "react-router-dom";
import { useAuth } from "./auth";

// Hides pages from the wrong role. This is politeness only: the BACKEND enforces the real rule (403).
export default function RequireRole({ roles }: { roles: string[] }) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) {
    return (
      <>
        <h1>Not allowed</h1>
        <p>This area is for managers.</p>
      </>
    );
  }
  return <Outlet />;
}