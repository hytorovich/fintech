package org.example.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.core.error.ValidationException;
import org.example.core.model.OperationType;
import org.example.core.service.Period;
import org.example.core.service.Validator;

/**
 * Разбор аргументов команд. Названия категорий могут содержать пробелы, поэтому сумма и дата
 * берутся с конца строки, а всё, что перед ними, считается названием.
 */
final class CommandArgs {

  private CommandArgs() {}

  /** Команда и строка аргументов после неё. */
  record CommandLine(String name, String args) {
    static CommandLine parse(String line) {
      String trimmed = line.trim();
      int space = trimmed.indexOf(' ');
      if (space < 0) {
        return new CommandLine(trimmed.toLowerCase(Locale.ROOT), "");
      }
      return new CommandLine(
          trimmed.substring(0, space).toLowerCase(Locale.ROOT),
          trimmed.substring(space + 1).trim());
    }
  }

  record Operation(String category, BigDecimal amount, LocalDate date) {}

  record NameAndType(String name, OperationType type) {}

  record Rename(String oldName, String newName) {}

  record Filter(List<String> categories, Period period) {}

  /** {@code <категория> <сумма> [дата]}. */
  static Operation operation(String args, String usage) {
    List<String> tokens = tokens(args);
    LocalDate date = null;
    if (!tokens.isEmpty() && Validator.looksLikeDate(last(tokens))) {
      date = Validator.parseDate(removeLast(tokens));
    }
    if (tokens.size() < 2) {
      throw new ValidationException("Не хватает аргументов. Пример: " + usage);
    }
    BigDecimal amount = Validator.parseAmount(removeLast(tokens));
    return new Operation(String.join(" ", tokens), amount, date);
  }

  /** {@code <название> <income|expense>}. */
  static NameAndType nameAndType(String args, String usage) {
    List<String> tokens = tokens(args);
    if (tokens.size() < 2) {
      throw new ValidationException("Не хватает аргументов. Пример: " + usage);
    }
    OperationType type = OperationType.parse(removeLast(tokens));
    return new NameAndType(String.join(" ", tokens), type);
  }

  /** {@code <категория> <сумма>}. */
  static Operation categoryAndAmount(String args, String usage) {
    List<String> tokens = tokens(args);
    if (tokens.size() < 2) {
      throw new ValidationException("Не хватает аргументов. Пример: " + usage);
    }
    BigDecimal amount = Validator.parseAmount(removeLast(tokens));
    return new Operation(String.join(" ", tokens), amount, null);
  }

  /** {@code <старое название> -> <новое название>}. */
  static Rename rename(String args, String usage) {
    String[] parts = args.split("->", -1);
    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
      throw new ValidationException("Неверный формат. Пример: " + usage);
    }
    return new Rename(parts[0].trim(), parts[1].trim());
  }

  /** {@code [категория, категория...] [--from ГГГГ-ММ-ДД] [--to ГГГГ-ММ-ДД]}. */
  static Filter filter(String args) {
    List<String> tokens = tokens(args);
    LocalDate from = null;
    LocalDate to = null;
    List<String> words = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      if (token.equals("--from") || token.equals("--to")) {
        if (i + 1 >= tokens.size()) {
          throw new ValidationException("После " + token + " укажите дату ГГГГ-ММ-ДД");
        }
        LocalDate date = Validator.parseDate(tokens.get(++i));
        if (token.equals("--from")) {
          from = date;
        } else {
          to = date;
        }
      } else {
        words.add(token);
      }
    }
    List<String> categories =
        words.isEmpty()
            ? List.of()
            : Arrays.stream(String.join(" ", words).split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    return new Filter(categories, new Period(from, to));
  }

  /** Ровно {@code count} аргументов без пробелов внутри. */
  static List<String> exactly(String args, int count, String usage) {
    List<String> tokens = tokens(args);
    if (tokens.size() != count) {
      throw new ValidationException("Неверное число аргументов. Пример: " + usage);
    }
    return tokens;
  }

  /** Один аргумент, который может содержать пробелы (название категории, путь к файлу). */
  static String required(String args, String usage) {
    if (args == null || args.isBlank()) {
      throw new ValidationException("Не хватает аргумента. Пример: " + usage);
    }
    return args.trim();
  }

  private static List<String> tokens(String args) {
    if (args == null || args.isBlank()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(Arrays.asList(args.trim().split("\\s+")));
  }

  private static String last(List<String> tokens) {
    return tokens.get(tokens.size() - 1);
  }

  private static String removeLast(List<String> tokens) {
    return tokens.remove(tokens.size() - 1);
  }
}
