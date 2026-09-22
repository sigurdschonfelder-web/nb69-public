package sigurdws.NB69.house;
import java.time.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:resettest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","nb69.bootstrap-code=test-bootstrap-code-with-at-least-24-characters"})
@AutoConfigureMockMvc
class PasswordResetTest {
 @Autowired PasswordReset reset;@Autowired HouseService house;@Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;@Autowired ObjectMapper mapper;
 @MockitoBean EmailSender sender;
 @BeforeEach void setup() throws Exception {
  jdbc.update("delete from nb69_password_resets");jdbc.update("delete from nb69_email_contacts");
  String code=house.invite("eilif","eilif");house.activate("eilif",code,"Old-test-password!");
  jdbc.update("insert into nb69_email_contacts (username,email,verified_at) values ('eilif','eilif@example.com',?)",OffsetDateTime.now());
  when(sender.ready()).thenReturn(true);when(sender.send(anyString(),anyString(),anyString(),anyString())).thenReturn("test");
 }
 String token() throws Exception {var text=ArgumentCaptor.forClass(String.class);verify(sender,atLeastOnce()).send(eq("eilif@example.com"),anyString(),text.capture(),anyString());return text.getValue().split("#reset-password=")[1].split("\\s")[0];}
 @Test void resetIsSingleUseAndInvalidatesExistingSessions() throws Exception {
  var login=mvc.perform(post("/api/login").with(csrf()).param("username","eilif").param("password","Old-test-password!")).andExpect(status().isNoContent()).andReturn();
  var session=(MockHttpSession)login.getRequest().getSession(false);
  reset.request("eilif");String token=token();assertNotEquals(token,jdbc.queryForObject("select token_hash from nb69_password_resets",String.class));
  String body=mapper.writeValueAsString(java.util.Map.of("token",token,"password","New-test-password!"));
  mvc.perform(post("/api/password-reset/confirm").contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post("/api/password-reset/confirm").with(csrf()).contentType("application/json").content(body)).andExpect(status().isNoContent());
  mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/password-reset/confirm").with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/login").with(csrf()).param("username","eilif").param("password","New-test-password!")).andExpect(status().isNoContent());
  mvc.perform(post("/api/login").with(csrf()).param("username","eilif").param("password","Old-test-password!")).andExpect(status().isUnauthorized());
 }
 @Test void requestsAreGenericRateLimitedAndNeverExposeToken() throws Exception {
  var first=mvc.perform(post("/api/password-reset/request").with(csrf()).contentType("application/json").content("{\"username\":\"eilif\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  var unknown=mvc.perform(post("/api/password-reset/request").with(csrf()).contentType("application/json").content("{\"username\":\"unknown\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertEquals(first,unknown);assertFalse(first.contains(token()));reset.request("eilif");verify(sender,times(1)).send(anyString(),anyString(),anyString(),anyString());
 }
 @Test void expiredChangedEmailAndChangedPasswordInvalidateTokens() throws Exception {
  reset.request("eilif");String token=token();jdbc.update("update nb69_password_resets set expires_at=?",OffsetDateTime.now().minusMinutes(1));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reset.confirm(token,"New-test-password!"));
  jdbc.update("update nb69_password_resets set expires_at=?",OffsetDateTime.now().plusMinutes(30));jdbc.update("update nb69_email_contacts set verified_at=null");
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reset.confirm(token,"New-test-password!"));
  jdbc.update("update nb69_email_contacts set verified_at=?",OffsetDateTime.now());house.activate("eilif",house.invite("eilif","eilif"),"Another-test-password!");
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reset.confirm(token,"New-test-password!"));
 }
 @Test void disabledOrUnverifiedAccountsDoNotSendAndBadPasswordsDoNotConsumeToken() throws Exception {
  when(sender.ready()).thenReturn(false);reset.request("eilif");verify(sender,never()).send(anyString(),anyString(),anyString(),anyString());
  when(sender.ready()).thenReturn(true);jdbc.update("update nb69_email_contacts set verified_at=null");reset.request("eilif");verify(sender,never()).send(anyString(),anyString(),anyString(),anyString());
  jdbc.update("update nb69_email_contacts set verified_at=?",OffsetDateTime.now());reset.request("eilif");String token=token();
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reset.confirm(token,"short"));assertEquals("eilif",reset.confirm(token,"Valid-test-password!"));
 }
}
