error id: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java:_empty_/JdbcTemplate#
file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java
empty definition using pc, found symbol in pc: _empty_/JdbcTemplate#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 259
uri: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java
text:
```scala
package sigurdws.NB69.prikker;


import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Service
public class PrikkManager {
  private final JdbcTemplate jdbc;

  public PrikkManager(JdbcTem@@plate jdbc) { this.jdbc = jdbc; }

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



```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/JdbcTemplate#