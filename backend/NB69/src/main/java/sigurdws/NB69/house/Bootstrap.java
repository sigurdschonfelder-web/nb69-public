package sigurdws.NB69.house;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Bootstrap implements ApplicationRunner {
    private final HouseService house;
    private final boolean local;
    private final String code;
    public Bootstrap(HouseService house, @Value("${nb69.local:false}") boolean local, @Value("${nb69.bootstrap-code:}") String code) {
        this.house=house; this.local=local; this.code=code;
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        house.seedUsers();
        if (local) {
            var lines = new ArrayList<String>();
            lines.add("# NB69 – lokale aktiveringskoder\n\nKun for test på denne maskinen. Koder gjelder i 24 timer.\nVelg «Ny bruker eller glemt passord?» på innloggingssiden.\n");
            for (var user: house.users()) if (!user.active()) lines.add(user.name() + ": `" + house.invite(user.username(), "local-setup") + "`\n");
            if (lines.size() > 1) {
                Path folder = Path.of(".local"); Files.createDirectories(folder);
                Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("rwx------"));
                Path file = folder.resolve("activation-codes.md");
                if (!Files.exists(file)) Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                Files.write(file, lines);
            }
        } else if (!house.user("eilif").active()) {
            if (code.length() < 24) throw new IllegalStateException("Set NB69_BOOTSTRAP_CODE to a random activation code of at least 24 characters for initial setup.");
            house.setInvite("eilif", code);
        }
    }
}
