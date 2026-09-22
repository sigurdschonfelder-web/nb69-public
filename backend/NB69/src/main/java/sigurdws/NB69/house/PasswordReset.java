package sigurdws.NB69.house;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class PasswordReset {
    private final JdbcTemplate jdbc;
    private final EmailSender sender;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String site;
    public PasswordReset(JdbcTemplate jdbc,EmailSender sender,PasswordEncoder encoder,Clock clock,@Value("${nb69.site-url:https://nb69.no}") String site){this.jdbc=jdbc;this.sender=sender;this.encoder=encoder;this.clock=clock;this.site=site.replaceAll("/+$","");}
    @Transactional
    public void request(String username){
        if(username==null || username.length()>32 || !sender.ready())return;
        var users=jdbc.query("select password_hash from nb69_users where username=? for update",(rs,i)->rs.getString(1),username);
        if(users.isEmpty() || users.get(0)==null)return;
        var contacts=jdbc.query("select email from nb69_email_contacts where username=? and verified_at is not null",(rs,i)->rs.getString(1),username);
        if(contacts.isEmpty())return;
        var last=jdbc.query("select requested_at from nb69_password_resets where username=?",(rs,i)->rs.getObject(1,OffsetDateTime.class),username);
        if(!last.isEmpty() && last.get(0).toInstant().plusSeconds(300).isAfter(clock.instant()))return;
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        jdbc.update("delete from nb69_password_resets where username=?",username);
        jdbc.update("insert into nb69_password_resets (username,token_hash,email,password_snapshot,expires_at,requested_at) values (?,?,?,?,?,?)",username,HouseService.digest(token),contacts.get(0),users.get(0),OffsetDateTime.now(clock).plusMinutes(30),OffsetDateTime.now(clock));
        try{sender.send(contacts.get(0),"Sett nytt passord for NB69","Hei!\n\nDu kan sette nytt passord her:\n"+site+"/#reset-password="+token+"\n\nLenken gjelder i 30 minutter og kan brukes én gang. Hvis du ikke ba om dette, trenger du ikke gjøre noe. Passordet ditt er uendret til du lagrer et nytt.","reset/"+HouseService.digest(token));}
        catch(Exception error){if(error instanceof InterruptedException)Thread.currentThread().interrupt(); /* Delivery may have succeeded; keep the token and cooldown. */}
    }
    @Transactional
    public String confirm(String token,String password){
        if(token==null || !token.matches("[A-Za-z0-9_-]{43}"))throw invalid();
        if(password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(BAD_REQUEST,"Passordet må ha minst 12 tegn og maksimalt 72 byte.");
        var usernames=jdbc.query("select username from nb69_password_resets where token_hash=?",(rs,i)->rs.getString(1),HouseService.digest(token));
        if(usernames.isEmpty())throw invalid();String username=usernames.get(0);
        jdbc.queryForObject("select username from nb69_users where username=? for update",String.class,username);
        // Recheck under the account lock: a changed password or email invalidates the link.
        int valid=jdbc.queryForObject("select count(*) from nb69_password_resets r join nb69_users u on u.username=r.username join nb69_email_contacts e on e.username=r.username where r.username=? and r.token_hash=? and r.expires_at>? and r.password_snapshot=u.password_hash and r.email=e.email and e.verified_at is not null",Integer.class,username,HouseService.digest(token),OffsetDateTime.now(clock));
        if(valid!=1)throw invalid();
        jdbc.update("update nb69_users set password_hash=?,activation_hash=null,activation_expires=null where username=?",encoder.encode(password),username);
        jdbc.update("update nb69_password_resets set token_hash=null,password_snapshot=null where username=?",username);
        jdbc.update("insert into nb69_audit (actor,action,detail,recorded_at) values (?,'PASSWORD_RESET',?,?)",username,username,OffsetDateTime.now(clock));
        return username;
    }
    private ResponseStatusException invalid(){return new ResponseStatusException(BAD_REQUEST,"Lenken er ugyldig eller utløpt. Be om en ny passordlenke.");}
}
