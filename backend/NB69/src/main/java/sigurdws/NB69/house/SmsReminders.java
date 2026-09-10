package sigurdws.NB69.house;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class SmsReminders {
    private final JdbcTemplate jdbc;
    private final HouseService house;
    private final SmsSender sender;
    private final Clock clock;
    private final TransactionTemplate transaction;
    public SmsReminders(JdbcTemplate jdbc,HouseService house,SmsSender sender,Clock clock,PlatformTransactionManager manager) {
        this.jdbc=jdbc; this.house=house; this.sender=sender; this.clock=clock; transaction=new TransactionTemplate(manager);
    }
    public record Contact(String username,String name,String phone) {}
    public record Attempt(LocalDate start,int taskId,String kind,String name,String status,Instant attemptedAt) {}
    public record Settings(boolean ready,List<Contact> contacts,List<Attempt> attempts) {}
    public record Window(LocalDate start,String kind) {}
    private record Candidate(int id,String task,String username,String name) {}
    /** A late-running worker catches up only on the intended calendar day. */
    public static Window window(Instant instant) {
        var now=instant.atZone(HouseService.OSLO);
        var start=now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (now.getDayOfWeek()==DayOfWeek.SUNDAY && now.getHour()>=14) return new Window(start,"SUNDAY");
        if (now.getDayOfWeek()==DayOfWeek.MONDAY && now.getHour()>=8) return new Window(start.minusWeeks(1),"MONDAY");
        return null;
    }
    public Settings settings() {
        var contacts=jdbc.query("select u.username,u.display_name,c.phone from nb69_users u left join nb69_sms_contacts c on c.username=u.username order by u.display_name", (rs,i) -> new Contact(rs.getString(1),rs.getString(2),Objects.requireNonNullElse(rs.getString(3),"")));
        var attempts=jdbc.query("select r.*,u.display_name from nb69_sms_reminders r join nb69_users u on u.username=r.username order by attempted_at desc limit 20", (rs,i) -> new Attempt(rs.getObject("week_start",LocalDate.class),rs.getInt("task_id"),rs.getString("kind"),rs.getString("display_name"),rs.getString("status"),rs.getObject("attempted_at",OffsetDateTime.class).toInstant()));
        return new Settings(sender.ready(),contacts,attempts);
    }
    public void contact(String username,String phone) {
        house.user(username);
        if (phone==null) throw new ResponseStatusException(BAD_REQUEST,"Oppgi et mobilnummer.");
        String normalized=phone.replaceAll("[\\s()-]", "");
        if (!normalized.isEmpty() && !normalized.matches("\\+[1-9][0-9]{7,14}")) throw new ResponseStatusException(BAD_REQUEST,"Bruk landskode, for eksempel +47 etterfulgt av 8 siffer.");
        transaction.executeWithoutResult(status -> {
            jdbc.queryForObject("select username from nb69_users where username=? for update",String.class,username);
            jdbc.update("delete from nb69_sms_contacts where username=?",username);
            if (!normalized.isEmpty()) jdbc.update("insert into nb69_sms_contacts (username,phone) values (?,?)",username,normalized);
        });
    }
    public void dispatch() {
        if (!sender.ready()) return;
        var window=window(clock.instant());
        if (window==null) return;
        house.weeks();
        var candidates=jdbc.query("select a.task_id,a.task_name,a.username,u.display_name from nb69_assignments a join nb69_users u on u.username=a.username where a.week_start=? and a.completed_at is null", (rs,i)->new Candidate(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4)),window.start());
        for (var candidate:candidates) {
            boolean claimed=Boolean.TRUE.equals(transaction.execute(status -> {
                // Serialize claims using the assignment lock, across processes as well.
                var owners=jdbc.query("select username from nb69_assignments where week_start=? and task_id=? and completed_at is null for update",(rs,i)->rs.getString(1),window.start(),candidate.id());
                if (owners.isEmpty() || !owners.get(0).equals(candidate.username())) return false;
                if (jdbc.queryForObject("select count(*) from nb69_sms_contacts where username=?",Integer.class,candidate.username())==0) return false;
                if (jdbc.queryForObject("select count(*) from nb69_sms_reminders where week_start=? and task_id=? and kind=?",Integer.class,window.start(),candidate.id(),window.kind())>0) return false;
                jdbc.update("insert into nb69_sms_reminders (week_start,task_id,kind,username,status,attempted_at) values (?,?,?,?,?,?)",window.start(),candidate.id(),window.kind(),candidate.username(),"CLAIMED",OffsetDateTime.now(clock));
                return true;
            }));
            if (!claimed) continue;
            // A durable CLAIMED row prevents a second SMS after an uncertain network outcome or crash.
            transaction.executeWithoutResult(status -> {
                var owners=jdbc.query("select username from nb69_assignments where week_start=? and task_id=? and completed_at is null for update",(rs,i)->rs.getString(1),window.start(),candidate.id());
                var phones=jdbc.query("select phone from nb69_sms_contacts where username=?",(rs,i)->rs.getString(1),candidate.username());
                if (owners.isEmpty() || !owners.get(0).equals(candidate.username()) || phones.isEmpty() || !window.equals(window(clock.instant()))) {
                    finish(window,candidate.id(),"SKIPPED",null); return;
                }
                String message=message(window,candidate.name(),candidate.task());
                try { finish(window,candidate.id(),"ACCEPTED",sender.send(phones.get(0),message)); }
                catch (Exception error) {
                    if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                    finish(window,candidate.id(),"UNCERTAIN",null);
                }
            });
        }
    }
    static String message(Window window,String name,String task) {
        int week=window.start().get(WeekFields.ISO.weekOfWeekBasedYear());
        return window.kind().equals("SUNDAY")
            ? "NB69: Hei "+name+"! "+task+" (uke "+week+") er ikke bekreftet. Husk å gjøre det før søndag er over og bekreft på nb69.no."
            : "NB69: Hei "+name+"! "+task+" (uke "+week+") mangler fortsatt etter fristen. Gjør det så snart som mulig og bekreft på nb69.no.";
    }
    private void finish(Window window,int id,String state,String sid) {
        jdbc.update("update nb69_sms_reminders set status=?,provider_id=? where week_start=? and task_id=? and kind=?",state,sid,window.start(),id,window.kind());
    }
}
