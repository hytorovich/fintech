package org.example.core.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.example.core.error.ValidationException;
import org.example.core.model.Category;
import org.example.core.model.Money;
import org.example.core.model.OperationType;
import org.example.core.model.Transaction;
import org.example.core.model.Wallet;

/** Доходы, расходы, категории и бюджеты внутри одного кошелька. */
public class FinanceService {

  private final Clock clock;
  private final NotificationService notifications;

  /**
   * @param clock источник текущей даты; в тестах подставляется фиксированная дата
   */
  public FinanceService(Clock clock, NotificationService notifications) {
    this.clock = clock;
    this.notifications = notifications;
  }

  /** Итог импорта: сколько операций добавлено и какие строки пропущены. */
  public record ImportReport(int imported, List<String> errors) {}

  public List<String> addOperation(
      Wallet wallet, OperationType type, String categoryName, BigDecimal amount, LocalDate date) {
    return addOperation(wallet, type, categoryName, amount, date, null);
  }

  /**
   * Добавляет доход или расход. Если категории ещё нет, она создаётся с типом операции. Если
   * категория есть, её тип должен совпадать с типом операции.
   *
   * @return сообщения для пользователя: о новой категории и оповещения о бюджете
   */
  public List<String> addOperation(
      Wallet wallet,
      OperationType type,
      String categoryName,
      BigDecimal amount,
      LocalDate date,
      String counterparty) {
    String name = Validator.requireCategoryName(categoryName);
    BigDecimal value = Validator.requireAmount(amount);
    LocalDate today = LocalDate.now(clock);
    LocalDate operationDate = date == null ? today : date;
    if (operationDate.isAfter(today)) {
      throw new ValidationException("Дата операции не может быть в будущем: " + operationDate);
    }

    List<String> messages = new ArrayList<>();
    Category category = wallet.findCategory(name).orElse(null);
    if (category == null) {
      category = wallet.addCategory(name, type);
      messages.add("Создана новая категория «%s» (%s)".formatted(name, type.title()));
    } else {
      requireType(category, type);
    }

    wallet.addTransaction(
        new Transaction(type, category.getName(), value, operationDate, counterparty));
    if (type == OperationType.EXPENSE) {
      messages.addAll(notifications.afterExpense(wallet, category));
    }
    return messages;
  }

  /** Проверяет заранее, что в категорию можно добавить операцию этого типа. */
  public void checkCategoryAccepts(Wallet wallet, String categoryName, OperationType type) {
    wallet.findCategory(categoryName).ifPresent(category -> requireType(category, type));
  }

  public Category addCategory(Wallet wallet, String name, OperationType type) {
    return wallet.addCategory(Validator.requireCategoryName(name), type);
  }

  /** Устанавливает или меняет бюджет. Если категории нет, создаёт категорию расходов. */
  public List<String> setBudget(Wallet wallet, String categoryName, BigDecimal amount) {
    String name = Validator.requireCategoryName(categoryName);
    BigDecimal value = Validator.requireAmount(amount);
    List<String> messages = new ArrayList<>();

    Category category = wallet.findCategory(name).orElse(null);
    if (category == null) {
      category = wallet.addCategory(name, OperationType.EXPENSE);
      messages.add("Создана новая категория расходов «%s»".formatted(name));
    } else if (category.getType() != OperationType.EXPENSE) {
      throw new ValidationException(
          "Бюджет можно задать только для категории расходов, а «%s» - категория доходов"
              .formatted(category.getName()));
    }

    String action = category.hasBudget() ? "изменён" : "установлен";
    category.setBudget(value);
    messages.add("Бюджет «%s» %s: %s".formatted(category.getName(), action, Money.format(value)));
    messages.addAll(notifications.budgetAlerts(wallet, category));
    return messages;
  }

  public void removeBudget(Wallet wallet, String categoryName) {
    Category category = requireCategory(wallet, categoryName);
    if (!category.hasBudget()) {
      throw new ValidationException("У категории «" + category.getName() + "» нет бюджета");
    }
    category.removeBudget();
  }

  /** Переименовывает категорию, операции переносятся под новое название. */
  public void renameCategory(Wallet wallet, String oldName, String newName) {
    Category category = requireCategory(wallet, oldName);
    String name = Validator.requireCategoryName(newName);
    Category existing = wallet.findCategory(name).orElse(null);
    if (existing != null && existing != category) {
      throw new ValidationException("Категория «" + existing.getName() + "» уже существует");
    }
    wallet.renameCategory(category, name);
  }

  /** Удаляет категорию без операций, чтобы не потерять историю. */
  public void deleteCategory(Wallet wallet, String categoryName) {
    Category category = requireCategory(wallet, categoryName);
    long count = wallet.countTransactions(category.getName());
    if (count > 0) {
      throw new ValidationException(
          "Нельзя удалить «%s»: по ней есть операции (%d). Её можно переименовать"
              .formatted(category.getName(), count));
    }
    wallet.removeCategory(category);
  }

  public Category requireCategory(Wallet wallet, String categoryName) {
    if (categoryName == null || categoryName.isBlank()) {
      throw new ValidationException("Не указана категория");
    }
    return wallet
        .findCategory(categoryName)
        .orElseThrow(
            () -> new ValidationException("Категория «" + categoryName.trim() + "» не найдена"));
  }

  /**
   * Добавляет операции из файла. Ошибочные строки пропускаются и попадают в отчёт.
   *
   * @param rowsByLine операции по номеру строки файла
   */
  public ImportReport importTransactions(Wallet wallet, Map<Integer, Transaction> rowsByLine) {
    int imported = 0;
    List<String> errors = new ArrayList<>();
    for (Map.Entry<Integer, Transaction> row : rowsByLine.entrySet()) {
      Transaction t = row.getValue();
      try {
        addOperation(wallet, t.type(), t.category(), t.amount(), t.date(), t.counterparty());
        imported++;
      } catch (ValidationException e) {
        errors.add("Строка " + row.getKey() + ": " + e.getMessage());
      }
    }
    return new ImportReport(imported, errors);
  }

  private static void requireType(Category category, OperationType type) {
    if (category.getType() != type) {
      throw new ValidationException(
          "«%s» - категория типа «%s», в неё нельзя добавить %s"
              .formatted(category.getName(), category.getType().title(), type.title()));
    }
  }
}
