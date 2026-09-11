package sigurdws.NB69.house;

import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:profiletest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","nb69.bootstrap-code=test-bootstrap-code-with-at-least-24-characters"})
@AutoConfigureMockMvc
class EmailProfileTest {
    @Autowired EmailProfile profile;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmailReminders reminders;
    @Autowired MockMvc mvc;
    @MockitoBean EmailSender sender;
    @BeforeEach void setup() throws Exception {
        jdbc.update("delete from nb69_email_contacts");jdbc.update("delete from nb69_email_verification_limits");
        when(sender.ready()).thenReturn(true);when(sender.send(anyString(),anyString(),anyString(),anyString())).thenReturn("test-id");
    }
    String sentToken() throws Exception {
        var text=ArgumentCaptor.forClass(String.class);
        verify(sender).send(anyString(),anyString(),text.capture(),anyString());
        return text.getValue().split("#confirm-email=")[1].split("\\s")[0];
    }
    @Test void linkIsOneUseBoundToResidentAndStoredOnlyAsHash() throws Exception {
        var result=profile.save("eilif","eilif@example.com");
        assertFalse(result.profile().verified());assertNull(result.previewLink());
        String token=sentToken();
        assertNotEquals(token,jdbc.queryForObject("select token_hash from nb69_email_contacts where username='eilif'",String.class));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->profile.confirm("sigurd",token));
        assertTrue(profile.confirm("eilif",token).verified());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->profile.confirm("eilif",token));
    }
    @Test void changingAddressRevokesVerificationAndOldLink() throws Exception {
        profile.save("eilif","first@example.com");String token=sentToken();profile.confirm("eilif",token);
        jdbc.update("delete from nb69_email_verification_limits");
        profile.save("eilif","second@example.com");
        assertFalse(profile.profile("eilif").verified());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->profile.confirm("eilif",token));
    }
    @Test void expiredTokensAndRapidResendsAreRejected() throws Exception {
        profile.save("eilif","eilif@example.com");String token=sentToken();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->profile.save("eilif","different@example.com"));
        jdbc.update("update nb69_email_contacts set token_expires=?",OffsetDateTime.now().minusDays(1));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->profile.confirm("eilif",token));
    }
    @Test void disabledServiceStoresAddressWithoutPretendingToSend() throws Exception {
        when(sender.ready()).thenReturn(false);
        var result=profile.save("eilif","eilif@example.com");
        assertFalse(result.profile().verified());assertNull(result.profile().expiresAt());assertNull(result.previewLink());
        verify(sender,never()).send(anyString(),anyString(),anyString(),anyString());
    }
    @Test void profileEndpointsRequireLoginAndCsrfAndIgnoreClaimedUsername() throws Exception {
        mvc.perform(get("/api/profile/email")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/profile/email").with(user("andreas")).contentType("application/json").content("{\"email\":\"andreas@example.com\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/profile/email").with(user("andreas")).with(csrf()).contentType("application/json").content("{\"email\":\"andreas@example.com\",\"username\":\"eilif\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.previewLink").isEmpty());
        assertEquals("andreas@example.com",profile.profile("andreas").email());assertEquals("",profile.profile("eilif").email());
        mvc.perform(put("/api/admin/email/andreas").with(user("eilif").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"email\":\"other@example.com\"}"))
            .andExpect(status().isNotFound());
    }
    @Test void localPreviewLinkWorksWithoutSendingEmail() throws Exception {
        var local=new EmailProfile(jdbc,reminders,sender,Clock.system(HouseService.OSLO),true,"http://127.0.0.1:5173");
        var result=local.save("eilif","local@example.com");
        assertTrue(result.previewLink().startsWith("http://127.0.0.1:5173/#confirm-email="));
        assertTrue(local.confirm("eilif",result.previewLink().split("#confirm-email=")[1]).verified());
        verify(sender,never()).send(anyString(),anyString(),anyString(),anyString());
    }
}
