package org.example.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.example.core.error.ValidationException;
import org.example.core.model.OperationType;
import org.junit.jupiter.api.Test;

class CommandArgsTest {

  @Test
  void categoryWithSpacesAmountAndDate() {
    CommandArgs.Operation op =
        CommandArgs.operation("Коммунальные услуги 3000,50 2026-11-15", "usage");

    assertEquals("Коммунальные услуги", op.category());
    assertEquals(new BigDecimal("3000.50"), op.amount());
    assertEquals(LocalDate.of(2026, 11, 15), op.date());
  }

  @Test
  void dateIsOptional() {
    assertNull(CommandArgs.operation("Еда 300", "usage").date());
  }

  @Test
  void missingAmountIsRejected() {
    assertThrows(ValidationException.class, () -> CommandArgs.operation("Еда", "usage"));
  }

  @Test
  void filterReadsCategoriesAndPeriod() {
    CommandArgs.Filter filter =
        CommandArgs.filter("Еда, Коммунальные услуги --from 2026-11-01 --to 2026-11-30");

    assertEquals(List.of("Еда", "Коммунальные услуги"), filter.categories());
    assertEquals(LocalDate.of(2026, 11, 1), filter.period().from());
    assertEquals(LocalDate.of(2026, 11, 30), filter.period().to());
  }

  @Test
  void renameAndTypeParsing() {
    assertEquals("Продукты", CommandArgs.rename("Еда -> Продукты", "usage").newName());
    assertEquals(OperationType.INCOME, CommandArgs.nameAndType("Подработка доход", "usage").type());
    assertThrows(ValidationException.class, () -> CommandArgs.rename("Еда Продукты", "usage"));
  }
}
