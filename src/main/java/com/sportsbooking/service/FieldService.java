package com.sportsbooking.service;

import com.sportsbooking.dto.FieldRequest;
import com.sportsbooking.dto.FieldResponse;
import com.sportsbooking.entity.Field;
import com.sportsbooking.exception.ResourceNotFoundException;
import com.sportsbooking.repository.FieldRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FieldService {

    private static final Logger log = LoggerFactory.getLogger(FieldService.class);

    private final FieldRepository fieldRepository;

    // ─── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FieldResponse> findAll() {
        return fieldRepository.findAll()
                .stream()
                .map(FieldResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FieldResponse findById(Long id) {
        return FieldResponse.from(getOrThrow(id));
    }

    // ─── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public FieldResponse create(FieldRequest request) {
        Field field = toEntity(request, new Field());
        Field saved = fieldRepository.save(field);
        log.info("Field created: id={}, name='{}', sport={}, indoor={}",
                saved.getId(), saved.getName(), saved.getSportType(), saved.isIndoor());
        return FieldResponse.from(saved);
    }

    @Transactional
    public FieldResponse update(Long id, FieldRequest request) {
        Field field = getOrThrow(id);
        toEntity(request, field);
        Field saved = fieldRepository.save(field);
        log.info("Field updated: id={}, name='{}'", saved.getId(), saved.getName());
        return FieldResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Field field = getOrThrow(id);
        fieldRepository.delete(field);
        log.info("Field deleted: id={}, name='{}'", field.getId(), field.getName());
    }

    // ─── Package-private helper used by ReservationService ────────────────────

    @Transactional(readOnly = true)
    public Field getOrThrow(Long id) {
        return fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Field not found with id: " + id));
    }

    // ─── Internal mapper ───────────────────────────────────────────────────────

    private Field toEntity(FieldRequest request, Field field) {
        field.setName(request.name());
        field.setSportType(request.sportType());
        field.setIndoor(request.indoor());
        field.setPricePerHour(request.pricePerHour());
        if (request.latitude() != null)  field.setLatitude(request.latitude());
        if (request.longitude() != null) field.setLongitude(request.longitude());
        return field;
    }
}
