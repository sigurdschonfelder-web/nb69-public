error id: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java:java/nio/file/Files#exists().
file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java
empty definition using pc, found symbol in pc: java/nio/file/Files#exists().
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 597
uri: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java
text:
```scala
package sigurdws.NB69.prikker;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class PrikkManager {

    private static List<PrikkRow> getPrikker() {

        final ObjectMapper objectMapper = new ObjectMapper();
        final Path jsonPath = Path.of("backend/NB69/src/main/resources/prikker.json");

        final static List<PrikkRow> readPrikker() = {
        try {
            if (!Files.ex@@ists(jsonPath)) {
                return new ArrayList<>(); // eller kast exception hvis du forventer at fila finnes
            }

            String json = Files.readString(jsonPath);
            if (json.isBlank()) return new ArrayList<>();

            return objectMapper.readValue(json, new TypeReference<List<PrikkRow>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lese " + jsonPath.toAbsolutePath(), e);
        }
    }
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: java/nio/file/Files#exists().