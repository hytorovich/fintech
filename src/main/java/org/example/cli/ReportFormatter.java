package org.example.cli;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.example.core.model.Category;
import org.example.core.model.Money;
import org.example.core.model.Transaction;
import org.example.core.model.Wallet;
import org.example.core.service.StatsService.BudgetStatus;
import org.example.core.service.StatsService.CategoryTotal;
import org.example.core.service.StatsService.Selection;
import org.example.core.service.StatsService.Summary;

/**
 * Превращает результаты подсчётов в текст с колонками. Один и тот же текст идёт в терминал и в файл
 * отчёта.
 */
final class ReportFormatter {

  private static final String AMOUNTS = "  %-22s %14s%n";
  private static final String BUDGETS = "  %-22s %12s %12s %18s%n";
  private static final String SELECTION = "  %-22s %-7s %9s %14s%n";
  private static final String CATEGORIES = "  %-22s %-7s %12s %12s %12s%n";
  private static final String HISTORY = "  %-10s  %-7s %-22s %13s  %s%n";
  private static final String HELP = "  %-44s %s%n";

  private ReportFormatter() {}

  static String summary(String owner, Summary summary) {
    StringBuilder out = new StringBuilder();
    out.append("Отчёт пользователя %s, %s%n".formatted(owner, summary.period().describe()));
    out.append("Общий доход: %s%n".formatted(Money.format(summary.totalIncome())));
    out.append("Доходы по категориям:\n");
    appendAmounts(out, summary.incomeByCategory(), "нет доходов");
    out.append("Общие расходы: %s%n".formatted(Money.format(summary.totalExpense())));
    out.append("Расходы по категориям:\n");
    appendAmounts(out, summary.expenseByCategory(), "нет расходов");
    out.append("Баланс: %s%n".formatted(Money.format(summary.balance())));
    out.append("Бюджет по категориям:\n");
    if (summary.budgets().isEmpty()) {
      out.append("  бюджеты не заданы\n");
    } else {
      out.append(BUDGETS.formatted("Категория", "Бюджет", "Потрачено", "Оставшийся бюджет"));
      for (BudgetStatus budget : summary.budgets()) {
        out.append(
            BUDGETS.formatted(
                budget.category(),
                Money.format(budget.limit()),
                Money.format(budget.spent()),
                Money.format(budget.remaining())));
      }
    }
    return out.toString();
  }

  static String selection(Selection selection) {
    StringBuilder out = new StringBuilder();
    out.append("Подсчёт по выбранным категориям, %s%n".formatted(selection.period().describe()));
    if (!selection.found().isEmpty()) {
      out.append(SELECTION.formatted("Категория", "Тип", "Операций", "Сумма"));
      for (CategoryTotal total : selection.found()) {
        out.append(
            SELECTION.formatted(
                total.category(),
                total.type().title(),
                total.count(),
                Money.format(total.amount())));
      }
    }
    for (CategoryTotal total : selection.found()) {
      if (total.count() == 0) {
        out.append("  По категории «%s» нет операций за этот период%n".formatted(total.category()));
      }
    }
    for (String name : selection.notFound()) {
      out.append("  Категория «%s» не найдена%n".formatted(name));
    }
    if (!selection.found().isEmpty()) {
      out.append("Доходы по выбранным: %s%n".formatted(Money.format(selection.totalIncome())));
      out.append("Расходы по выбранным: %s%n".formatted(Money.format(selection.totalExpense())));
    }
    return out.toString();
  }

  static String categories(Wallet wallet) {
    if (wallet.getCategories().isEmpty()) {
      return "Категорий пока нет. Добавьте операцию или команду add-category\n";
    }
    StringBuilder out = new StringBuilder();
    out.append(CATEGORIES.formatted("Категория", "Тип", "Сумма", "Бюджет", "Остаток"));
    for (Category category : wallet.getCategories()) {
      BigDecimal total =
          wallet.getTransactions().stream()
              .filter(t -> t.category().equals(category.getName()))
              .map(Transaction::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      String budget = "-";
      String remaining = "-";
      if (category.hasBudget()) {
        budget = Money.format(category.getBudget());
        remaining = Money.format(category.getBudget().subtract(total));
      }
      out.append(
          CATEGORIES.formatted(
              category.getName(),
              category.getType().title(),
              Money.format(total),
              budget,
              remaining));
    }
    return out.toString();
  }

  static String history(List<Transaction> transactions) {
    if (transactions.isEmpty()) {
      return "Операций нет\n";
    }
    StringBuilder out = new StringBuilder();
    out.append(HISTORY.formatted("Дата", "Тип", "Категория", "Сумма", "Перевод"));
    for (Transaction t : transactions) {
      String transfer = "";
      if (t.counterparty() != null) {
        transfer = (t.isExpense() ? "кому: " : "от: ") + t.counterparty();
      }
      out.append(
          HISTORY.formatted(
              t.date(),
              t.type().title(),
              t.category(),
              (t.isExpense() ? "-" : "+") + Money.format(t.amount()),
              transfer));
    }
    return out.toString();
  }

  static String help() {
    StringBuilder out = new StringBuilder("Команды:\n");
    String[][] commands = {
      {"register <логин> <пароль>", "регистрация нового пользователя"},
      {"login <логин> <пароль>", "вход, загружает кошелёк из файла"},
      {"income <категория> <сумма> [дата]", "добавить доход"},
      {"expense <категория> <сумма> [дата]", "добавить расход"},
      {"add-category <название> <income|expense>", "создать категорию"},
      {"rename-category <старое> -> <новое>", "переименовать категорию"},
      {"delete-category <название>", "удалить категорию без операций"},
      {"budget <категория> <сумма>", "установить или изменить бюджет"},
      {"remove-budget <категория>", "убрать бюджет"},
      {"categories", "категории, бюджеты и остатки"},
      {"balance", "текущий баланс"},
      {"stats [--from дата] [--to дата]", "сводка по доходам, расходам и бюджетам"},
      {"stats <кат1>, <кат2> [--from] [--to]", "подсчёт по выбранным категориям"},
      {"history [--from дата] [--to дата]", "список операций"},
      {"report <файл> [--from] [--to]", "сохранить сводку в текстовый файл"},
      {"transfer <логин> <сумма>", "перевод другому пользователю"},
      {"export <файл.csv>", "выгрузить операции в CSV"},
      {"import <файл.csv>", "загрузить операции из CSV"},
      {"logout", "сохранить кошелёк и выйти из аккаунта"},
      {"exit", "сохранить данные и завершить программу"},
      {"help", "эта справка"}
    };
    for (String[] command : commands) {
      out.append(HELP.formatted(command[0], command[1]));
    }
    out.append(
        """

        Даты в формате ГГГГ-ММ-ДД. Сумма: 1500 или 99.90.
        Название категории может содержать пробелы: expense Коммунальные услуги 3000
        Примеры:
          register anna secret1
          login anna secret1
          income Зарплата 40000
          expense Еда 300 2026-11-15
          budget Еда 4000
          stats Еда, Такси --from 2026-11-01
        """);
    return out.toString();
  }

  private static void appendAmounts(
      StringBuilder out, Map<String, BigDecimal> amounts, String none) {
    if (amounts.isEmpty()) {
      out.append("  ").append(none).append('\n');
      return;
    }
    amounts.forEach((name, amount) -> out.append(AMOUNTS.formatted(name, Money.format(amount))));
  }
}
