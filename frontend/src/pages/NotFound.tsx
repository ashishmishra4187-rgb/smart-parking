import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <>
      <h1>Page not found</h1>
      <p>That address does not exist.</p>
      <p><Link to="/lots">Go to the parking lots</Link></p>
    </>
  );
}