package sigurdws.NB69.house;

import java.time.*;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class EmailProfile {
    public record Profile(String email, boolean verified, boolean sendingEnabled, Instant expiresAt) {}
    public record Result(Profile profile, String message, String previewLink) {}
    private final JdbcTemplate jdbc;
    private final EmailReminders reminders;
    private final EmailSender sender;
    private final Clock clock;
    private final boolean local;
    private final String site;
    public EmailProfile(JdbcTemplate jdbc,EmailReminders reminders,EmailSender sender,Clock clock,
            @Value("${nb69.local:false}") boolean local,@Value("${nb69.site-url:https://nb69.no}") String site) {
        this.jdbc=jdbc;this.reminders=reminders;this.sender=sender;this.clock=clock;this.local=local;this.site=site.replaceAll("/+$","");
        if (!this.site.startsWith("https://") && !(local && this.site.startsWith("http://127.0.0.1:")))
            throw new IllegalArgumentException("NB69_SITE_URL must use HTTPS in production");
    }
    public Profile profile(String username) {
        return jdbc.query("select email,verified_at,token_expires from nb69_email_contacts where username=?",(rs,i)-> {
            var expires=rs.getObject(3,OffsetDateTime.class);
            return new Profile(rs.getString(1),rs.getObject(2)!=null,sender.ready(),expires==null?null:expires.toInstant());
        },username).stream().findFirst().orElse(new Profile("",false,sender.ready(),null));
    }
    @Transactional
    public Result save(String username,String email) {
        jdbc.queryForObject("select username from nb69_users where username=? for update",String.class,username);
        var old=profile(username);
        if(email==null || email.isBlank()) throw new ResponseStatusException(BAD_REQUEST,"Oppgi e-postadressen din.");
        if(old.verified() && old.email().equals(email.strip())) return new Result(old,"Adressen er allerede bekreftet.",null);
        var last=jdbc.query("select last_sent from nb69_email_verification_limits where username=?",(rs,i)->rs.getObject(1,OffsetDateTime.class),username);
        if(!last.isEmpty() && last.get(0).toInstant().plusSeconds(60).isAfter(clock.instant()))
            throw new ResponseStatusException(TOO_MANY_REQUESTS,"Vent ett minutt før du ber om en ny bekreftelseslenke.");
        reminders.contact(username,email);
        if(!local && !sender.ready()) return new Result(profile(username),"Adressen er lagret. E-posttjenesten er ikke aktivert ennå. Be om bekreftelseslenke når den er klar.",null);
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        jdbc.update("update nb69_email_contacts set token_hash=?,token_expires=? where username=?",HouseService.digest(token),OffsetDateTime.now(clock).plusHours(24),username);
        jdbc.update("delete from nb69_email_verification_limits where username=?",username);
        jdbc.update("insert into nb69_email_verification_limits (username,last_sent) values (?,?)",username,OffsetDateTime.now(clock));
        String link=site+"/#confirm-email="+token;
        if(local) return new Result(profile(username),"Lokal test: ingen e-post er sendt. Bruk testlenken for å bekrefte adressen.",link);
        try {
            sender.send(email.strip(),"Bekreft e-postadressen din for NB69", "Hei!\n\nBekreft e-postadressen din for å motta oppgavepåminnelser fra NB69:\n"+link+"\n\nLogg inn på din egen konto og trykk Bekreft e-postadresse. Lenken gjelder i 24 timer. Hvis du ikke ba om dette, kan du se bort fra meldingen.","verify/"+HouseService.digest(token));
        } catch(Exception error) {
            if(error instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Result(profile(username),"Vi kunne ikke bekrefte utsendingen. Sjekk innboksen; hvis lenken ikke kommer, be om en ny etter ett minutt.",null);
        }
        return new Result(profile(username),"Bekreftelseslenken er sendt til e-posttjenesten. Sjekk innboksen og eventuelt søppelpost.",null);
    }
    @Transactional
    public Profile confirm(String username,String token) {
        if(token==null || token.length()>128) throw new ResponseStatusException(BAD_REQUEST,"Lenken er ugyldig eller utløpt.");
        jdbc.queryForObject("select username from nb69_users where username=? for update",String.class,username);
        int changed=jdbc.update("update nb69_email_contacts set verified_at=?,token_hash=null,token_expires=null where username=? and token_hash=? and token_expires>?",OffsetDateTime.now(clock),username,HouseService.digest(token),OffsetDateTime.now(clock));
        if(changed!=1) throw new ResponseStatusException(BAD_REQUEST,"Lenken er ugyldig, utløpt eller tilhører en annen konto. Be om en ny lenke i Min profil.");
        return profile(username);
    }
}
