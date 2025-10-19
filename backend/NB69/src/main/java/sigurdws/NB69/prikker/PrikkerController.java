package sigurdws.NB69.prikker;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:5173"}) // dev: tillat frontend
public class PrikkerController {

    @GetMapping("/api/prikker")
    public List<PrikkRow> getPrikker() {
        // Midlertidige data – byttes senere til database/fil om ønskelig
        return List.of(
                new PrikkRow("Andreas", 2),
                new PrikkRow("Jørgen", 5),
                new PrikkRow("Erlend", 1),
                new PrikkRow("Eilif", 3),
                new PrikkRow("Sigurd", 0)
        );
    }
}
