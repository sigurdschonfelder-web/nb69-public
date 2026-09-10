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
    static final List<String> NAMES = List.of("Eilif", "Sigurd", "Andreas", "Jørgen", "Erlend");
    static final List<String> TASKS = List.of("Kjøkken", "Stue & støvsuging", "Bad", "Papp & glass", "Pant");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public HouseService(JdbcTemplate jdbc, PasswordEncoder encoder, Clock clock) {
        this.jdbc=jdbc; this.encoder=encoder; this.clock=clock;
    }
    public record Resident(String username, String name, boolean admin, boolean active) {}
    public record Task(int id, String task, String username, String person, Instant completedAt) {}
    public record Week(LocalDate start, int week, String label, List<Task> assignments) {}
    public LocalDate currentStart() { return LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }
    public List<Resident> users() {
        return jdbc.query("select * from nb69_users order by display_name", (rs, i) ->
            new Resident(rs.getString("username"), rs.getString("display_name"), rs.getBoolean("admin"), rs.getString("password_hash") != null));
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
        return List.of(week(start, "Denne uka"), week(start.plusWeeks(1), "Neste uke"));
    }
    private Week week(LocalDate start, String label) {
        int week = start.get(WeekFields.ISO.weekOfWeekBasedYear());
        for (int i=0;i<TASKS.size();i++) {
            String person = PEOPLE.get((i + week) % PEOPLE.size());
            jdbc.update("insert into nb69_assignments (week_start,task_id,task_name,username) select ?,?,?,? where not exists (select 1 from nb69_assignments where week_start=? and task_id=?)", start, i, TASKS.get(i), person, start, i);
        }
        var tasks = jdbc.query("select a.*, u.display_name from nb69_assignments a join nb69_users u on u.username=a.username where week_start=? order by task_id", (rs,i) -> {
            var completed = rs.getObject("completed_at", OffsetDateTime.class);
            return new Task(rs.getInt("task_id"), rs.getString("task_name"), rs.getString("username"), rs.getString("display_name"), completed == null ? null : completed.toInstant());
        }, start);
        return new Week(start, week, label, tasks);
    }
    private void editable(LocalDate start) {
        if (!start.equals(currentStart()) && !start.equals(currentStart().plusWeeks(1))) throw new ResponseStatusException(CONFLICT, "Bare denne og neste uke kan endres. Oppdater siden.");
    }
    @Transactional
    public void complete(LocalDate start, int task, String actor) {
        if (!start.equals(currentStart())) throw new ResponseStatusException(CONFLICT, "Uka har endret seg. Oppdater siden før du bekrefter.");
        var owners = jdbc.query("select username from nb69_assignments where week_start=? and task_id=? for update", (rs,i) -> rs.getString(1), start, task);
        if (owners.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Oppgaven finnes ikke.");
        if (!owners.get(0).equals(actor)) throw new ResponseStatusException(FORBIDDEN, "Du kan bare bekrefte dine egne oppgaver.");
        int changed = jdbc.update("update nb69_assignments set completed_at=?, completed_by=? where week_start=? and task_id=? and completed_at is null", OffsetDateTime.now(clock), actor, start, task);
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
        int changed = jdbc.update("update nb69_assignments set completed_at=null, completed_by=null where week_start=? and task_id=?", start, task);
        if (changed == 0) throw new ResponseStatusException(NOT_FOUND, "Oppgaven finnes ikke.");
        audit(actor, "UNDO", start + "/" + task);
    }
    private void audit(String actor, String action, String detail) {
        jdbc.update("insert into nb69_audit (actor,action,detail,recorded_at) values (?,?,?,?)", actor, action, detail, OffsetDateTime.now(clock));
    }
}
