package org.example;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import org.example.cli.AppServices;
import org.example.cli.Console;
import org.example.core.error.StorageException;
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

/**
 * Точка входа: собирает зависимости и запускает консоль. Папку с данными можно передать первым
 * аргументом, по умолчанию это {@code data} в текущей папке.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    Path dataDir = Path.of(args.length > 0 ? args[0] : "data");

    UserRepository users;
    try {
      users = new JsonUserRepository(dataDir.resolve("users.json"));
    } catch (StorageException e) {
      System.err.println("Не удалось запустить программу: " + e.getMessage());
      return;
    }
    WalletRepository wallets = new JsonWalletRepository(dataDir.resolve("wallets"));
    NotificationService notifications = new NotificationService();
    FinanceService finance = new FinanceService(Clock.systemDefaultZone(), notifications);

    AppServices services =
        new AppServices(
            new AuthService(users),
            finance,
            new StatsService(),
            new TransferService(users, wallets, finance),
            notifications,
            wallets,
            new CsvTransactionFile());

    // Явно UTF-8, чтобы кириллица читалась и печаталась одинаково в любом терминале.
    BufferedReader in =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    PrintStream out =
        new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
    Console console = new Console(in, out, services);
    console.run();
  }
}
