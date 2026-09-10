package sigurdws.NB69.house;

import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Flow;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResendEmailSenderTest {
    @Test void sendsSingleRecipientTextAndStableIdempotencyKey() throws Exception {
        HttpClient http=mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"id\":\"49a3999c-0ce1-4ea6-ab68-afcd6dc2e794\"}");
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        var mapper=new ObjectMapper();
        var sender=new ResendEmailSender(true,"test-key","NB69 <test@example.com>",mapper,http);
        assertEquals("49a3999c-0ce1-4ea6-ab68-afcd6dc2e794",sender.send("person@example.com","Ukens ansvar","Hei Jørgen!","nb69/2026-09-07/2/SUNDAY"));
        var capture=ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(capture.capture(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        var request=capture.getValue();
        assertEquals("https://api.resend.com/emails",request.uri().toString());
        assertEquals("Bearer test-key",request.headers().firstValue("Authorization").orElseThrow());
        assertEquals("nb69/2026-09-07/2/SUNDAY",request.headers().firstValue("Idempotency-Key").orElseThrow());
        var bytes=new ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription){subscription.request(Long.MAX_VALUE);}
            public void onNext(ByteBuffer buffer){byte[] data=new byte[buffer.remaining()];buffer.get(data);bytes.writeBytes(data);}
            public void onError(Throwable error){throw new AssertionError(error);}
            public void onComplete(){}
        });
        var body=mapper.readTree(bytes.toString(StandardCharsets.UTF_8));
        assertEquals(1,body.get("to").size());
        assertEquals("person@example.com",body.get("to").get(0).asText());
        assertEquals("Hei Jørgen!",body.get("text").asText());
    }
    @Test void disabledSenderNeverCallsProvider() {
        HttpClient http=mock(HttpClient.class);
        var sender=new ResendEmailSender(false,"test-key","test@example.com",new ObjectMapper(),http);
        assertFalse(sender.ready());
        assertThrows(IllegalStateException.class,()->sender.send("person@example.com","Subject","Text","key"));
        verifyNoInteractions(http);
    }
    @Test void providerRejectionIsNotTreatedAsSuccessfulDelivery() throws Exception {
        HttpClient http=mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(422);
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        var sender=new ResendEmailSender(true,"test-key","test@example.com",new ObjectMapper(),http);
        assertThrows(IllegalStateException.class,()->sender.send("person@example.com","Subject","Text","key"));
    }
}
