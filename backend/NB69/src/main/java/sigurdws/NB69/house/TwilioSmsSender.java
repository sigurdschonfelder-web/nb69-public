package sigurdws.NB69.house;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwilioSmsSender implements SmsSender {
    private final String account, token, from;
    private final boolean enabled;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public TwilioSmsSender(@Value("${nb69.sms.enabled:false}") boolean enabled,
            @Value("${nb69.sms.account:}") String account, @Value("${nb69.sms.token:}") String token,
            @Value("${nb69.sms.from:}") String from, ObjectMapper mapper) {
        this.enabled=enabled; this.account=account; this.token=token; this.from=from; this.mapper=mapper;
    }
    public boolean ready() { return enabled && account.matches("AC[0-9a-fA-F]{32}") && !token.isBlank() && !from.isBlank(); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    public String send(String phone, String message) throws Exception {
        if (!ready()) throw new IllegalStateException("SMS is not configured");
        var request=HttpRequest.newBuilder(URI.create("https://api.twilio.com/2010-04-01/Accounts/"+account+"/Messages.json"))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((account+":"+token).getBytes(StandardCharsets.UTF_8)))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("To="+encode(phone)+"&From="+encode(from)+"&Body="+encode(message))).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        if (response.statusCode()!=201) throw new IllegalStateException("SMS provider rejected the request");
        String sid=mapper.readTree(response.body()).path("sid").asText();
        if (!sid.matches("SM[0-9a-fA-F]{32}")) throw new IllegalStateException("Unknown SMS provider response");
        return sid; // accepted by provider, not proof of delivery to the handset
    }
}
