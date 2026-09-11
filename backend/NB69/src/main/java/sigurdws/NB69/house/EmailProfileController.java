package sigurdws.NB69.house;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile/email")
public class EmailProfileController {
    private final EmailProfile profile;
    public EmailProfileController(EmailProfile profile){this.profile=profile;}
    public record Address(String email) {}
    public record Verification(String token) {}
    @GetMapping public EmailProfile.Profile get(Principal user){return profile.profile(user.getName());}
    @PutMapping public EmailProfile.Result save(Principal user,@RequestBody Address body){return profile.save(user.getName(),body.email());}
    @PostMapping("/confirm") public EmailProfile.Profile confirm(Principal user,@RequestBody Verification body){return profile.confirm(user.getName(),body.token());}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()));}
}
