package sigurdws.NB69.prikker;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class PrikkManager {

    static final ObjectMapper objectMapper = new ObjectMapper();
    static final Path jsonPath = Path.of("/Users/sigurdschonfelder/KODE/NB69 - App/backend/NB69/src/main/java/sigurdws/NB69/prikker/prikker.json");


    public static List<PrikkRow> readPrikker() {
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

    public static void writePrikker(List<PrikkRow> prikker) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(prikker);
            Files.writeString(jsonPath, json);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke skrive " + jsonPath.toAbsolutePath(), e);
        }
    }

    public static List<PrikkRow> updatePrikker(String navn, boolean increase) {
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
                found = true;
                // returner hele lista (oppdatert)
                return prikker;
            }
        }
    
        if (!found) {
            throw new IllegalArgumentException("Fant ikke person: " + navn);
        }

        writePrikker(prikker);
        return prikker;
    }
    
}

