package sigurdws.NB69.house;

import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:nb69test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa", "spring.datasource.password=",
 "nb69.bootstrap-code=test-bootstrap-code-with-at-least-24-characters"
})
@AutoConfigureMockMvc
class HouseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HouseService house;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @BeforeEach void clean() {
        jdbc.update("delete from nb69_email_reminders"); jdbc.update("delete from nb69_email_contacts");
        jdbc.update("delete from nb69_assignments"); jdbc.update("delete from nb69_audit");
        jdbc.update("update nb69_users set password_hash=null, activation_hash=null, activation_expires=null");
    }
    @Test void anonymousCannotReadAndResidentsCannotAdminOrUseOldPrikker() throws Exception {
        mvc.perform(get("/api/weeks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").with(user("andreas").roles("RESIDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(user("eilif").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(put("/api/prikker/update").with(user("eilif").roles("ADMIN")).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        assertTrue(house.user("eilif").admin()); assertTrue(house.user("sigurd").admin()); assertFalse(house.user("andreas").admin());
    }
    @Test void activationIsOneUseAndLoginUsesPersonalPassword() throws Exception {
        String code = house.invite("andreas", "eilif");
        String body = mapper.writeValueAsString(Map.of("username","andreas","code",code,"password","A-long-test-password!"));
        mvc.perform(post("/api/activate").contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/activate").with(csrf()).contentType("application/json").content(body)).andExpect(status().isNoContent());
        mvc.perform(post("/api/activate").with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        var login = mvc.perform(post("/api/login").with(csrf()).param("username","andreas").param("password","A-long-test-password!"))
            .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Andreas")).andExpect(jsonPath("$.admin").value(false));
        mvc.perform(post("/api/login").with(csrf()).param("username","andreas").param("password","wrong")).andExpect(status().isUnauthorized());
        String resetCode = house.invite("andreas", "eilif");
        mvc.perform(post("/api/activate").with(csrf()).contentType("application/json").content(mapper.writeValueAsString(Map.of("username","andreas","code",resetCode,"password","Another-long-password!")))).andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void completionIsOwnerOnlyIdempotentAndRejectsFutureWeeks() throws Exception {
        var week = house.weeks().get(0); var task = week.assignments().get(0);
        String route = "/api/weeks/"+week.start()+"/tasks/"+task.id()+"/completion";
        String other = task.username().equals("eilif") ? "sigurd" : "eilif";
        mvc.perform(post(route).with(user(other).roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(route).with(user(task.username()))).andExpect(status().isForbidden());
        mvc.perform(post(route).with(user(task.username())).with(csrf())).andExpect(status().isNoContent());
        var first = house.weeks().get(0).assignments().get(0).completedAt(); assertNotNull(first);
        mvc.perform(post(route).with(user(task.username())).with(csrf())).andExpect(status().isNoContent());
        assertEquals(first, house.weeks().get(0).assignments().get(0).completedAt());
        mvc.perform(post("/api/weeks/"+week.start().plusWeeks(1)+"/tasks/0/completion").with(user(task.username())).with(csrf())).andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("select count(*) from nb69_audit where action='COMPLETE'",Integer.class));
    }
    @Test void adminReassignmentPersistsAndCompletedTaskMustBeUndoneFirst() throws Exception {
        var week=house.weeks().get(0); var task=week.assignments().get(0);
        String path="/api/admin/weeks/"+week.start()+"/tasks/0";
        house.complete(week.start(),0,task.username());
        mvc.perform(put(path).with(user("eilif").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"username\":\"sigurd\"}"))
            .andExpect(status().isConflict());
        mvc.perform(delete(path+"/completion").with(user("eilif").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(put(path).with(user("eilif").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"username\":\"sigurd\"}"))
            .andExpect(status().isNoContent());
        assertEquals("sigurd",house.weeks().get(0).assignments().get(0).username());
    }
    @Test void expiredCodeCannotActivateAccount() throws Exception {
        String code=house.invite("erlend","eilif");
        jdbc.update("update nb69_users set activation_expires=? where username='erlend'",OffsetDateTime.now().minusDays(1));
        mvc.perform(post("/api/activate").with(csrf()).contentType("application/json").content(mapper.writeValueAsString(Map.of("username","erlend","code",code,"password","A-long-test-password!"))))
            .andExpect(status().isBadRequest());
    }
    @Test void browserCsrfCookieFlowWorksThroughLoginAndLogout() throws Exception {
        String code=house.invite("sigurd","eilif");
        house.activate("sigurd",code,"Browser-test-password!");
        var first=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession) first.getRequest().getSession(false);
        var token=mapper.readTree(first.getResponse().getContentAsString());
        var signedIn=mvc.perform(post("/api/login").session(session)
            .header(token.get("headerName").asText(),token.get("token").asText())
            .param("username","sigurd").param("password","Browser-test-password!"))
            .andExpect(status().isNoContent()).andReturn();
        session=(MockHttpSession) signedIn.getRequest().getSession(false);
        var fresh=mvc.perform(get("/api/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var freshToken=mapper.readTree(fresh.getResponse().getContentAsString());
        mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/logout").session(session)
            .header(freshToken.get("headerName").asText(),freshToken.get("token").asText()))
            .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
    }
    @Test void newIsoYearGetsIndependentAssignmentsAndCompletions() {
        var encoder=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        var december=new HouseService(jdbc,encoder,Clock.fixed(Instant.parse("2026-12-31T12:00:00Z"),ZoneId.of("Europe/Oslo")));
        var old=december.weeks().get(0);
        assertEquals(53,old.week());
        december.complete(old.start(),0,old.assignments().get(0).username());
        var january=new HouseService(jdbc,encoder,Clock.fixed(Instant.parse("2027-01-04T12:00:00Z"),ZoneId.of("Europe/Oslo")));
        var next=january.weeks().get(0);
        assertEquals(1,next.week());
        assertEquals(LocalDate.of(2027,1,4),next.start());
        assertTrue(next.assignments().stream().allMatch(task -> task.completedAt()==null));
        assertNotNull(december.weeks().get(0).assignments().get(0).completedAt());
    }

    private HouseService at(String time) {
        return new HouseService(jdbc,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(),Clock.fixed(Instant.parse(time),HouseService.OSLO));
    }
    @Test void sundayReminderAndMidnightDeadlineUseOsloTime() {
        var before=at("2026-09-13T11:59:59Z").weeks().get(0);
        assertFalse(before.reminderDue());
        assertTrue(at("2026-09-13T12:00:00Z").weeks().get(0).reminderDue());
        assertEquals(Instant.parse("2026-09-13T22:00:00Z"),before.dueAt());
        var sunday=at("2026-09-13T21:59:59Z");
        var task=sunday.weeks().get(0).assignments().get(0);
        sunday.complete(before.start(),task.id(),task.username());
        assertFalse(sunday.weeks().get(0).assignments().get(0).late());
        var monday=at("2026-09-13T22:00:00Z");
        var remaining=before.assignments().get(1);
        assertEquals(1,monday.dashboard(remaining.username()).overdue().size());
        monday.complete(before.start(),remaining.id(),remaining.username());
        assertTrue(monday.dashboard(remaining.username()).overdue().isEmpty());
        assertTrue(sunday.weeks().get(0).assignments().get(1).late());
    }
    @Test void deadlinesAndEmailWindowsFollowDaylightSavingAndYearBoundaries() {
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"),HouseService.deadline(LocalDate.of(2026,3,23)));
        assertEquals(Instant.parse("2026-10-25T23:00:00Z"),HouseService.deadline(LocalDate.of(2026,10,19)));
        assertNull(EmailReminders.window(Instant.parse("2026-10-25T12:59:59Z")));
        assertEquals("SUNDAY",EmailReminders.window(Instant.parse("2026-10-25T13:00:00Z")).kind());
        assertNull(EmailReminders.window(Instant.parse("2026-10-26T06:59:59Z")));
        assertEquals("MONDAY",EmailReminders.window(Instant.parse("2026-10-26T07:00:00Z")).kind());
        assertEquals(LocalDate.of(2026,12,28),EmailReminders.window(Instant.parse("2027-01-04T07:00:00Z")).start());
        assertNull(EmailReminders.window(Instant.parse("2026-10-27T07:00:00Z")));
    }
    @Test void overdueApiIsPersonalAndLateCompletionStillRequiresOwner() throws Exception {
        var start=house.currentStart().minusWeeks(1);
        jdbc.update("insert into nb69_assignments (week_start,task_id,task_name,username) values (?,0,'Kjøkken','andreas')",start);
        mvc.perform(get("/api/dashboard").with(user("andreas")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.overdue.length()").value(1));
        mvc.perform(get("/api/dashboard").with(user("sigurd")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.overdue[?(@.id == 0)]").isEmpty());
        mvc.perform(post("/api/weeks/"+start+"/tasks/0/completion").with(user("sigurd").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/weeks/"+start+"/tasks/0/completion").with(user("andreas")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/dashboard").with(user("andreas"))).andExpect(jsonPath("$.overdue[?(@.id == 0)]").isEmpty());
        mvc.perform(get("/api/admin/email").with(user("andreas"))).andExpect(status().isForbidden());
    }
    @Test void missedWeeksArePreservedWithoutCreatingDebtBeforeFirstUse() {
        var first=at("2026-09-07T10:00:00Z");
        assertTrue(first.dashboard("eilif").overdue().isEmpty());
        var later=at("2026-09-28T10:00:00Z");
        assertEquals(3,later.dashboard("eilif").overdue().size());
    }

    @Test void newRotationFollowsRequestedOrderAcrossYearBoundaries() {
        var expected=java.util.List.of("andreas","sigurd","jorgen","eilif","erlend");
        for (int week=0; week<75; week++) {
            var start=HouseService.ROTATION_START.plusWeeks(week);
            var assigned=new java.util.HashSet<String>();
            for (int task=0;task<5;task++) {
                String person=HouseService.scheduledPerson(start,task);
                assigned.add(person);
                assertEquals(expected.get((expected.indexOf(person)+1)%5),HouseService.scheduledPerson(start.plusWeeks(1),task));
            }
            assertEquals(5,assigned.size());
        }
        assertEquals("sigurd",HouseService.scheduledPerson(HouseService.ROTATION_START,0));
        assertTrue(house.user("eilif").admin());
        assertTrue(house.user("sigurd").admin());
        assertFalse(house.user("andreas").admin());
    }
    @Test void alreadyGeneratedNextWeekUsesNewRotationButKeepsManualAssignments() {
        var start=HouseService.ROTATION_START;
        jdbc.update("insert into nb69_assignments (week_start,task_id,task_name,username) values (?,0,'Kjøkken','jorgen')",start);
        jdbc.update("insert into nb69_assignments (week_start,task_id,task_name,username) values (?,1,'Stue','andreas')",start);
        var before=at("2026-09-11T10:00:00Z");
        before.assign(start,1,"erlend","eilif");
        var current=before.weeks().get(0);
        var next=before.weeks().get(1);
        assertEquals("sigurd",next.assignments().get(0).username());
        assertEquals("erlend",next.assignments().get(1).username());
        assertEquals(current,before.weeks().get(0));
        before.assign(start,0,"andreas","sigurd");
        assertEquals("andreas",before.weeks().get(1).assignments().get(0).username());
    }

    @Test void commentsAreOwnerOnlyBoundedIdempotentAndClearedOnUndo() throws Exception {
        var week=house.weeks().get(0); var task=week.assignments().get(0);
        String route="/api/weeks/"+week.start()+"/tasks/0/completion";
        String other=task.username().equals("eilif") ? "sigurd" : "eilif";
        mvc.perform(get("/api/history")).andExpect(status().isUnauthorized());
        mvc.perform(post(route).with(user(other)).with(csrf()).contentType("application/json").content("{\"comment\":\"Feil person\"}")).andExpect(status().isForbidden());
        mvc.perform(post(route).with(user(task.username())).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(Map.of("comment","x".repeat(501))))).andExpect(status().isBadRequest());
        assertNull(house.weeks().get(0).assignments().get(0).completedAt());
        mvc.perform(post(route).with(user(task.username())).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(Map.of("comment","  Tomt for såpe 🧼  ")))).andExpect(status().isNoContent());
        mvc.perform(post(route).with(user(task.username())).with(csrf()).contentType("application/json").content("{\"comment\":\"Overskrevet\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/history").with(user(other))).andExpect(status().isOk()).andExpect(jsonPath("$[0].assignments[0].comment").value("Tomt for såpe 🧼"));
        house.undo(week.start(),0,"eilif");
        assertNull(house.history().get(0).assignments().get(0).comment());
        assertNull(house.history().get(0).assignments().get(0).completedAt());
    }
    @Test void historyDoesNotGenerateWeeksAndIncludesLateCompletionsAcrossYears() {
        var december=at("2026-12-31T12:00:00Z");
        assertTrue(december.history().isEmpty());
        var old=december.weeks().get(0);var task=old.assignments().get(0);
        var january=at("2027-01-04T12:00:00Z");
        january.weeks();
        january.complete(old.start(),task.id(),task.username(),"Ferdig mandag");
        var history=january.history();
        assertEquals(2,history.size());
        assertEquals(LocalDate.of(2027,1,4),history.get(0).start());
        assertEquals(old.start(),history.get(1).start());
        assertTrue(history.get(1).assignments().get(0).late());
        assertEquals("Ferdig mandag",history.get(1).assignments().get(0).comment());
        assertTrue(at("2027-04-05T12:00:00Z").history().isEmpty());
    }

}
