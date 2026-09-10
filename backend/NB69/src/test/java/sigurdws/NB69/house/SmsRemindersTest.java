package sigurdws.NB69.house;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:smstest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","nb69.bootstrap-code=test-bootstrap-code-with-at-least-24-characters","nb69.sms.enabled=false"})
class SmsRemindersTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    final List<String> sent=new ArrayList<>();
    @BeforeEach void clean() {
        jdbc.update("delete from nb69_sms_reminders");jdbc.update("delete from nb69_sms_contacts");jdbc.update("delete from nb69_assignments");sent.clear();
    }
    private SmsSender sender(boolean fail) {
        return new SmsSender() {
            public boolean ready(){return true;}
            public String send(String phone,String message) throws Exception { sent.add(phone+" "+message); if(fail) throw new java.io.IOException("Simulated timeout"); return "SMtest"; }
        };
    }
    private HouseService house(Clock clock) {return new HouseService(jdbc,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(),clock);}
    private Clock at(String time) {return Clock.fixed(Instant.parse(time),HouseService.OSLO);}
    private SmsReminders reminders(Clock clock,boolean fail) {return new SmsReminders(jdbc,house(clock),sender(fail),clock,manager);}
    @Test void sendsOnlyOncePerTaskAndStageAndSkipsCompletedTasks() {
        var sunday=at("2026-09-13T12:00:00Z");var house=house(sunday);var tasks=house.weeks().get(0).assignments();
        var reminders=reminders(sunday,false);
        for(var user:house.users()) reminders.contact(user.username(),"+4799999999");
        house.complete(house.currentStart(),tasks.get(0).id(),tasks.get(0).username());
        reminders.dispatch();reminders.dispatch();assertEquals(4,sent.size());
        // A new service after restart must not send a second Sunday SMS.
        reminders(sunday,false).dispatch();assertEquals(4,sent.size());
        var monday=at("2026-09-14T06:00:00Z");
        house(monday).complete(house.currentStart(),tasks.get(1).id(),tasks.get(1).username());
        reminders(monday,false).dispatch();reminders(monday,false).dispatch();assertEquals(7,sent.size());
        assertTrue(sent.get(0).contains("før søndag er over"));assertTrue(sent.get(6).contains("etter fristen"));
    }
    @Test void unavailableContactDoesNotConsumeReminderAndFailureIsNotRetried() {
        var clock=at("2026-09-13T12:00:00Z");var service=reminders(clock,true);
        house(clock).weeks();service.dispatch();assertEquals(0,sent.size());
        service.contact("eilif","+47 9999 9999");service.dispatch();service.dispatch();
        assertEquals(1,sent.size());assertEquals("UNCERTAIN",service.settings().attempts().get(0).status());
    }
    @Test void disabledSenderMakesNoCallsAndCannotClaimJobs() {
        var clock=at("2026-09-13T12:00:00Z");
        SmsSender disabled=new SmsSender(){public boolean ready(){return false;}public String send(String phone,String message){throw new AssertionError("Must not send");}};
        var service=new SmsReminders(jdbc,house(clock),disabled,clock,manager);
        service.contact("eilif","+4799999999");service.dispatch();assertTrue(service.settings().attempts().isEmpty());
    }
}
