package sigurdws.NB69.core;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class choreAssignment {

    private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");

    private final List<String> people = List.of("Eilif", "Sigurd", "Andreas", "Jørgen", "Erlend");
    private final List<String> tasks = List.of("Kjøkken", "Stue & Støvsuging", "Bad", "Papp & Glass", "Pant");

    public int currentIsoWeek() {
        var today = LocalDate.now(OSLO);
        var iso = WeekFields.ISO;
        return today.get(iso.weekOfWeekBasedYear());
    }

    public int currentIsoWeekYear() {
        var today = LocalDate.now(OSLO);
        var iso = WeekFields.ISO;
        return today.get(iso.weekBasedYear());
    }

    public List<Assignment> assignmentsFor(int week) {

        int offset = Math.floorMod(week, people.size());

        List<Assignment> list = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            String person = people.get((i + offset) % people.size());
            list.add(new Assignment(tasks.get(i), person));
        }
        return list;
    }
}
