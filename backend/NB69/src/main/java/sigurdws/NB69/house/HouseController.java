package sigurdws.NB69.house;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class HouseController {
    private final HouseService house;
    private final org.springframework.security.core.session.SessionRegistry sessions;
    public HouseController(HouseService house, org.springframework.security.core.session.SessionRegistry sessions) { this.house=house; this.sessions=sessions; }
    record Activation(String username, String code, String password) {}
    record Assignment(String username) {}
    record Completion(String comment) {}
    @GetMapping("/history") public List<HouseService.Week> history() { return house.history(); }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("headerName", token.getHeaderName(), "token", token.getToken()); }
    @GetMapping("/me") public HouseService.Resident me(Principal principal) { return house.user(principal.getName()); }
    @PostMapping("/activate") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void activate(@RequestBody Activation body) { house.activate(body.username(),body.code(),body.password());
        for (Object principal : sessions.getAllPrincipals()) {
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails user && user.getUsername().equals(body.username()))
                sessions.getAllSessions(principal, false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);
        }
    }
    @GetMapping("/dashboard") public HouseService.Dashboard dashboard(Principal principal) { return house.dashboard(principal.getName()); }
    @GetMapping("/weeks") public List<HouseService.Week> weeks() { return house.weeks(); }
    @PostMapping("/weeks/{start}/tasks/{id}/completion") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void complete(@PathVariable LocalDate start, @PathVariable int id, Principal principal, @RequestBody(required=false) Completion body) { house.complete(start,id,principal.getName(),body == null ? null : body.comment()); }
    @GetMapping("/admin/users") public List<HouseService.Resident> users() { return house.users(); }
    @PostMapping("/admin/users/{username}/invite")
    public Map<String,String> invite(@PathVariable String username, Principal principal) { return Map.of("code",house.invite(username, principal.getName())); }
    @PutMapping("/admin/weeks/{start}/tasks/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void assign(@PathVariable LocalDate start,@PathVariable int id,@RequestBody Assignment body,Principal principal) { house.assign(start,id,body.username(),principal.getName()); }
    @DeleteMapping("/admin/weeks/{start}/tasks/{id}/completion") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void undo(@PathVariable LocalDate start,@PathVariable int id,Principal principal) { house.undo(start,id,principal.getName()); }
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String,String>> error(ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode()).body(Map.of("message", Objects.requireNonNullElse(error.getReason(), "Handlingen kunne ikke utføres.")));
    }
}
