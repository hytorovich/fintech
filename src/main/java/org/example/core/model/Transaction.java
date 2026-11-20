package org.example.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Одна операция в кошельке.
 *
 * @param type доход или расход
 * @param category название категории
 * @param amount сумма, всегда больше нуля
 * @param date дата операции
 * @param counterparty логин второй стороны для переводов, иначе {@code null}
 */
public record Transaction(
    OperationType type, String category, BigDecimal amount, LocalDate date, String counterparty) {

  public boolean isIncome() {
    return type == OperationType.INCOME;
  }

  public boolean isExpense() {
    return type == OperationType.EXPENSE;
  }

  /** Копия операции с другим названием категории (нужна при переименовании). */
  public Transaction withCategory(String newCategory) {
    return new Transaction(type, newCategory, amount, date, counterparty);
  }
}
