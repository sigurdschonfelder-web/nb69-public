package sigurdws.NB69.house;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:emailtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","nb69.bootstrap-code=test-bootstrap-code-with-at-least-24-characters","nb69.email.enabled=false"})
class EmailRemindersTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    final List<String> sent=new ArrayList<>();
    @BeforeEach void clean() {
        jdbc.update("delete from nb69_email_reminders");jdbc.update("delete from nb69_email_contacts");jdbc.update("delete from nb69_assignments");sent.clear();
    }
    private EmailSender sender(boolean fail) {
        return new EmailSender() {
            public boolean ready(){return true;}
            public String send(String email,String subject, String message, String key) throws Exception { sent.add(email+" "+message); if(fail) throw new java.io.IOException("Simulated timeout"); return "test-email-id"; }
        };
    }
    private HouseService house(Clock clock) {return new HouseService(jdbc,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(),clock);}
    private Clock at(String time) {return Clock.fixed(Instant.parse(time),HouseService.OSLO);}
    private EmailReminders reminders(Clock clock,boolean fail) {return new EmailReminders(jdbc,house(clock),sender(fail),clock,manager);}
    @Test void sendsOnlyOncePerTaskAndStageAndSkipsCompletedTasks() {
        var sunday=at("2026-09-13T12:00:00Z");var house=house(sunday);var tasks=house.weeks().get(0).assignments();
        var reminders=reminders(sunday,false);
        for(var user:house.users()) reminders.contact(user.username(),"resident@example.com");
        jdbc.update("update nb69_email_contacts set verified_at=current_timestamp");
        house.complete(house.currentStart(),tasks.get(0).id(),tasks.get(0).username());
        reminders.dispatch();reminders.dispatch();assertEquals(4,sent.size());
        // A new service after restart must not send a second Sunday email.
        reminders(sunday,false).dispatch();assertEquals(4,sent.size());
        var monday=at("2026-09-14T06:00:00Z");
        house(monday).complete(house.currentStart(),tasks.get(1).id(),tasks.get(1).username());
        reminders(monday,false).dispatch();reminders(monday,false).dispatch();assertEquals(7,sent.size());
        assertTrue(sent.get(0).contains("før søndag er over"));assertTrue(sent.get(6).contains("etter fristen"));
    }
    @Test void unavailableContactDoesNotConsumeReminderAndFailureIsNotRetried() {
        var clock=at("2026-09-13T12:00:00Z");var service=reminders(clock,true);
        house(clock).weeks();service.dispatch();assertEquals(0,sent.size());
        service.contact("eilif"," resident@example.com ");jdbc.update("update nb69_email_contacts set verified_at=current_timestamp");service.dispatch();service.dispatch();
        assertEquals(1,sent.size());assertEquals("UNCERTAIN",service.settings().attempts().get(0).status());
    }
    @Test void disabledSenderMakesNoCallsAndCannotClaimJobs() {
        var clock=at("2026-09-13T12:00:00Z");
        EmailSender disabled=new EmailSender(){public boolean ready(){return false;}public String send(String email,String subject, String message, String key){throw new AssertionError("Must not send");}};
        var service=new EmailReminders(jdbc,house(clock),disabled,clock,manager);
        service.contact("eilif","resident@example.com");service.dispatch();assertTrue(service.settings().attempts().isEmpty());
    }
    @Test void emailAddressesAreValidatedStoredAndRemovable() {
        var service=reminders(at("2026-09-13T12:00:00Z"),false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.contact("eilif","not-an-email"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.contact("eilif","one@example.com\nBcc:two@example.com"));
        service.contact("eilif"," eilif@example.com ");
        assertEquals("eilif@example.com",service.settings().contacts().stream().filter(c->c.username().equals("eilif")).findFirst().orElseThrow().email());
        service.contact("eilif","");
        assertEquals("",service.settings().contacts().stream().filter(c->c.username().equals("eilif")).findFirst().orElseThrow().email());
    }
    @Test void unverifiedAddressesNeverReceiveReminders() {
        var clock=at("2026-09-13T12:00:00Z");var service=reminders(clock,false);
        house(clock).weeks();service.contact("eilif","eilif@example.com");
        service.dispatch();assertTrue(sent.isEmpty());assertTrue(service.settings().attempts().isEmpty());
    }
}
