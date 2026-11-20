package org.example.core.model;

import java.util.Locale;
import org.example.core.error.ValidationException;

/** Тип операции и категории: доход или расход. */
public enum OperationType {
  INCOME("доход"),
  EXPENSE("расход");

  private final String title;

  OperationType(String title) {
    this.title = title;
  }

  /** Название типа на русском для вывода пользователю. */
  public String title() {
    return title;
  }

  /** Разбирает тип из ввода: income/expense или доход/расход. */
  public static OperationType parse(String text) {
    String value = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    return switch (value) {
      case "income", "доход", "доходы" -> INCOME;
      case "expense", "расход", "расходы" -> EXPENSE;
      default ->
          throw new ValidationException(
              "Неизвестный тип: «" + text + "». Ожидается income (доход) или expense (расход)");
    };
  }
}
