package org.example.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.example.core.error.ValidationException;

/** Проверки и разбор пользовательского ввода. Все ошибки - {@link ValidationException}. */
public final class Validator {

  /** Логин используется в имени файла кошелька, поэтому набор символов ограничен. */
  private static final Pattern LOGIN = Pattern.compile("[a-z0-9][a-z0-9_.-]{2,31}");

  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final int MIN_PASSWORD_LENGTH = 4;
  private static final int MAX_CATEGORY_LENGTH = 40;

  private Validator() {}

  /** Проверяет логин и приводит его к нижнему регистру. */
  public static String normalizeLogin(String login) {
    if (login == null || login.isBlank()) {
      throw new ValidationException("Логин не может быть пустым");
    }
    String normalized = login.trim().toLowerCase(Locale.ROOT);
    if (!LOGIN.matcher(normalized).matches()) {
      throw new ValidationException(
          "Логин: от 3 до 32 символов, латинские буквы, цифры и символы _ . - "
              + "(начинается с буквы или цифры)");
    }
    return normalized;
  }

  public static void requirePassword(String password) {
    if (password == null || password.isBlank()) {
      throw new ValidationException("Пароль не может быть пустым");
    }
    if (password.chars().anyMatch(Character::isWhitespace)) {
      throw new ValidationException("Пароль не должен содержать пробелов");
    }
    if (password.length() < MIN_PASSWORD_LENGTH) {
      throw new ValidationException(
          "Пароль должен быть не короче " + MIN_PASSWORD_LENGTH + " символов");
    }
  }

  /**
   * Проверяет название категории. Запятая запрещена, потому что ею разделяются категории в команде
   * stats, а «->» используется в команде переименования.
   */
  public static String requireCategoryName(String name) {
    if (name == null || name.isBlank()) {
      throw new ValidationException("Название категории не может быть пустым");
    }
    String trimmed = name.trim().replaceAll("\\s+", " ");
    if (trimmed.length() > MAX_CATEGORY_LENGTH) {
      throw new ValidationException(
          "Название категории длиннее " + MAX_CATEGORY_LENGTH + " символов");
    }
    if (trimmed.contains(",") || trimmed.contains("->")) {
      throw new ValidationException("Название категории не может содержать «,» и «->»");
    }
    return trimmed;
  }

  /** Проверяет сумму и приводит её к двум знакам после точки. */
  public static BigDecimal requireAmount(BigDecimal amount) {
    if (amount == null) {
      throw new ValidationException("Сумма не указана");
    }
    if (amount.signum() <= 0) {
      throw new ValidationException("Сумма должна быть больше нуля");
    }
    if (amount.stripTrailingZeros().scale() > 2) {
      throw new ValidationException("У суммы не может быть больше двух знаков после точки");
    }
    return amount.setScale(2, RoundingMode.UNNECESSARY);
  }

  /** Разбирает сумму из текста. Допускается и точка, и запятая: 99.90 или 99,90. */
  public static BigDecimal parseAmount(String text) {
    if (text == null || text.isBlank()) {
      throw new ValidationException("Сумма не указана");
    }
    BigDecimal value;
    try {
      value = new BigDecimal(text.trim().replace(',', '.'));
    } catch (NumberFormatException e) {
      throw new ValidationException("«" + text + "» - не число. Пример суммы: 1500 или 99.90");
    }
    return requireAmount(value);
  }

  public static boolean looksLikeDate(String text) {
    return text != null && DATE.matcher(text.trim()).matches();
  }

  public static LocalDate parseDate(String text) {
    try {
      return LocalDate.parse(text.trim());
    } catch (DateTimeParseException | NullPointerException e) {
      throw new ValidationException(
          "Неверная дата «" + text + "». Формат ГГГГ-ММ-ДД, например 2026-11-20");
    }
  }
}
