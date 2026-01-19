error id: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java:_empty_/PrikkManager#updatePrikker#
file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java
empty definition using pc, found symbol in pc: _empty_/PrikkManager#updatePrikker#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1139
uri: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkerController.java
text:
```scala
package sigurdws.NB69.prikker;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
@CrossOrigin(origins = { "http://localhost:5173",
        "https://nb69-public.vercel.app/",
        "https://nb69.no",
        "https://www.nb69.no" }) // dev: tillat frontend
public class PrikkerController {

    @GetMapping("/api/prikker")
    public List<PrikkRow> getPrikker() {
        // Midlertidige data – byttes senere til database/fil om ønskelig
        PrikkManager pc = new PrikkManager();
        List<PrikkRow> prikker = pc.readPrikker();
        return prikker.stream().sorted((o1, o2) -> o2.prikker() - o1.prikker()).toList();
    }

    @PutMapping("/api/prikker/update")
    public List<PrikkRow> incrementPrikker(String name, boolean increase) {
        PrikkManager pc = new PrikkManager();
        return pc.updat@@ePrikker(name, increase)


        // return
        // this.getPrikker().stream().map((x) -> {
        //     if (x.navn().equals(name) && increase) {
        //         int current = x.prikker(); 
        //         x.prikker = x.prikker + 1;
        //         return x;
        //     } else if (x.getNavn().equals(name) && !increase) {
        //         int current = x.getPrikker(); 
        //         x.setPrikker(current - 1);
        //         return x;
        //     }
        //     else {
        //         return x;
        //     }
        // }).toList();
    }

    public static void main(String[] args) {
        PrikkerController pc = new PrikkerController();
        System.out.println(pc.getPrikker());
        // System.out.println(pc.incrementPrikker("Jørgen", true));
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/PrikkManager#updatePrikker#