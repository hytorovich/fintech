package org.example.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.example.core.repository.UserRepository;
import org.example.core.repository.WalletRepository;
import org.example.core.service.AuthService;
import org.example.core.service.FinanceService;
import org.example.core.service.NotificationService;
import org.example.core.service.StatsService;
import org.example.core.service.TransferService;
import org.example.infra.CsvTransactionFile;
import org.example.infra.JsonUserRepository;
import org.example.infra.JsonWalletRepository;
import org.example.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Сквозные тесты: настоящие JSON-файлы во временной папке и консоль с подменённым вводом. */
class ConsoleIntegrationTest {

  @TempDir Path dir;

  @Test
  void exampleFromAssignmentAndDataSurvivesRestart() {
    String first =
        run(
            "register nikolay pass1",
            "login nikolay pass1",
            "expense Еда 300",
            "expense Еда 500",
            "expense Развлечения 3000",
            "expense Коммунальные услуги 3000",
            "expense Такси 1500",
            "income Зарплата 20000",
            "income Зарплата 40000",
            "income Бонус 3000",
            "budget Еда 4000",
            "budget Развлечения 3000",
            "budget Коммунальные услуги 2500",
            "stats",
            "exit");

    assertTrue(first.contains("Общий доход: 63,000.00"));
    assertTrue(first.contains("Общие расходы: 8,300.00"));
    assertTrue(first.contains("[!] Превышен бюджет «Коммунальные услуги»"));
    assertTrue(first.matches("(?s).*Коммунальные услуги\\s+2,500.00\\s+3,000.00\\s+-500.00.*"));
    assertTrue(first.matches("(?s).*Еда\\s+4,000.00\\s+800.00\\s+3,200.00.*"));

    // Новый запуск программы: данные загружаются из файла при входе.
    String second = run("login nikolay pass1", "balance", "exit");
    assertTrue(second.contains("Баланс: 54,700.00"));
  }

  @Test
  void errorsAreReportedWithoutCrashing() {
    String output =
        run(
            "balance",
            "unknown",
            "register nikolay pass1",
            "login nikolay wrong",
            "login nikolay pass1",
            "expense Еда abc",
            "expense",
            "stats Нет такой",
            "transfer ghost 10",
            "exit");

    assertTrue(output.contains("Сначала войдите"));
    assertTrue(output.contains("Неизвестная команда «unknown»"));
    assertTrue(output.contains("Неверный логин или пароль"));
    assertTrue(output.contains("«abc» - не число"));
    assertTrue(output.contains("Категория «Нет такой» не найдена"));
    assertTrue(output.contains("Пользователь «ghost» не найден"));
    assertFalse(output.contains("Непредвиденная ошибка"));
  }

  @Test
  void endOfInputSavesWallet() {
    run("register anna secret1", "login anna secret1", "income Зарплата 100");

    assertTrue(run("login anna secret1", "balance").contains("Баланс: 100.00"));
  }

  @Test
  void transferReportAndCsvRoundTrip() throws IOException {
    Path report = dir.resolve("report.txt");
    Path csv = dir.resolve("ops.csv");
    run(
        "register anna secret1",
        "register boris secret2",
        "login anna secret1",
        "income Зарплата 1000",
        "transfer boris 400",
        "report " + report,
        "export " + csv,
        "logout",
        "login boris secret2",
        "import " + csv,
        "exit");

    assertTrue(Files.readString(report).contains("Общие расходы: 400.00"));
    String boris = run("login boris secret2", "balance", "exit");
    // 400 перевод + 1000 и 400 из импортированного файла Анны.
    assertTrue(boris.contains("доходы 1,400.00, расходы 400.00"), boris);
  }

  private String run(String... commands) {
    UserRepository users = new JsonUserRepository(dir.resolve("data/users.json"));
    WalletRepository wallets = new JsonWalletRepository(dir.resolve("data/wallets"));
    NotificationService notifications = new NotificationService();
    FinanceService finance = new FinanceService(TestData.CLOCK, notifications);
    AppServices services =
        new AppServices(
            new AuthService(users),
            finance,
            new StatsService(),
            new TransferService(users, wallets, finance),
            notifications,
            wallets,
            new CsvTransactionFile());

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    BufferedReader in = new BufferedReader(new StringReader(String.join("\n", commands)));
    PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
    new Console(in, out, services).run();
    return buffer.toString(StandardCharsets.UTF_8);
  }
}
