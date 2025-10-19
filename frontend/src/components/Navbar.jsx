import { useState } from "react";
import { Link, NavLink } from "react-router-dom";
import "./navbar.css";

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  return (
    <header className="nb-nav">
      <div className="nb-nav__inner">
        <Link to="/" className="nb-brand" onClick={close}>
          NB69
        </Link>

        <button
          className="nb-burger"
          aria-label="Meny"
          aria-expanded={open}
          onClick={() => setOpen(!open)}
        >
          <span />
          <span />
          <span />
        </button>

        <nav className={`nb-menu ${open ? "is-open" : ""}`}>
          <NavLink to="/" end className="nb-link" onClick={close}>
            Hjem
          </NavLink>
          <NavLink to="/prikker" className="nb-link" onClick={close}>
            Prikker
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
