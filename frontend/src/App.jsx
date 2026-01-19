import { BrowserRouter, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import Home from "./Home";
import Prikker from "./pages/Prikker";
import Admin from "./pages/Admin";
import { useState } from "react";

export default function App() {
  const[isLoggedIn, setIsLoggedIn] = useState(false);

  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home isLoggedIn={isLoggedIn}/>} />
        <Route path="/prikker" element={<Prikker isLoggedIn={isLoggedIn}/>} />
        <Route path="/admin" element={<Admin isLoggedIn={isLoggedIn} setIsLoggedIn={setIsLoggedIn}/>} />
      </Routes>
    </BrowserRouter>
  );
}

