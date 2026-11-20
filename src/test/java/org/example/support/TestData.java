package org.example.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.example.core.model.OperationType;
import org.example.core.model.Wallet;
import org.example.core.service.FinanceService;
import org.example.core.service.NotificationService;

/** Общие данные для тестов: фиксированная дата и кошелёк из примера в ТЗ. */
public final class TestData {

  public static final LocalDate TODAY = LocalDate.of(2026, 11, 20);
  public static final Clock CLOCK =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private TestData() {}

  public static FinanceService finance() {
    return new FinanceService(CLOCK, new NotificationService());
  }

  public static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  /** Кошелёк с данными из примера в задании. */
  public static Wallet exampleWallet(FinanceService finance) {
    Wallet wallet = new Wallet("nikolay");
    expense(finance, wallet, "Еда", "300");
    expense(finance, wallet, "Еда", "500");
    expense(finance, wallet, "Развлечения", "3000");
    expense(finance, wallet, "Коммунальные услуги", "3000");
    expense(finance, wallet, "Такси", "1500");
    income(finance, wallet, "Зарплата", "20000");
    income(finance, wallet, "Зарплата", "40000");
    income(finance, wallet, "Бонус", "3000");
    finance.setBudget(wallet, "Еда", money("4000"));
    finance.setBudget(wallet, "Развлечения", money("3000"));
    finance.setBudget(wallet, "Коммунальные услуги", money("2500"));
    return wallet;
  }

  public static void expense(FinanceService finance, Wallet wallet, String category, String sum) {
    finance.addOperation(wallet, OperationType.EXPENSE, category, money(sum), null);
  }

  public static void income(FinanceService finance, Wallet wallet, String category, String sum) {
    finance.addOperation(wallet, OperationType.INCOME, category, money(sum), null);
  }
}
