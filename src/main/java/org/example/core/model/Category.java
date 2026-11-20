package org.example.core.model;

import java.math.BigDecimal;

/**
 * Категория операций. У категории расходов может быть бюджет (лимит трат), у категории доходов
 * бюджета нет.
 */
public class Category {

  private String name;
  private OperationType type;

  /** Лимит трат. {@code null} означает, что бюджет не задан. */
  private BigDecimal budget;

  /** Нужен для загрузки из JSON. */
  private Category() {}

  public Category(String name, OperationType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public OperationType getType() {
    return type;
  }

  public BigDecimal getBudget() {
    return budget;
  }

  public boolean hasBudget() {
    return budget != null;
  }

  /** Совпадает ли название без учёта регистра и пробелов по краям. */
  public boolean hasName(String other) {
    return other != null && name.equalsIgnoreCase(other.trim());
  }

  void setName(String name) {
    this.name = name;
  }

  public void setBudget(BigDecimal budget) {
    this.budget = budget;
  }

  public void removeBudget() {
    this.budget = null;
  }
}
