package com.sportsbooking.controller;

import com.sportsbooking.dto.FieldRequest;
import com.sportsbooking.dto.FieldResponse;
import com.sportsbooking.service.FieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD operations for sports fields.
 * All endpoints require a valid ADMIN JWT (Authorization: Bearer <token>).
 *
 * GET    /api/fields        — list all fields
 * POST   /api/fields        — create a new field
 * GET    /api/fields/{id}   — get field by id
 * PUT    /api/fields/{id}   — update field
 * DELETE /api/fields/{id}   — delete field
 */
@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
@Tag(name = "Fields", description = "Manage sports fields (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class FieldController {

    private final FieldService fieldService;

    @GetMapping
    @Operation(summary = "List all fields")
    public ResponseEntity<List<FieldResponse>> getAll() {
        return ResponseEntity.ok(fieldService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get field by ID")
    public ResponseEntity<FieldResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fieldService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new field")
    public ResponseEntity<FieldResponse> create(@Valid @RequestBody FieldRequest request) {
        FieldResponse created = fieldService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing field")
    public ResponseEntity<FieldResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldRequest request) {
        return ResponseEntity.ok(fieldService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a field")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fieldService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
