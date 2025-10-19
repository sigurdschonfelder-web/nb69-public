import "./site.css";
import ChoresSection from "./components/ChoresSection";

export default function Home() {
  return (
    <section className="hero">
      <div className="hero__inner">
        {/* litt ekstra top-padding pga fast navbar */}
        <div className="headline-wrap" style={{ paddingTop: "88px" }}>
          <div className="headline-card">
            <h1 className="headline">Nedre Bakklandet 69</h1>
          </div>
        </div>

        <div className="overlay-area">
          <div className="cards">
            <ChoresSection />
          </div>
        </div>
      </div>

      <div className="hero-fade" />
    </section>
  );
}
