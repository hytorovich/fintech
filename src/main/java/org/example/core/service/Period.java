package org.example.core.service;

import java.time.LocalDate;
import org.example.core.error.ValidationException;

/**
 * Период для отбора операций. Любая граница может быть {@code null}, тогда период с этой стороны не
 * ограничен.
 */
public record Period(LocalDate from, LocalDate to) {

  public static final Period ALL = new Period(null, null);

  public Period {
    if (from != null && to != null && from.isAfter(to)) {
      throw new ValidationException("Начало периода позже конца: " + from + " > " + to);
    }
  }

  public boolean contains(LocalDate date) {
    return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
  }

  public boolean isAll() {
    return from == null && to == null;
  }

  /** Описание для заголовка отчёта. */
  public String describe() {
    if (isAll()) {
      return "за всё время";
    }
    if (from == null) {
      return "по " + to;
    }
    if (to == null) {
      return "с " + from;
    }
    return "с " + from + " по " + to;
  }
}
