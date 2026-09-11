package sigurdws.NB69.house;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class HouseService {
    static final List<String> PEOPLE = List.of("eilif", "sigurd", "andreas", "jorgen", "erlend");
    // Separate from account order: account order must not change administrator roles.
    static final List<String> ROTATION = List.of("andreas", "sigurd", "jorgen", "eilif", "erlend");
    static final LocalDate ROTATION_START = LocalDate.of(2026, 9, 14);
    static String scheduledPerson(LocalDate start, int task) {
        if (start.isBefore(ROTATION_START))
            return PEOPLE.get((task + start.get(WeekFields.ISO.weekOfWeekBasedYear())) % PEOPLE.size());
        String previous = PEOPLE.get((task + ROTATION_START.minusWeeks(1).get(WeekFields.ISO.weekOfWeekBasedYear())) % PEOPLE.size());
        long steps = ChronoUnit.WEEKS.between(ROTATION_START, start) + 1;
        return ROTATION.get(Math.floorMod(ROTATION.indexOf(previous) + steps, ROTATION.size()));
    }
    static final List<String> NAMES = List.of("Eilif", "Sigurd", "Andreas", "Jørgen", "Erlend");
    static final List<String> TASKS = List.of("Kjøkken", "Stue & støvsuging", "Bad", "Papp & glass", "Pant");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public HouseService(JdbcTemplate jdbc, PasswordEncoder encoder, Clock clock) {
        this.jdbc=jdbc; this.encoder=encoder; this.clock=clock;
    }
    public record Resident(String username, String name, boolean admin, boolean active, String avatarVersion) {}
    public record Task(int id, String task, String username, String person, Instant completedAt, boolean late, String avatarVersion, String comment) {}
    public record Week(LocalDate start, int week, String label, Instant dueAt, boolean reminderDue, List<Task> assignments) {}
    public record Overdue(LocalDate start, int week, int id, String task, Instant dueAt) {}
    public record Dashboard(List<Week> weeks, List<Overdue> overdue) {}
    public static final ZoneId OSLO = ZoneId.of("Europe/Oslo");
    public static Instant deadline(LocalDate start) { return start.plusWeeks(1).atStartOfDay(OSLO).toInstant(); }
    public static Instant sundayReminder(LocalDate start) { return start.plusDays(6).atTime(14,0).atZone(OSLO).toInstant(); }
    @Transactional
    public Dashboard dashboard(String actor) {
        var weeks = weeks();
        var overdue = jdbc.query("select week_start,task_id,task_name from nb69_assignments where username=? and week_start<? and completed_at is null order by week_start,task_id", (rs,i) -> {
            var start = rs.getObject("week_start", LocalDate.class);
            return new Overdue(start,start.get(WeekFields.ISO.weekOfWeekBasedYear()),rs.getInt("task_id"),rs.getString("task_name"),deadline(start));
        }, actor,currentStart());
        return new Dashboard(weeks,overdue);
    }
    public LocalDate currentStart() { return LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }
    public List<Resident> users() {
        return jdbc.query("select u.*, a.version as avatar_version from nb69_users u left join nb69_avatars a on a.username=u.username order by display_name", (rs, i) ->
            new Resident(rs.getString("username"), rs.getString("display_name"), rs.getBoolean("admin"), rs.getString("password_hash") != null, rs.getString("avatar_version")));
    }
    public Resident user(String username) {
        return users().stream().filter(u -> u.username().equals(username)).findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ukjent beboer."));
    }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public String invite(String username, String actor) {
        user(username);
        byte[] random = new byte[24]; new SecureRandom().nextBytes(random);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        setInvite(username, code); audit(actor, "INVITE", username);
        return code;
    }
    void setInvite(String username, String code) {
        jdbc.update("update nb69_users set activation_hash=?, activation_expires=? where username=?", digest(code), OffsetDateTime.now(clock).plusHours(24), username);
    }
    @Transactional
    public void activate(String username, String code, String password) {
        if (username == null || code == null || password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(BAD_REQUEST, "Passordet må ha minst 12 tegn og maksimalt 72 byte.");
        // Lock the account: an activation code may only be consumed once.
        var hashes = jdbc.query("select activation_hash from nb69_users where username=? and activation_expires>? for update", (rs,i) -> rs.getString(1), username, OffsetDateTime.now(clock));
        if (hashes.isEmpty() || hashes.get(0) == null || !MessageDigest.isEqual(hashes.get(0).getBytes(StandardCharsets.UTF_8), digest(code).getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(BAD_REQUEST, "Aktiveringskoden er feil eller utløpt.");
        jdbc.update("update nb69_users set password_hash=?, activation_hash=null, activation_expires=null where username=?", encoder.encode(password), username);
        audit(username, "PASSWORD_SET", username);
    }
    public void seedUsers() {
        for (int i=0;i<PEOPLE.size();i++) {
            jdbc.update("insert into nb69_users (username,display_name,admin) select ?,?,? where not exists (select 1 from nb69_users where username=?)", PEOPLE.get(i), NAMES.get(i), i<2, PEOPLE.get(i));
        }
    }
    @Transactional
    public List<Week> weeks() {
        // The resident row is a cross-instance lock for weekly snapshot creation.
        jdbc.queryForObject("select username from nb69_users where username='eilif' for update", String.class);
        var start = currentStart();
        // Fill weeks missed while the server/site was unused, starting only at first use.
        var last = jdbc.queryForObject("select max(week_start) from nb69_assignments", LocalDate.class);
        if (last != null) for (var missing=last.plusWeeks(1); missing.isBefore(start); missing=missing.plusWeeks(1)) ensureWeek(missing);
        return List.of(week(start, "Denne uka"), week(start.plusWeeks(1), "Neste uke"));
    }
    private void ensureWeek(LocalDate start) {
        boolean newRotation = !start.isBefore(ROTATION_START);
        for (int i=0;i<TASKS.size();i++) {
            String person = scheduledPerson(start, i);
            jdbc.update("insert into nb69_assignments (week_start,task_id,task_name,username,rotation_version) select ?,?,?,?,? where not exists (select 1 from nb69_assignments where week_start=? and task_id=?)", start, i, TASKS.get(i), person, newRotation ? 1 : 0, start, i);
            // Upgrade a previously generated preview once, preserving manual assignments and history.
            if (newRotation && start.isAfter(currentStart())) {
                jdbc.update("update nb69_assignments set username=?, rotation_version=1 where week_start=? and task_id=? and rotation_version=0 and completed_at is null and not exists (select 1 from nb69_audit where action='ASSIGN' and detail like ?)", person, start, i, start + "/" + i + " -> %");
            }
        }
    }

    private Week week(LocalDate start, String label) {
        ensureWeek(start);
        return readWeek(start, label);
    }
    public List<Week> history() {
        return jdbc.query("select distinct week_start from nb69_assignments where week_start<=? and week_start>=? order by week_start desc", (rs,i) -> rs.getObject(1, LocalDate.class), currentStart(), currentStart().minusWeeks(11))
            .stream().map(start -> readWeek(start, "Uke " + start.get(WeekFields.ISO.weekOfWeekBasedYear()))).toList();
    }
    private Week readWeek(LocalDate start, String label) {
        int week = start.get(WeekFields.ISO.weekOfWeekBasedYear());
        var tasks = jdbc.query("select a.*, u.display_name, p.version as avatar_version from nb69_assignments a join nb69_users u on u.username=a.username left join nb69_avatars p on p.username=a.username where week_start=? order by task_id", (rs,i) -> {
            var completed = rs.getObject("completed_at", OffsetDateTime.class);
            return new Task(rs.getInt("task_id"), rs.getString("task_name"), rs.getString("username"), rs.getString("display_name"), completed == null ? null : completed.toInstant(), completed != null && !completed.toInstant().isBefore(deadline(start)), rs.getString("avatar_version"), rs.getString("completion_comment"));
        }, start);
        return new Week(start, week, label, deadline(start), !clock.instant().isBefore(sundayReminder(start)) && clock.instant().isBefore(deadline(start)), tasks);
    }
    private void editable(LocalDate start) {
        if (!start.equals(currentStart()) && !start.equals(currentStart().plusWeeks(1))) throw new ResponseStatusException(CONFLICT, "Bare denne og neste uke kan endres. Oppdater siden.");
    }
    @Transactional
    public void complete(LocalDate start, int task, String actor) { complete(start, task, actor, null); }
    @Transactional
    public void complete(LocalDate start, int task, String actor, String comment) {
        if (comment != null && comment.length() > 500) throw new ResponseStatusException(BAD_REQUEST, "Kommentaren kan ha maksimalt 500 tegn.");
        comment = comment == null || comment.isBlank() ? null : comment.strip();
        if (start.isAfter(currentStart())) throw new ResponseStatusException(CONFLICT, "Du kan ikke bekrefte oppgaver for en fremtidig uke.");
        var owners = jdbc.query("select username from nb69_assignments where week_start=? and task_id=? for update", (rs,i) -> rs.getString(1), start, task);
        if (owners.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Oppgaven finnes ikke.");
        if (!owners.get(0).equals(actor)) throw new ResponseStatusException(FORBIDDEN, "Du kan bare bekrefte dine egne oppgaver.");
        int changed = jdbc.update("update nb69_assignments set completed_at=?, completed_by=?, completion_comment=? where week_start=? and task_id=? and completed_at is null", OffsetDateTime.now(clock), actor, comment, start, task);
        if (changed > 0) audit(actor, "COMPLETE", start + "/" + task);
    }
    @Transactional
    public void assign(LocalDate start, int task, String username, String actor) {
        editable(start); user(username);
        int changed = jdbc.update("update nb69_assignments set username=? where week_start=? and task_id=? and completed_at is null", username, start, task);
        if (changed == 0) throw new ResponseStatusException(CONFLICT, "Oppgaven finnes ikke eller er allerede utført. Oppdater siden.");
        audit(actor, "ASSIGN", start + "/" + task + " -> " + username);
    }
    @Transactional
    public void undo(LocalDate start, int task, String actor) {
        editable(start);
        int changed = jdbc.update("update nb69_assignments set completed_at=null, completed_by=null, completion_comment=null where week_start=? and task_id=?", start, task);
        if (changed == 0) throw new ResponseStatusException(NOT_FOUND, "Oppgaven finnes ikke.");
        audit(actor, "UNDO", start + "/" + task);
    }
    private void audit(String actor, String action, String detail) {
        jdbc.update("insert into nb69_audit (actor,action,detail,recorded_at) values (?,?,?,?)", actor, action, detail, OffsetDateTime.now(clock));
    }
}
