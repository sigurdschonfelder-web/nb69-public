package sigurdws.NB69.prikker;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
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
        List<PrikkRow> prikker = new ArrayList<>(List.of(
                new PrikkRow("Andreas", 0),
                new PrikkRow("Jørgen", 0),
                new PrikkRow("Erlend", 0),
                new PrikkRow("Eilif", 0),
                new PrikkRow("Sigurd", 0)));
        return prikker.stream().sorted((o1, o2) -> o1.prikker() - o2.prikker()).toList();
    }
}
