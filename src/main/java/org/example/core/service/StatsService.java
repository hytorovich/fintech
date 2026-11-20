package org.example.core.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.example.core.error.ValidationException;
import org.example.core.model.Category;
import org.example.core.model.OperationType;
import org.example.core.model.Transaction;
import org.example.core.model.Wallet;

/** Подсчёты доходов, расходов и бюджетов: общие, по категориям и за период. */
public class StatsService {

  /** Состояние бюджета категории. */
  public record BudgetStatus(String category, BigDecimal limit, BigDecimal spent) {
    public BigDecimal remaining() {
      return limit.subtract(spent);
    }
  }

  /** Полная сводка по кошельку. */
  public record Summary(
      Period period,
      BigDecimal totalIncome,
      BigDecimal totalExpense,
      Map<String, BigDecimal> incomeByCategory,
      Map<String, BigDecimal> expenseByCategory,
      List<BudgetStatus> budgets) {

    public BigDecimal balance() {
      return totalIncome.subtract(totalExpense);
    }
  }

  /** Итог по одной выбранной категории. */
  public record CategoryTotal(String category, OperationType type, BigDecimal amount, long count) {}

  /** Итог по нескольким выбранным категориям. */
  public record Selection(
      Period period,
      List<CategoryTotal> found,
      List<String> notFound,
      BigDecimal totalIncome,
      BigDecimal totalExpense) {}

  public Summary summary(Wallet wallet, Period period) {
    List<Transaction> operations = operations(wallet, period);
    List<BudgetStatus> budgets = new ArrayList<>();
    for (Category category : wallet.getCategories()) {
      if (category.hasBudget()) {
        BigDecimal spent =
            sum(operations, t -> t.isExpense() && t.category().equals(category.getName()));
        budgets.add(new BudgetStatus(category.getName(), category.getBudget(), spent));
      }
    }
    return new Summary(
        period,
        sum(operations, Transaction::isIncome),
        sum(operations, Transaction::isExpense),
        byCategory(operations, OperationType.INCOME),
        byCategory(operations, OperationType.EXPENSE),
        budgets);
  }

  /**
   * Подсчёт по выбранным категориям. Ненайденные категории попадают в {@code notFound}, а не
   * прерывают подсчёт остальных.
   */
  public Selection select(Wallet wallet, List<String> categoryNames, Period period) {
    List<String> names =
        categoryNames.stream().map(String::trim).filter(name -> !name.isEmpty()).toList();
    if (names.isEmpty()) {
      throw new ValidationException("Укажите хотя бы одну категорию");
    }
    List<Transaction> operations = operations(wallet, period);
    List<CategoryTotal> found = new ArrayList<>();
    List<String> notFound = new ArrayList<>();
    for (String name : names) {
      Category category = wallet.findCategory(name).orElse(null);
      if (category == null) {
        notFound.add(name);
        continue;
      }
      if (found.stream().anyMatch(total -> total.category().equals(category.getName()))) {
        continue;
      }
      List<Transaction> ops =
          operations.stream().filter(t -> t.category().equals(category.getName())).toList();
      found.add(
          new CategoryTotal(
              category.getName(), category.getType(), sum(ops, t -> true), ops.size()));
    }
    BigDecimal income = BigDecimal.ZERO;
    BigDecimal expense = BigDecimal.ZERO;
    for (CategoryTotal total : found) {
      if (total.type() == OperationType.INCOME) {
        income = income.add(total.amount());
      } else {
        expense = expense.add(total.amount());
      }
    }
    return new Selection(period, found, notFound, income, expense);
  }

  /** Операции кошелька за период, отсортированные по дате. */
  public List<Transaction> history(Wallet wallet, Period period) {
    return operations(wallet, period);
  }

  private static List<Transaction> operations(Wallet wallet, Period period) {
    return wallet.getTransactions().stream()
        .filter(t -> period.contains(t.date()))
        .sorted((a, b) -> a.date().compareTo(b.date()))
        .toList();
  }

  private static Map<String, BigDecimal> byCategory(
      List<Transaction> operations, OperationType type) {
    return operations.stream()
        .filter(t -> t.type() == type)
        .collect(
            Collectors.groupingBy(
                Transaction::category,
                LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
  }

  private static BigDecimal sum(List<Transaction> operations, Predicate<Transaction> filter) {
    return operations.stream()
        .filter(filter)
        .map(Transaction::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
