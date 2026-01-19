package sigurdws.NB69.prikker;


import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Service
public class PrikkManager {
  private final JdbcTemplate jdbc;

  public PrikkManager(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<PrikkRow> getPrikker() {
    return jdbc.query(
      "select navn, prikker from prikker order by navn",
      (rs, i) -> new PrikkRow(rs.getString("navn"), rs.getInt("prikker"))
    );
  }

  public List<PrikkRow> updatePrikker(String navn, boolean increase) {
    int delta = increase ? 1 : -1;

    int updated = jdbc.update(
      "update prikker set prikker = greatest(0, prikker + ?) where navn = ?",
      delta, navn
    );

    if (updated == 0) throw new IllegalArgumentException("Fant ikke: " + navn);
    return getPrikker(); // returner liste så frontend slipper endring
  }
}


