package com.pushkar.smart_toll.repository;

import com.pushkar.smart_toll.model.TollPlaza;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TollPlazaRepository {
    private final Map<Long, TollPlaza> tollPlazas = new ConcurrentHashMap<>();
    private Long idCounter = 1L;

    public void save(TollPlaza tollPlaza) {
        if (tollPlaza.getId() == null) {
            tollPlaza.setId(idCounter++);
        }
        tollPlazas.put(tollPlaza.getId(), tollPlaza);
    }

    public void saveAll(List<TollPlaza> plazas) {
        plazas.forEach(this::save);
    }

    public Optional<TollPlaza> findById(Long id) {
        return Optional.ofNullable(tollPlazas.get(id));
    }

    public List<TollPlaza> findAll() {
        return new ArrayList<>(tollPlazas.values());
    }

    public List<TollPlaza> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat,
            Double minLng, Double maxLng) {
        return tollPlazas.values().stream()
                .filter(plaza -> plaza.getLatitude() != null && plaza.getLongitude() != null)
                .filter(plaza -> plaza.getLatitude() >= Math.min(minLat, maxLat)
                        && plaza.getLatitude() <= Math.max(minLat, maxLat))
                .filter(plaza -> plaza.getLongitude() >= Math.min(minLng, maxLng)
                        && plaza.getLongitude() <= Math.max(minLng, maxLng))
                .collect(Collectors.toList());
    }

    public List<TollPlaza> findByName(String name) {
        return tollPlazas.values().stream()
                .filter(plaza -> plaza.getName() != null && plaza.getName().contains(name))
                .collect(Collectors.toList());
    }

    public List<TollPlaza> findByState(String state) {
        return tollPlazas.values().stream()
                .filter(plaza -> plaza.getState() != null && plaza.getState().equalsIgnoreCase(state))
                .collect(Collectors.toList());
    }

    public void deleteAll() {
        tollPlazas.clear();
        idCounter = 1L;
    }

    public long count() {
        return tollPlazas.size();
    }
}
