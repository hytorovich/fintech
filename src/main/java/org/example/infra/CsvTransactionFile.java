package org.example.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.core.error.StorageException;
import org.example.core.error.ValidationException;
import org.example.core.model.OperationType;
import org.example.core.model.Transaction;
import org.example.core.service.Validator;

/**
 * Экспорт и импорт операций в CSV. Формат: {@code date,type,category,amount,counterparty}, первая
 * строка - заголовок. Запятые в названиях категорий запрещены, поэтому кавычки не нужны.
 */
public class CsvTransactionFile {

  static final String HEADER = "date,type,category,amount,counterparty";

  /** Результат чтения: корректные строки по номерам и ошибки разбора. */
  public record ReadResult(Map<Integer, Transaction> rows, List<String> errors) {}

  public void write(List<Transaction> transactions, Path file) {
    List<String> lines = new ArrayList<>();
    lines.add(HEADER);
    for (Transaction t : transactions) {
      String counterparty = t.counterparty() == null ? "" : t.counterparty();
      lines.add(
          String.join(
              ",",
              t.date().toString(),
              t.type().name(),
              t.category(),
              t.amount().toPlainString(),
              counterparty));
    }
    try {
      Files.write(file, lines, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new StorageException("Не удалось записать файл " + file, e);
    }
  }

  public ReadResult read(Path file) {
    List<String> lines;
    try {
      lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (NoSuchFileException e) {
      throw new ValidationException("Файл не найден: " + file);
    } catch (IOException e) {
      throw new StorageException("Не удалось прочитать файл " + file, e);
    }
    if (lines.isEmpty() || !lines.get(0).trim().equals(HEADER)) {
      throw new ValidationException("Неверный формат CSV: первая строка должна быть " + HEADER);
    }

    Map<Integer, Transaction> rows = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      if (lines.get(i).isBlank()) {
        continue;
      }
      int lineNumber = i + 1;
      try {
        rows.put(lineNumber, parseRow(lines.get(i)));
      } catch (ValidationException e) {
        errors.add("Строка " + lineNumber + ": " + e.getMessage());
      }
    }
    return new ReadResult(rows, errors);
  }

  private static Transaction parseRow(String line) {
    String[] fields = line.split(",", -1);
    if (fields.length != 5) {
      throw new ValidationException("ожидается 5 полей, а найдено " + fields.length);
    }
    String counterparty = fields[4].isBlank() ? null : fields[4].trim();
    return new Transaction(
        OperationType.parse(fields[1]),
        Validator.requireCategoryName(fields[2]),
        Validator.parseAmount(fields[3]),
        Validator.parseDate(fields[0]),
        counterparty);
  }
}
