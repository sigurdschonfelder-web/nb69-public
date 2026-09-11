package sigurdws.NB69.house;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/email")
public class EmailController {
    private final EmailReminders reminders;
    public EmailController(EmailReminders reminders) { this.reminders=reminders; }
    @GetMapping public EmailReminders.Settings settings() { return reminders.settings(); }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String,String>> invalid(org.springframework.web.server.ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode()).body(java.util.Map.of("message", error.getReason()));
    }
}
