package sigurdws.NB69.house;

import java.time.*;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:sharedtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","nb69.bootstrap-code=test-bootstrap-code-with-at-least-24-characters"})
@AutoConfigureMockMvc
class SharedTest {
    @Autowired MockMvc mvc;
    @Autowired HouseService house;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @BeforeEach void clean(){jdbc.update("delete from nb69_swaps");jdbc.update("delete from nb69_shopping");jdbc.update("delete from nb69_assignments");jdbc.update("delete from nb69_audit");}
    long requestSwap() throws Exception {
        var weeks=house.plannedWeeks();var first=weeks.get(0).assignments().get(0);var second=weeks.get(1).assignments().stream().filter(t->!t.username().equals(first.username())).findFirst().orElseThrow();
        mvc.perform(post("/api/shared/swaps").with(user(first.username())).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(Map.of("awayWeek",weeks.get(0).start().toString(),"awayTask",first.id(),"returnWeek",weeks.get(1).start().toString(),"returnTask",second.id())))).andExpect(status().isNoContent());
        return jdbc.queryForObject("select max(id) from nb69_swaps",Long.class);
    }
    String owner(long id,String field){return jdbc.queryForObject("select "+field+" from nb69_swaps where id=?",String.class,id);}
    void decision(long id,String actor,String action,int status) throws Exception {
        mvc.perform(post("/api/shared/swaps/"+id+"/decision").with(user(actor)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(Map.of("action",action)))).andExpect(status().is(status));
    }
    @Test void acceptanceSwapsBothOwnersOnceAndRequiresRecipient() throws Exception {
        long id=requestSwap();String sender=owner(id,"requester"),recipient=owner(id,"recipient");
        assertEquals(sender,house.weeks().get(0).assignments().get(0).username());
        decision(id,sender,"ACCEPT",403);
        decision(id,recipient,"CANCEL",403);
        decision(id,recipient,"ACCEPT",200);
        assertEquals(recipient,house.weeks().get(0).assignments().get(0).username());
        int task=jdbc.queryForObject("select return_task from nb69_swaps where id=?",Integer.class,id);
        assertEquals(sender,house.weeks().get(1).assignments().get(task).username());
        decision(id,recipient,"ACCEPT",409);
        assertEquals(1,jdbc.queryForObject("select count(*) from nb69_audit where action='SWAP'",Integer.class));
    }
    @Test void completedOrReassignedTasksCannotBeSwappedAndDeclineChangesNoOwners() throws Exception {
        long id=requestSwap();String sender=owner(id,"requester"),recipient=owner(id,"recipient");
        house.complete(house.currentStart(),0,sender);
        decision(id,recipient,"ACCEPT",200);
        assertEquals("EXPIRED",owner(id,"status"));
        assertEquals(sender,house.weeks().get(0).assignments().get(0).username());
        house.undoOwn(house.currentStart(),0,sender);
        id=requestSwap();decision(id,recipient,"DECLINE",200);
        assertEquals("DECLINED",owner(id,"status"));
        assertEquals(sender,house.weeks().get(0).assignments().get(0).username());
        id=requestSwap();house.assign(house.currentStart(),0,recipient,"eilif");decision(id,recipient,"ACCEPT",200);
        assertEquals("EXPIRED",owner(id,"status"));
    }
    @Test void requestsArePrivateAndValidatedAndCancellationAllowsNewRequest() throws Exception {
        long id=requestSwap();String sender=owner(id,"requester"),recipient=owner(id,"recipient");
        String other=HouseService.PEOPLE.stream().filter(p->!p.equals(sender)&&!p.equals(recipient)).findFirst().orElseThrow();
        mvc.perform(get("/api/shared/swaps").with(user(other))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        var weeks=house.plannedWeeks();int task=jdbc.queryForObject("select return_task from nb69_swaps where id=?",Integer.class,id);
        String body=mapper.writeValueAsString(Map.of("awayWeek",weeks.get(0).start().toString(),"awayTask",0,"returnWeek",weeks.get(1).start().toString(),"returnTask",task));
        mvc.perform(post("/api/shared/swaps").with(user(other)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/shared/swaps").with(user(sender)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/shared/swaps").with(user(sender)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        decision(id,sender,"CANCEL",200);requestSwap();
        assertEquals(8,house.plannedWeeks().size());
    }
    @Test void shoppingIsSharedAuthenticatedValidatedAndCanBeUndone() throws Exception {
        mvc.perform(get("/api/shared/shopping")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/shared/shopping").with(user("eilif")).contentType("application/json").content("{\"name\":\"Såpe\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/shared/shopping").with(user("eilif")).with(csrf()).contentType("application/json").content("{\"name\":\"   \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/shared/shopping").with(user("eilif")).with(csrf()).contentType("application/json").content("{\"name\":\"Såpe\"}")).andExpect(status().isNoContent());
        long id=jdbc.queryForObject("select max(id) from nb69_shopping",Long.class);
        mvc.perform(put("/api/shared/shopping/"+id).with(user("sigurd")).with(csrf()).contentType("application/json").content("{\"bought\":true}")).andExpect(status().isNoContent());
        mvc.perform(put("/api/shared/shopping/"+id).with(user("andreas")).with(csrf()).contentType("application/json").content("{\"bought\":true}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/shared/shopping").with(user("jorgen"))).andExpect(jsonPath("$[0].boughtName").value("Sigurd")).andExpect(jsonPath("$[0].addedName").value("Eilif"));
        mvc.perform(put("/api/shared/shopping/"+id).with(user("sigurd")).with(csrf()).contentType("application/json").content("{\"bought\":false}")).andExpect(status().isNoContent());
        assertNull(jdbc.queryForObject("select bought_by from nb69_shopping where id=?",String.class,id));
    }
}
