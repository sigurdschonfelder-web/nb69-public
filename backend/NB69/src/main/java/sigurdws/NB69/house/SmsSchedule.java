package sigurdws.NB69.house;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SmsSchedule {
    private final SmsReminders reminders;
    public SmsSchedule(SmsReminders reminders) { this.reminders=reminders; }
    @Scheduled(initialDelay=60000, fixedDelay=60000)
    public void run() { reminders.dispatch(); }
}
