error id: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java:org/springframework/web/bind/annotation/PostMapping#
file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java
empty definition using pc, found symbol in pc: org/springframework/web/bind/annotation/PostMapping#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 207
uri: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java
text:
```scala
package sigurdws.NB69.prikker;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMappi@@ng;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;


@RestController
@CrossOrigin(origins = { "http://localhost:5173",
        "https://nb69-public.vercel.app/",
        "https://nb69.no",
        "https://www.nb69.no" }) // dev: tillat frontend
public class PrikkerController {

    private final PrikkManager prikkManager;

    public PrikkerController(PrikkManager prikkManager) {
        this.prikkManager = prikkManager;
    }

    @GetMapping("/api/prikker")
    public List<PrikkRow> getPrikker() {
        return prikkManager.getPrikker()
                .stream()
                .sorted(Comparator.comparingInt(PrikkRow::prikker).reversed())
                .toList();
    }

    @PutMapping("/api/prikker/update")
    public List<PrikkRow> incrementPrikker(@RequestBody PrikkUpdateRequest req) {
        return prikkManager.updatePrikker(req.navn(), req.increase());
    }

}

```


#### Short summary: 

empty definition using pc, found symbol in pc: org/springframework/web/bind/annotation/PostMapping#