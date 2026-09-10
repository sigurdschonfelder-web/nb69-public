package sigurdws.NB69.house;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResendEmailSender implements EmailSender {
    private final String token, from;
    private final boolean enabled;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private long nextRequestNanos;
    @Autowired
    public ResendEmailSender(@Value("${nb69.email.enabled:false}") boolean enabled,
            @Value("${nb69.email.token:}") String token, @Value("${nb69.email.from:}") String from,
            ObjectMapper mapper) {
        this(enabled,token,from,mapper,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }
    ResendEmailSender(boolean enabled,String token,String from,ObjectMapper mapper,HttpClient http) {
        this.enabled=enabled; this.token=token; this.from=from; this.mapper=mapper; this.http=http;
    }
    public boolean ready() { return enabled && !token.isBlank() && !from.isBlank(); }
    public synchronized String send(String email,String subject,String message,String key) throws Exception {
        if (!ready()) throw new IllegalStateException("Email is not configured");
        // Pace a batch of resident reminders instead of bursting at the provider.
        long wait=nextRequestNanos-System.nanoTime();
        if(wait>0) java.util.concurrent.TimeUnit.NANOSECONDS.sleep(wait);
        nextRequestNanos=System.nanoTime()+Duration.ofSeconds(1).toNanos();
        var request=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization","Bearer "+token)
            .header("Content-Type","application/json")
            .header("Idempotency-Key",key)
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of(
                "from",from,"to",List.of(email),"subject",subject,"text",message)))).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        if (response.statusCode()!=200) throw new IllegalStateException("Email provider rejected the request");
        String id=mapper.readTree(response.body()).path("id").asText();
        UUID.fromString(id); // reject malformed/uncertain responses, without logging email or credentials
        return id; // accepted by Resend, not proof of delivery to the recipient
    }
}
