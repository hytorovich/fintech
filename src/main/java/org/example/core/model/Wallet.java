package org.example.core.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.example.core.error.ValidationException;

/**
 * Кошелёк пользователя: категории, бюджеты и все операции. Баланс не хранится отдельно, а считается
 * из операций, поэтому он не может разойтись с историей.
 */
public class Wallet {

  private String owner;
  private final List<Category> categories = new ArrayList<>();
  private final List<Transaction> transactions = new ArrayList<>();

  /** Нужен для загрузки из JSON. */
  private Wallet() {}

  public Wallet(String owner) {
    this.owner = owner;
  }

  public String getOwner() {
    return owner;
  }

  public List<Category> getCategories() {
    return Collections.unmodifiableList(categories);
  }

  public List<Transaction> getTransactions() {
    return Collections.unmodifiableList(transactions);
  }

  public Optional<Category> findCategory(String name) {
    return categories.stream().filter(category -> category.hasName(name)).findFirst();
  }

  public Category addCategory(String name, OperationType type) {
    if (findCategory(name).isPresent()) {
      throw new ValidationException("Категория «" + name + "» уже существует");
    }
    Category category = new Category(name, type);
    categories.add(category);
    return category;
  }

  public void removeCategory(Category category) {
    categories.remove(category);
  }

  /** Переименовывает категорию и обновляет все её операции. */
  public void renameCategory(Category category, String newName) {
    String oldName = category.getName();
    category.setName(newName);
    transactions.replaceAll(
        transaction ->
            transaction.category().equalsIgnoreCase(oldName)
                ? transaction.withCategory(newName)
                : transaction);
  }

  public void addTransaction(Transaction transaction) {
    transactions.add(transaction);
  }

  /** Удаляет операцию. Используется для отката неудачного перевода. */
  public boolean removeTransaction(Transaction transaction) {
    return transactions.remove(transaction);
  }

  public long countTransactions(String categoryName) {
    return transactions.stream().filter(t -> t.category().equalsIgnoreCase(categoryName)).count();
  }

  public BigDecimal totalIncome() {
    return sum(Transaction::isIncome);
  }

  public BigDecimal totalExpense() {
    return sum(Transaction::isExpense);
  }

  public BigDecimal balance() {
    return totalIncome().subtract(totalExpense());
  }

  /** Сколько потрачено по категории за всё время. */
  public BigDecimal spent(String categoryName) {
    return sum(t -> t.isExpense() && t.category().equalsIgnoreCase(categoryName));
  }

  private BigDecimal sum(Predicate<Transaction> filter) {
    return transactions.stream()
        .filter(filter)
        .map(Transaction::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
