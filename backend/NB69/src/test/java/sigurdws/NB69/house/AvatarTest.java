package sigurdws.NB69.house;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:avatartest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","nb69.bootstrap-code=test-bootstrap-code-with-at-least-24-characters"})
@AutoConfigureMockMvc
class AvatarTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void clean(){jdbc.update("delete from nb69_avatars");}
    MockMultipartFile picture() throws Exception{
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(500,300,BufferedImage.TYPE_INT_RGB),"png",out);
        return new MockMultipartFile("image","test.png","image/png",out.toByteArray());
    }
    @Test void uploadProducesPersistentSquareImageAndProfileVersion() throws Exception {
        mvc.perform(multipart("/api/profile/avatar").file(picture()).with(user("eilif")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.version").isString());
        mvc.perform(get("/api/me").with(user("eilif"))).andExpect(jsonPath("$.avatarVersion").isString());
        var result=mvc.perform(get("/api/avatars/eilif").with(user("andreas"))).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("image/jpeg")).andReturn();
        var stored=ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));assertEquals(256,stored.getWidth());assertEquals(256,stored.getHeight());
        assertEquals(1,jdbc.queryForObject("select count(*) from nb69_avatars",Integer.class));
    }
    @Test void identityCannotBeOverriddenAndDeletionOnlyAffectsOwnPhoto() throws Exception {
        mvc.perform(multipart("/api/profile/avatar").file(picture()).param("username","sigurd").with(user("eilif")).with(csrf())).andExpect(status().isOk());
        mvc.perform(delete("/api/profile/avatar").with(user("sigurd")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/avatars/eilif").with(user("eilif"))).andExpect(status().isOk());
        mvc.perform(delete("/api/profile/avatar").with(user("eilif")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/avatars/eilif").with(user("eilif"))).andExpect(status().isNotFound());
    }
    @Test void imagesRequireLoginCsrfAndRealImageContent() throws Exception {
        mvc.perform(get("/api/avatars/eilif")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/profile/avatar").file(picture()).with(user("eilif"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/profile/avatar").file(new MockMultipartFile("image","fake.jpg","image/jpeg","not an image".getBytes())).with(user("eilif")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/profile/avatar").file(new MockMultipartFile("image","big.jpg","image/jpeg",new byte[1000001])).with(user("eilif")).with(csrf())).andExpect(status().isBadRequest());
    }
}
