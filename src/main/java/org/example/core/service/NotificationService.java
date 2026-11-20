package org.example.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.example.core.model.Category;
import org.example.core.model.Money;
import org.example.core.model.Wallet;

/**
 * Оповещения о бюджетах и балансе: 80% лимита, исчерпанный и превышенный бюджет, расходы больше
 * доходов, нулевой баланс. Каждое оповещение - строка, начинающаяся с «[!]».
 */
public class NotificationService {

  private static final int WARNING_PERCENT = 80;

  /** Оповещения после нового расхода по категории. */
  public List<String> afterExpense(Wallet wallet, Category category) {
    List<String> result = new ArrayList<>(budgetAlerts(wallet, category));
    result.addAll(balanceAlerts(wallet));
    return result;
  }

  /** Оповещения по всем бюджетам и балансу, например при входе. */
  public List<String> overview(Wallet wallet) {
    List<String> result = new ArrayList<>();
    for (Category category : wallet.getCategories()) {
      result.addAll(budgetAlerts(wallet, category));
    }
    result.addAll(balanceAlerts(wallet));
    return result;
  }

  public List<String> budgetAlerts(Wallet wallet, Category category) {
    if (!category.hasBudget()) {
      return List.of();
    }
    String name = category.getName();
    BigDecimal limit = category.getBudget();
    BigDecimal spent = wallet.spent(name);

    if (spent.compareTo(limit) > 0) {
      return List.of(
          "[!] Превышен бюджет «%s»: потрачено %s из %s, перерасход %s"
              .formatted(
                  name,
                  Money.format(spent),
                  Money.format(limit),
                  Money.format(spent.subtract(limit))));
    }
    if (spent.compareTo(limit) == 0) {
      return List.of(
          "[!] Бюджет «%s» исчерпан: потрачено %s из %s"
              .formatted(name, Money.format(spent), Money.format(limit)));
    }
    int percent =
        spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.DOWN).intValue();
    if (percent >= WARNING_PERCENT) {
      return List.of(
          "[!] Израсходовано %d%% бюджета «%s»: осталось %s из %s"
              .formatted(percent, name, Money.format(limit.subtract(spent)), Money.format(limit)));
    }
    return List.of();
  }

  public List<String> balanceAlerts(Wallet wallet) {
    BigDecimal income = wallet.totalIncome();
    BigDecimal expense = wallet.totalExpense();
    if (expense.compareTo(income) > 0) {
      return List.of(
          "[!] Расходы (%s) превысили доходы (%s), баланс %s"
              .formatted(
                  Money.format(expense), Money.format(income), Money.format(wallet.balance())));
    }
    if (!wallet.getTransactions().isEmpty() && wallet.balance().signum() == 0) {
      return List.of("[!] Баланс равен нулю");
    }
    return List.of();
  }
}
