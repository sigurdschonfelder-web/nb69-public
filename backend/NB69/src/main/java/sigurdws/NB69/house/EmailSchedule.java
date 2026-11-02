package sigurdws.NB69.house;

import java.time.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class EmailSchedule {
    private final EmailReminders reminders;
    private final Clock clock;
    private EmailReminders.Window lastWindow;
    private Instant lastRun=Instant.MIN;
    public EmailSchedule(EmailReminders reminders,Clock clock) { this.reminders=reminders; this.clock=clock; }
    @Scheduled(initialDelay=60000, fixedDelay=60000)
    public void run() {
        var now=clock.instant();
        var window=EmailReminders.window(now);
        if (window==null) return;
        // Every pass wakes the database; within a reminder window an hourly catch-up is enough.
        if (window.equals(lastWindow) && now.isBefore(lastRun.plus(Duration.ofHours(1)))) return;
        lastWindow=window; lastRun=now;
        reminders.dispatch();
    }
}
