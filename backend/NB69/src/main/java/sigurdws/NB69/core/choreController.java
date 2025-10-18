package sigurdws.NB69.core;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chores")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://<DITT-FRONTEND-DOMENE>.vercel.app" // Bytt til domene
})
public class choreController {

    private choreAssignment assignment;

    public choreController(choreAssignment assignment) {
        this.assignment = assignment;
    }

    record ChoreResponse(int week, int weekYear, List<Assignment> assignments) {
    }

    @GetMapping("/current")
    public ChoreResponse current() {
        int week = assignment.currentIsoWeek();
        int year = assignment.currentIsoWeekYear();
        var list = assignment.assignmentsFor(week).stream()
                .map(a -> new Assignment(a.getTask(), a.getPerson()))
                .toList();
        return new ChoreResponse(week, year, list);
    }

    // Slår opp vilkårlig uke
    @GetMapping
    public ChoreResponse byWeek(@RequestParam int weekYear, @RequestParam int week) {
        return new ChoreResponse(week, weekYear, assignment.assignmentsFor(week));
    }

}
