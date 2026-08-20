package com.sportsbooking.repository;

import com.sportsbooking.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository extends JpaRepository<Field, Long> {
}
