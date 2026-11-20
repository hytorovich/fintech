package org.example.infra;

import static org.example.support.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.core.error.ValidationException;
import org.example.core.model.OperationType;
import org.example.core.model.Transaction;
import org.example.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvTransactionFileTest {

  private final CsvTransactionFile csv = new CsvTransactionFile();

  @TempDir Path dir;

  @Test
  void exportThenImportGivesSameOperations() {
    List<Transaction> original =
        List.of(
            new Transaction(
                OperationType.EXPENSE,
                "Коммунальные услуги",
                money("3000.00"),
                TestData.TODAY,
                null),
            new Transaction(
                OperationType.INCOME,
                "Переводы полученные",
                money("10.50"),
                TestData.TODAY,
                "anna"));
    Path file = dir.resolve("ops.csv");

    csv.write(original, file);
    CsvTransactionFile.ReadResult result = csv.read(file);

    assertEquals(original, new ArrayList<>(result.rows().values()));
    assertTrue(result.errors().isEmpty());
  }

  @Test
  void badRowsAreReportedWithLineNumbers() throws IOException {
    Path file = dir.resolve("bad.csv");
    Files.writeString(
        file,
        String.join(
            "\n",
            CsvTransactionFile.HEADER,
            "2026-11-01,EXPENSE,Еда,300,",
            "2026-13-01,EXPENSE,Еда,300,",
            "2026-11-01,GIFT,Еда,300,",
            "2026-11-01,EXPENSE,Еда,abc,",
            "2026-11-01,EXPENSE,Еда"));

    CsvTransactionFile.ReadResult result = csv.read(file);

    assertEquals(1, result.rows().size());
    assertEquals(4, result.errors().size());
    assertTrue(result.errors().get(0).startsWith("Строка 3"));
  }

  @Test
  void fileWithoutHeaderIsRejected() throws IOException {
    Path file = dir.resolve("noheader.csv");
    Files.writeString(file, "2026-11-01,EXPENSE,Еда,300,\n");

    assertThrows(ValidationException.class, () -> csv.read(file));
  }
}
