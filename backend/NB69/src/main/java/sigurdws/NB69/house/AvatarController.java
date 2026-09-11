package sigurdws.NB69.house;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.Principal;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AvatarController {
    private final JdbcTemplate jdbc;
    public AvatarController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @PostMapping("/profile/avatar") @Transactional
    public Map<String,String> upload(Principal user,@RequestParam("image") MultipartFile file) throws IOException {
        if(file.isEmpty() || file.getSize()>1000000) throw badImage();
        byte[] jpeg;
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw badImage();
            var reader=readers.next();
            try {
                reader.setInput(input);
                String format=reader.getFormatName();
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(!(format.equalsIgnoreCase("JPEG") || format.equalsIgnoreCase("PNG")) || width<1 || height<1 || (long)width*height>20000000) throw badImage();
                var original=reader.read(0);
                var square=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
                var graphics=square.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);graphics.fillRect(0,0,256,256);
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int side=Math.min(width,height),x=(width-side)/2,y=(height-side)/2;
                    graphics.drawImage(original,0,0,256,256,x,y,x+side,y+side,null);
                } finally {graphics.dispose();}
                var output=new ByteArrayOutputStream();ImageIO.write(square,"jpeg",output);jpeg=output.toByteArray();
            } finally {reader.dispose();}
        } catch(javax.imageio.IIOException error) {throw badImage();}
        if(jpeg.length>200000) throw badImage();
        String version=UUID.randomUUID().toString();
        jdbc.queryForObject("select username from nb69_users where username=? for update",String.class,user.getName());
        jdbc.update("delete from nb69_avatars where username=?",user.getName());
        jdbc.update("insert into nb69_avatars (username,image_base64,version) values (?,?,?)",user.getName(),Base64.getEncoder().encodeToString(jpeg),version);
        return Map.of("version",version);
    }
    @GetMapping("/avatars/{username}")
    public ResponseEntity<byte[]> image(@PathVariable String username){
        var images=jdbc.query("select image_base64 from nb69_avatars where username=?",(rs,i)->rs.getString(1),username);
        if(images.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG).body(Base64.getDecoder().decode(images.get(0)));
    }
    @DeleteMapping("/profile/avatar") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal user){jdbc.update("delete from nb69_avatars where username=?",user.getName());}
    private ResponseStatusException badImage(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Velg et gyldig JPG- eller PNG-bilde. Bildet er for stort eller kunne ikke leses.");}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()));}
}
