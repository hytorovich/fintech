package org.example.core;

import static org.example.support.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.example.core.error.ValidationException;
import org.example.core.model.OperationType;
import org.example.core.model.Wallet;
import org.example.core.service.FinanceService;
import org.example.core.service.Period;
import org.example.core.service.StatsService;
import org.example.core.service.StatsService.BudgetStatus;
import org.example.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatsServiceTest {

  private final StatsService stats = new StatsService();
  private FinanceService finance;
  private Wallet wallet;

  @BeforeEach
  void setUp() {
    finance = TestData.finance();
    wallet = TestData.exampleWallet(finance);
  }

  /** Числа из примера в задании. */
  @Test
  void summaryMatchesExampleFromAssignment() {
    StatsService.Summary summary = stats.summary(wallet, Period.ALL);

    assertEquals(0, money("63000").compareTo(summary.totalIncome()));
    assertEquals(0, money("8300").compareTo(summary.totalExpense()));
    assertEquals(0, money("60000").compareTo(summary.incomeByCategory().get("Зарплата")));
    assertEquals(0, money("3000").compareTo(summary.incomeByCategory().get("Бонус")));

    Map<String, BigDecimal> remaining =
        summary.budgets().stream()
            .collect(Collectors.toMap(BudgetStatus::category, BudgetStatus::remaining));
    assertEquals(0, money("3200").compareTo(remaining.get("Еда")));
    assertEquals(0, money("0").compareTo(remaining.get("Развлечения")));
    assertEquals(0, money("-500").compareTo(remaining.get("Коммунальные услуги")));
  }

  @Test
  void selectionCountsSeveralCategoriesAndReportsMissing() {
    StatsService.Selection selection =
        stats.select(wallet, List.of("Еда", "такси", "Едаа"), Period.ALL);

    assertEquals(2, selection.found().size());
    assertEquals(List.of("Едаа"), selection.notFound());
    assertEquals(0, money("2300").compareTo(selection.totalExpense()));
  }

  @Test
  void selectionWithoutCategoriesIsRejected() {
    assertThrows(ValidationException.class, () -> stats.select(wallet, List.of(" "), Period.ALL));
  }

  @Test
  void periodFiltersOperationsByDate() {
    Wallet dated = new Wallet("anna");
    finance.addOperation(
        dated, OperationType.EXPENSE, "Еда", money("100"), TestData.TODAY.minusMonths(1));
    finance.addOperation(dated, OperationType.EXPENSE, "Еда", money("50"), TestData.TODAY);

    Period thisMonth = new Period(TestData.TODAY.withDayOfMonth(1), TestData.TODAY);
    StatsService.Summary summary = stats.summary(dated, thisMonth);

    assertEquals(0, money("50").compareTo(summary.totalExpense()));
    Period lastMonth = new Period(null, TestData.TODAY.withDayOfMonth(1).minusDays(1));
    assertEquals(0, money("100").compareTo(stats.summary(dated, lastMonth).totalExpense()));
  }

  @Test
  void periodWithStartAfterEndIsRejected() {
    assertThrows(
        ValidationException.class, () -> new Period(TestData.TODAY, TestData.TODAY.minusDays(1)));
  }
}
