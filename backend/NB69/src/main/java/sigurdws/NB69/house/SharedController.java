package sigurdws.NB69.house;

import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/shared")
public class SharedController {
    private final JdbcTemplate jdbc;
    private final HouseService house;
    private final Clock clock;
    public SharedController(JdbcTemplate jdbc, HouseService house, Clock clock) {this.jdbc=jdbc;this.house=house;this.clock=clock;}
    record SwapRequest(LocalDate awayWeek, int awayTask, LocalDate returnWeek, int returnTask) {}
    record Decision(String action) {}
    record ItemInput(String name) {}
    record Bought(boolean bought) {}
    record Swap(long id, String requester, String recipient, String requesterName, String recipientName, LocalDate awayWeek, String awayTask, LocalDate returnWeek, String returnTask, String status) {}
    record Item(long id, String name, String addedBy, String addedName, String boughtBy, String boughtName) {}
    private void lockHouse() {jdbc.queryForObject("select username from nb69_users where username='eilif' for update",String.class);}
    private ResponseStatusException fail(HttpStatus status,String message){return new ResponseStatusException(status,message);}
    private void validWeek(LocalDate week) {
        if(week==null || week.getDayOfWeek()!=DayOfWeek.MONDAY || week.isBefore(house.currentStart()) || week.isAfter(house.currentStart().plusWeeks(7)))
            throw fail(BAD_REQUEST,"Velg en uke fra denne uka og sju uker framover.");
    }
    @GetMapping("/weeks") public List<HouseService.Week> weeks(){return house.plannedWeeks();}
    @GetMapping("/swaps")
    public List<Swap> swaps(Principal user){
        return jdbc.query("select s.*,u.display_name as requester_name,v.display_name as recipient_name,a.task_name as away_name,b.task_name as return_name from nb69_swaps s join nb69_users u on u.username=s.requester join nb69_users v on v.username=s.recipient join nb69_assignments a on a.week_start=s.away_week and a.task_id=s.away_task join nb69_assignments b on b.week_start=s.return_week and b.task_id=s.return_task where (s.requester=? or s.recipient=?) and (s.status='PENDING' or s.away_week>=?) order by s.id desc",(rs,i)->new Swap(rs.getLong("id"),rs.getString("requester"),rs.getString("recipient"),rs.getString("requester_name"),rs.getString("recipient_name"),rs.getObject("away_week",LocalDate.class),rs.getString("away_name"),rs.getObject("return_week",LocalDate.class),rs.getString("return_name"),rs.getString("status").equals("PENDING") && (rs.getObject("away_week",LocalDate.class).isBefore(house.currentStart()) || rs.getObject("return_week",LocalDate.class).isBefore(house.currentStart())) ? "EXPIRED" : rs.getString("status")),user.getName(),user.getName(),house.currentStart().minusWeeks(4));
    }
    @PostMapping("/swaps") @ResponseStatus(NO_CONTENT) @Transactional
    public void requestSwap(@RequestBody SwapRequest body,Principal user){
        validWeek(body.awayWeek());validWeek(body.returnWeek());
        if(!body.returnWeek().isAfter(body.awayWeek())) throw fail(BAD_REQUEST,"Velg en senere uke der du tar oppgaven tilbake.");
        lockHouse();
        var away=assignment(body.awayWeek(),body.awayTask());var back=assignment(body.returnWeek(),body.returnTask());
        if(!away.get("username").equals(user.getName())) throw fail(FORBIDDEN,"Du kan bare bytte bort din egen oppgave.");
        if(back.get("username").equals(user.getName())) throw fail(BAD_REQUEST,"Velg en annen beboers oppgave som gjenytelse.");
        if(away.get("completed_at")!=null || back.get("completed_at")!=null) throw fail(CONFLICT,"En av oppgavene er allerede utført.");
        int pending=jdbc.queryForObject("select count(*) from nb69_swaps where status='PENDING' and away_week>=? and return_week>=? and ((away_week=? and away_task=?) or (return_week=? and return_task=?) or (away_week=? and away_task=?) or (return_week=? and return_task=?))",Integer.class,house.currentStart(),house.currentStart(),body.awayWeek(),body.awayTask(),body.awayWeek(),body.awayTask(),body.returnWeek(),body.returnTask(),body.returnWeek(),body.returnTask());
        if(pending>0) throw fail(CONFLICT,"En av oppgavene har allerede en åpen bytteforespørsel.");
        jdbc.update("insert into nb69_swaps (requester,recipient,away_week,away_task,return_week,return_task,status) values (?,?,?,?,?,?,'PENDING')",user.getName(),back.get("username"),body.awayWeek(),body.awayTask(),body.returnWeek(),body.returnTask());
    }
    private Map<String,Object> assignment(LocalDate week,int task){
        var rows=jdbc.queryForList("select username,completed_at from nb69_assignments where week_start=? and task_id=? for update",week,task);
        if(rows.isEmpty())throw fail(NOT_FOUND,"Oppgaven finnes ikke. Oppdater siden.");
        return rows.get(0);
    }
    @PostMapping("/swaps/{id}/decision") @Transactional
    public Map<String,String> decide(@PathVariable long id,@RequestBody Decision body,Principal user){
        lockHouse();
        var rows=jdbc.queryForList("select * from nb69_swaps where id=? for update",id);
        if(rows.isEmpty())throw fail(NOT_FOUND,"Forespørselen finnes ikke.");
        var swap=rows.get(0);String actor=user.getName();
        boolean cancel="CANCEL".equals(body.action());
        if(!List.of("ACCEPT","DECLINE","CANCEL").contains(Objects.toString(body.action(),"")))throw fail(BAD_REQUEST,"Ukjent handling.");
        if(!(cancel?swap.get("requester"):swap.get("recipient")).equals(actor))throw fail(FORBIDDEN,"Du kan ikke svare på denne forespørselen.");
        if(!swap.get("status").equals("PENDING"))throw fail(CONFLICT,"Forespørselen er allerede behandlet.");
        String status=cancel?"CANCELLED":"DECLINED";
        if("ACCEPT".equals(body.action())){
            LocalDate away=((java.sql.Date)swap.get("away_week")).toLocalDate(),back=((java.sql.Date)swap.get("return_week")).toLocalDate();
            int a=((Number)swap.get("away_task")).intValue(),b=((Number)swap.get("return_task")).intValue();
            var first=assignment(away,a);var second=assignment(back,b);
            if(away.isBefore(house.currentStart()) || back.isBefore(house.currentStart()) || first.get("completed_at")!=null || second.get("completed_at")!=null || !first.get("username").equals(swap.get("requester")) || !second.get("username").equals(swap.get("recipient"))) status="EXPIRED";
            else{
                jdbc.update("update nb69_assignments set username=?,rotation_version=1 where week_start=? and task_id=?",swap.get("recipient"),away,a);
                jdbc.update("update nb69_assignments set username=?,rotation_version=1 where week_start=? and task_id=?",swap.get("requester"),back,b);
                jdbc.update("insert into nb69_audit (actor,action,detail,recorded_at) values (?,'SWAP',?,?)",actor,"Swap " + id,OffsetDateTime.now(clock));
                status="ACCEPTED";
            }
        }
        jdbc.update("update nb69_swaps set status=? where id=?",status,id);
        return Map.of("status",status);
    }
    @GetMapping("/shopping")
    public List<Item> shopping(){return jdbc.query("select i.*,u.display_name as added_name,v.display_name as bought_name from nb69_shopping i join nb69_users u on u.username=i.added_by left join nb69_users v on v.username=i.bought_by order by case when i.bought_by is null then 0 else 1 end,i.id desc limit 200",(rs,i)->new Item(rs.getLong("id"),rs.getString("name"),rs.getString("added_by"),rs.getString("added_name"),rs.getString("bought_by"),rs.getString("bought_name")));}
    @PostMapping("/shopping") @ResponseStatus(NO_CONTENT) @Transactional
    public void add(@RequestBody ItemInput body,Principal user){
        String name=body.name()==null?"":body.name().strip();
        if(name.isEmpty() || name.length()>120)throw fail(BAD_REQUEST,"Skriv et varenavn på 1–120 tegn.");
        lockHouse();
        if(jdbc.queryForObject("select count(*) from nb69_shopping where bought_by is null",Integer.class)>=100)throw fail(CONFLICT,"Handlelisten er full. Kryss av varer som er kjøpt først.");
        jdbc.update("insert into nb69_shopping (name,added_by) values (?,?)",name,user.getName());
    }
    @PutMapping("/shopping/{id}") @ResponseStatus(NO_CONTENT)
    public void bought(@PathVariable long id,@RequestBody Bought body,Principal user){
        int changed=jdbc.update("update nb69_shopping set bought_by=? where id=? and " +(body.bought()?"bought_by is null":"bought_by is not null"),body.bought()?user.getName():null,id);
        if(changed==0 && jdbc.queryForObject("select count(*) from nb69_shopping where id=?",Integer.class,id)==0)throw fail(NOT_FOUND,"Varen finnes ikke.");
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(error.getReason(),"Handlingen kunne ikke utføres.")));}
}
