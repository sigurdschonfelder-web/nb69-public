error id: file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java:java/lang/IllegalArgumentException#
file://<HOME>/KODE/NB69%20-%20App/backend/NB69/src/main/java/sigurdws/NB69/prikker/PrikkManager.java
empty definition using pc, found symbol in pc: java/lang/IllegalArgumentException#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 2137
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

    final ObjectMapper objectMapper = new ObjectMapper();
    final Path jsonPath = Path.of("<WORKSPACE>/backend/NB69/src/main/java/sigurdws/NB69/prikker/prikker.json");


    public List<PrikkRow> readPrikker() {
        try {
            if (!Files.exists(jsonPath)) {
                return new ArrayList<>(); // eller kast exception hvis du forventer at fila finnes
            }

            String json = Files.readString(jsonPath);
            if (json.isBlank()) return new ArrayList<>();

            return objectMapper.readValue(json, new TypeReference<List<PrikkRow>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lese " + jsonPath.toAbsolutePath(), e);
        }
        }

    public void writePrikker(List<PrikkRow> prikker) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(prikker);
            Files.writeString(jsonPath, json);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke skrive " + jsonPath.toAbsolutePath(), e);
        }
    }

    public List<PrikkRow> updatePrikker(String navn, boolean increase) {
        List<PrikkRow> prikker = readPrikker();
    
        boolean found = false;
        
        for (int i = 0; i < prikker.size(); i++) {
            PrikkRow r = prikker.get(i);
    
            if (r.navn().equalsIgnoreCase(navn)) {
                int delta = increase ? 1 : -1;
                int ny = Math.max(0, r.prikker() + delta);
    
                PrikkRow updated = new PrikkRow(r.navn(), ny);
                prikker.set(i, updated);
    
                writePrikker(prikker);
    
                // returner hele lista (oppdatert)
                return prikker;
            }
        }
    
        throw new Il@@legalArgumentException("Fant ikke person: " + navn);
    }
    
}


```


#### Short summary: 

empty definition using pc, found symbol in pc: java/lang/IllegalArgumentException#