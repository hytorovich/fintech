package org.example.core;

import static org.example.support.TestData.expense;
import static org.example.support.TestData.income;
import static org.example.support.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.example.core.model.OperationType;
import org.example.core.model.Wallet;
import org.example.core.service.FinanceService;
import org.example.core.service.NotificationService;
import org.example.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

  private final NotificationService notifications = new NotificationService();
  private FinanceService finance;
  private Wallet wallet;

  @BeforeEach
  void setUp() {
    finance = TestData.finance();
    wallet = new Wallet("anna");
    income(finance, wallet, "Зарплата", "10000");
    finance.setBudget(wallet, "Еда", money("1000"));
  }

  @Test
  void noWarningBelowEightyPercent() {
    List<String> result = add("Еда", "799");

    assertTrue(result.isEmpty());
  }

  @Test
  void warningAtEightyPercentOfBudget() {
    List<String> result = add("Еда", "800");

    assertEquals(1, result.size());
    assertTrue(result.get(0).contains("80%"));
  }

  @Test
  void warningWhenBudgetExceeded() {
    expense(finance, wallet, "Еда", "900");
    List<String> result = add("Еда", "200");

    assertTrue(result.get(0).contains("Превышен бюджет «Еда»"));
    assertTrue(result.get(0).contains("перерасход 100.00"));
  }

  @Test
  void warningWhenExpensesExceedIncome() {
    List<String> result = add("Отпуск", "15000");

    assertTrue(result.stream().anyMatch(n -> n.startsWith("[!] Расходы")));
  }

  @Test
  void warningWhenBalanceIsZero() {
    List<String> result = add("Отпуск", "10000");

    assertTrue(result.stream().anyMatch(n -> n.equals("[!] Баланс равен нулю")));
  }

  @Test
  void overviewListsAllProblemBudgets() {
    expense(finance, wallet, "Еда", "1500");
    finance.setBudget(wallet, "Такси", money("100"));
    expense(finance, wallet, "Такси", "100");

    assertEquals(2, notifications.overview(wallet).size());
  }

  private List<String> add(String category, String amount) {
    return finance
        .addOperation(wallet, OperationType.EXPENSE, category, money(amount), null)
        .stream()
        .filter(message -> message.startsWith("[!]"))
        .toList();
  }
}
