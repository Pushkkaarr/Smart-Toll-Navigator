package com.pushkar.smart_toll.service;

import com.pushkar.smart_toll.model.TollPlaza;
import com.pushkar.smart_toll.repository.TollPlazaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TollPlazaCsvLoaderService {
    private final TollPlazaRepository tollPlazaRepository;

    @Value("${toll.plaza.csv.path:toll_plaza_india.csv}")
    private String csvFilePath;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTollPlazasFromCsv() {
        try {
            log.info("Starting to load toll plazas from CSV: {}", csvFilePath);
            List<TollPlaza> tollPlazas = readCsvFile();
            tollPlazaRepository.saveAll(tollPlazas);
            log.info("Successfully loaded {} toll plazas from CSV", tollPlazas.size());
        } catch (IOException e) {
            log.error("Error loading toll plazas from CSV file", e);
            throw new RuntimeException("Failed to load toll plaza data from CSV", e);
        }
    }

    private List<TollPlaza> readCsvFile() throws IOException {
        List<TollPlaza> tollPlazas = new ArrayList<>();
        
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(csvFilePath)) {
            
            if (inputStream == null) {
                throw new IOException("CSV file not found: " + csvFilePath);
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 CSVParser csvParser = CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .parse(reader)) {

                for (CSVRecord record : csvParser) {
                    try {
                        String longitude = record.get("longitude");
                        String latitude = record.get("latitude");
                        String tollName = record.get("toll_name");
                        String state = record.get("geo_state");

                        // Skip if required fields are missing
                        if (longitude == null || latitude == null || tollName == null || state == null) {
                            log.warn("Skipping record with missing fields: {}", record);
                            continue;
                        }

                        TollPlaza tollPlaza = TollPlaza.builder()
                                .name(tollName.trim())
                                .latitude(Double.parseDouble(latitude.trim()))
                                .longitude(Double.parseDouble(longitude.trim()))
                                .state(state.trim())
                                .build();

                        tollPlazas.add(tollPlaza);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid number format in record: {}", record, e);
                    } catch (Exception e) {
                        log.warn("Error processing CSV record: {}", record, e);
                    }
                }
            }
        }

        return tollPlazas;
    }
}
