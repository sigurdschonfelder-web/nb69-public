package sigurdws.NB69.house;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {
    private final PasswordReset reset;private final SessionRegistry sessions;
    public PasswordResetController(PasswordReset reset,SessionRegistry sessions){this.reset=reset;this.sessions=sessions;}
    record Request(String username){}
    record Confirm(String token,String password){}
    @PostMapping("/request") public Map<String,String> request(@RequestBody Request body){reset.request(body.username());return Map.of("message","Hvis kontoen har en bekreftet e-postadresse og e-postutsending er aktivert, får du en lenke. Sjekk også søppelpost. Vent fem minutter før du prøver igjen.");}
    @PostMapping("/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@RequestBody Confirm body,jakarta.servlet.http.HttpServletRequest request){String username=reset.confirm(body.token(),body.password());for(Object principal:sessions.getAllPrincipals())if(principal instanceof UserDetails user && user.getUsername().equals(username))sessions.getAllSessions(principal,false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);var session=request.getSession(false);if(session!=null)session.invalidate();}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> error(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()));}
}
