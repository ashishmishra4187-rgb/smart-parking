import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./Layout";
import LotDetail from "./pages/LotDetail";
import Lots from "./pages/Lots";
import Login from "./pages/Login";
import NotFound from "./pages/NotFound";
import Register from "./pages/Register";
import Reservations from "./pages/Reservations";
import Vehicles from "./pages/Vehicles";
import RequireAuth from "./RequireAuth";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/lots" replace />} />
          <Route path="/lots" element={<Lots />} />
          <Route path="/lots/:id" element={<LotDetail />} />
          <Route path="/reservations" element={<Reservations />} />
          <Route path="/vehicles" element={<Vehicles />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  );
}