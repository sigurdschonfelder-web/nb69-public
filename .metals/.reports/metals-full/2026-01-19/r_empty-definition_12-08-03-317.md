error id: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java:_empty_/`<any>`#getPrikker#
file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java
empty definition using pc, found symbol in pc: _empty_/`<any>`#getPrikker#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1423
uri: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java
text:
```scala
package sigurdws.NB69.prikker;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = { "http://localhost:5173",
        "https://nb69-public.vercel.app/",
        "https://nb69.no",
        "https://www.nb69.no" }) // dev: tillat frontend
public class PrikkerController {

    @GetMapping("/api/prikker")
    public List<PrikkRow> getPrikker() {
        // Midlertidige data – byttes senere til database/fil om ønskelig
        List<PrikkRow> prikker = new ArrayList<>(List.of(
                new PrikkRow("Andreas", 2),
                new PrikkRow("Jørgen", 0),
                new PrikkRow("Erlend", 2),
                new PrikkRow("Eilif", 4),
                new PrikkRow("Sigurd", 2)));
        return prikker.stream().sorted((o1, o2) -> o2.prikker() - o1.prikker()).toList();
    }

    public List<PrikkRow> increment(String name, boolean increase) {

        return
        this.getPrikker().stream().map((x) -> {
            if (x.getNavn().equals(name) && increase) {
                int current = x.getPrikker(); 
                x.setPrikker(current + 1);
            } else if () {
                int current = x.g@@etPrikker(); 
                x.setPrikker(current + 1);
            }
            else {
                return x;
            }
        }).toList();
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/`<any>`#getPrikker#