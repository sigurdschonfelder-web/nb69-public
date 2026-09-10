package sigurdws.NB69.house;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/email")
public class EmailController {
    private final EmailReminders reminders;
    public EmailController(EmailReminders reminders) { this.reminders=reminders; }
    public record ContactUpdate(String email) {}
    @GetMapping public EmailReminders.Settings settings() { return reminders.settings(); }
    @PutMapping("/{username}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void contact(@PathVariable String username,@RequestBody ContactUpdate update) { reminders.contact(username,update.email()); }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String,String>> invalid(org.springframework.web.server.ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode()).body(java.util.Map.of("message", error.getReason()));
    }
}
