package org.example.core;

import static org.example.support.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.core.error.ValidationException;
import org.example.core.model.OperationType;
import org.example.core.model.Transaction;
import org.example.core.model.Wallet;
import org.example.core.service.FinanceService;
import org.example.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinanceServiceTest {

  private FinanceService finance;
  private Wallet wallet;

  @BeforeEach
  void setUp() {
    finance = TestData.finance();
    wallet = new Wallet("anna");
  }

  @Test
  void addOperationCreatesCategoryAndUpdatesBalance() {
    finance.addOperation(wallet, OperationType.INCOME, "Зарплата", money("1000"), null);
    finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money("250.50"), null);

    assertEquals(0, money("749.50").compareTo(wallet.balance()));
    assertTrue(wallet.findCategory("зарплата").isPresent());
    assertEquals(TestData.TODAY, wallet.getTransactions().get(0).date());
  }

  @Test
  void categoryNameIsCaseInsensitive() {
    finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money("100"), null);
    finance.addOperation(wallet, OperationType.EXPENSE, "  еДА ", money("100"), null);

    assertEquals(1, wallet.getCategories().size());
    assertEquals(0, money("200").compareTo(wallet.spent("Еда")));
  }

  @Test
  void incomeToExpenseCategoryIsRejected() {
    finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money("100"), null);

    assertThrows(
        ValidationException.class,
        () -> finance.addOperation(wallet, OperationType.INCOME, "Еда", money("100"), null));
  }

  @Test
  void zeroNegativeAndTooPreciseAmountsAreRejected() {
    for (String bad : List.of("0", "-5", "1.005")) {
      assertThrows(
          ValidationException.class,
          () -> finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money(bad), null));
    }
    assertTrue(wallet.getTransactions().isEmpty());
  }

  @Test
  void futureDateIsRejected() {
    LocalDate tomorrow = TestData.TODAY.plusDays(1);

    assertThrows(
        ValidationException.class,
        () -> finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money("1"), tomorrow));
  }

  @Test
  void setBudgetCreatesExpenseCategoryAndCanBeChanged() {
    finance.setBudget(wallet, "Еда", money("4000"));
    List<String> changed = finance.setBudget(wallet, "Еда", money("5000"));

    assertEquals(0, money("5000").compareTo(wallet.findCategory("Еда").get().getBudget()));
    assertTrue(changed.get(0).contains("изменён"));
  }

  @Test
  void budgetOnIncomeCategoryIsRejected() {
    finance.addOperation(wallet, OperationType.INCOME, "Зарплата", money("1000"), null);

    assertThrows(
        ValidationException.class, () -> finance.setBudget(wallet, "Зарплата", money("100")));
  }

  @Test
  void removeBudgetKeepsCategory() {
    finance.setBudget(wallet, "Еда", money("4000"));

    finance.removeBudget(wallet, "Еда");

    assertFalse(wallet.findCategory("Еда").get().hasBudget());
    assertThrows(ValidationException.class, () -> finance.removeBudget(wallet, "Еда"));
  }

  @Test
  void renameCategoryMovesOperations() {
    finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money("300"), null);

    finance.renameCategory(wallet, "Еда", "Продукты");

    assertTrue(wallet.findCategory("Еда").isEmpty());
    assertEquals("Продукты", wallet.getTransactions().get(0).category());
    assertEquals(0, money("300").compareTo(wallet.spent("Продукты")));
  }

  @Test
  void renameToExistingCategoryIsRejected() {
    finance.addCategory(wallet, "Еда", OperationType.EXPENSE);
    finance.addCategory(wallet, "Такси", OperationType.EXPENSE);

    assertThrows(ValidationException.class, () -> finance.renameCategory(wallet, "Еда", "такси"));
  }

  @Test
  void categoryWithOperationsCannotBeDeleted() {
    finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money("300"), null);
    finance.addCategory(wallet, "Подарки", OperationType.EXPENSE);

    assertThrows(ValidationException.class, () -> finance.deleteCategory(wallet, "Еда"));
    finance.deleteCategory(wallet, "Подарки");
    assertTrue(wallet.findCategory("Подарки").isEmpty());
  }

  @Test
  void importReportsInvalidRowsAndKeepsValidOnes() {
    finance.addOperation(wallet, OperationType.EXPENSE, "Еда", money("1"), null);
    Map<Integer, Transaction> rows = new LinkedHashMap<>();
    rows.put(2, new Transaction(OperationType.INCOME, "Зарплата", money("100"), null, null));
    rows.put(3, new Transaction(OperationType.INCOME, "Еда", money("100"), null, null));

    FinanceService.ImportReport report = finance.importTransactions(wallet, rows);

    assertEquals(1, report.imported());
    assertEquals(1, report.errors().size());
    assertTrue(report.errors().get(0).startsWith("Строка 3"));
  }
}
