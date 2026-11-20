package org.example.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.cli.CommandArgs.CommandLine;
import org.example.cli.CommandArgs.Filter;
import org.example.cli.CommandArgs.NameAndType;
import org.example.cli.CommandArgs.Operation;
import org.example.cli.CommandArgs.Rename;
import org.example.core.error.StorageException;
import org.example.core.error.ValidationException;
import org.example.core.model.Money;
import org.example.core.model.OperationType;
import org.example.core.model.User;
import org.example.core.model.Wallet;
import org.example.core.service.FinanceService;
import org.example.core.service.Period;
import org.example.core.service.Validator;
import org.example.infra.CsvTransactionFile;

/**
 * Консольный интерфейс: читает команды в цикле, вызывает сервисы и печатает результат.
 * Бизнес-логики здесь нет, только разбор ввода и вывод.
 */
public class Console {

  private final BufferedReader in;
  private final PrintStream out;
  private final AppServices services;

  /** Текущая сессия: вошедший пользователь и его кошелёк. */
  private User user;

  private Wallet wallet;

  public Console(BufferedReader in, PrintStream out, AppServices services) {
    this.in = in;
    this.out = out;
    this.services = services;
  }

  /** Главный цикл. Завершается командой exit или концом ввода (Ctrl+D). */
  public void run() {
    out.println("Учёт личных финансов. Введите help, чтобы увидеть список команд.");
    while (true) {
      out.print(user == null ? "> " : user.getLogin() + "> ");
      out.flush();
      String line = readLine();
      if (line == null) {
        out.println();
        exit();
        return;
      }
      if (line.isBlank()) {
        continue;
      }
      CommandLine command = CommandLine.parse(line);
      if (command.name().equals("exit")) {
        if (exit()) {
          return;
        }
        continue;
      }
      execute(command);
    }
  }

  private void execute(CommandLine command) {
    try {
      dispatch(command.name(), command.args());
    } catch (ValidationException e) {
      out.println("Ошибка: " + e.getMessage());
    } catch (StorageException e) {
      out.println("Ошибка хранения данных: " + e.getMessage());
    } catch (RuntimeException e) {
      out.println("Непредвиденная ошибка: " + e);
    }
  }

  private void dispatch(String name, String args) {
    switch (name) {
      case "help" -> out.print(ReportFormatter.help());
      case "register" -> register(args);
      case "login" -> login(args);
      case "logout" -> logout();
      case "income" -> addOperation(OperationType.INCOME, args, "income Зарплата 40000");
      case "expense" -> addOperation(OperationType.EXPENSE, args, "expense Еда 300");
      case "add-category" -> addCategory(args);
      case "rename-category" -> renameCategory(args);
      case "delete-category" -> deleteCategory(args);
      case "budget" -> setBudget(args);
      case "remove-budget" -> removeBudget(args);
      case "categories" -> out.print(ReportFormatter.categories(requireWallet()));
      case "balance" -> balance();
      case "stats" -> stats(args);
      case "history" -> history(args);
      case "report" -> report(args);
      case "transfer" -> transfer(args);
      case "export" -> exportCsv(args);
      case "import" -> importCsv(args);
      default ->
          out.println(
              "Неизвестная команда «" + name + "». Введите help, чтобы увидеть список команд");
    }
  }

  private void register(String args) {
    requireLoggedOut();
    List<String> parts = CommandArgs.exactly(args, 2, "register anna secret1");
    User created = services.auth().register(parts.get(0), parts.get(1));
    out.println(
        "Пользователь "
            + created.getLogin()
            + " зарегистрирован. Войдите: login "
            + created.getLogin()
            + " <пароль>");
  }

  private void login(String args) {
    requireLoggedOut();
    List<String> parts = CommandArgs.exactly(args, 2, "login anna secret1");
    User found = services.auth().login(parts.get(0), parts.get(1));
    wallet = services.wallets().load(found.getLogin());
    user = found;
    out.println("Вы вошли как " + user.getLogin() + ". Баланс: " + Money.format(wallet.balance()));
    print(services.notifications().overview(wallet));
  }

  private void logout() {
    requireWallet();
    services.wallets().save(wallet);
    out.println("Кошелёк сохранён. Вы вышли из аккаунта " + user.getLogin());
    user = null;
    wallet = null;
  }

  /** Возвращает {@code true}, если можно завершать программу. */
  private boolean exit() {
    if (wallet != null) {
      try {
        services.wallets().save(wallet);
        out.println("Кошелёк сохранён.");
      } catch (StorageException e) {
        out.println("Ошибка хранения данных: " + e.getMessage());
        out.println("Данные не сохранены. Исправьте проблему и повторите exit.");
        return false;
      }
      user = null;
      wallet = null;
    }
    out.println("Программа завершена.");
    return true;
  }

  private void addOperation(OperationType type, String args, String usage) {
    Wallet current = requireWallet();
    Operation op = CommandArgs.operation(args, usage + " [2026-11-20]");
    List<String> messages =
        services.finance().addOperation(current, type, op.category(), op.amount(), op.date());
    out.println(
        "Добавлен %s: %s %s".formatted(type.title(), op.category(), Money.format(op.amount())));
    print(messages);
  }

  private void addCategory(String args) {
    NameAndType input = CommandArgs.nameAndType(args, "add-category Такси expense");
    var category = services.finance().addCategory(requireWallet(), input.name(), input.type());
    out.println(
        "Категория «%s» (%s) создана".formatted(category.getName(), category.getType().title()));
  }

  private void renameCategory(String args) {
    Rename input = CommandArgs.rename(args, "rename-category Еда -> Продукты");
    services.finance().renameCategory(requireWallet(), input.oldName(), input.newName());
    out.println("Категория «%s» переименована в «%s»".formatted(input.oldName(), input.newName()));
  }

  private void deleteCategory(String args) {
    String name = CommandArgs.required(args, "delete-category Такси");
    services.finance().deleteCategory(requireWallet(), name);
    out.println("Категория «" + name + "» удалена");
  }

  private void setBudget(String args) {
    Operation input = CommandArgs.categoryAndAmount(args, "budget Еда 4000");
    print(services.finance().setBudget(requireWallet(), input.category(), input.amount()));
  }

  private void removeBudget(String args) {
    String name = CommandArgs.required(args, "remove-budget Еда");
    services.finance().removeBudget(requireWallet(), name);
    out.println("Бюджет категории «" + name + "» удалён");
  }

  private void balance() {
    Wallet current = requireWallet();
    out.println(
        "Баланс: %s (доходы %s, расходы %s)"
            .formatted(
                Money.format(current.balance()),
                Money.format(current.totalIncome()),
                Money.format(current.totalExpense())));
  }

  private void stats(String args) {
    Wallet current = requireWallet();
    Filter filter = CommandArgs.filter(args);
    if (filter.categories().isEmpty()) {
      out.print(
          ReportFormatter.summary(
              user.getLogin(), services.stats().summary(current, filter.period())));
    } else {
      out.print(
          ReportFormatter.selection(
              services.stats().select(current, filter.categories(), filter.period())));
    }
  }

  private void history(String args) {
    Wallet current = requireWallet();
    Period period = CommandArgs.filter(args).period();
    out.print(ReportFormatter.history(services.stats().history(current, period)));
  }

  private void report(String args) {
    Wallet current = requireWallet();
    String usage = "report report.txt --from 2026-11-01";
    String[] parts = CommandArgs.required(args, usage).split("\\s+", 2);
    Path file = Path.of(parts[0]);
    Period period = CommandArgs.filter(parts.length > 1 ? parts[1] : "").period();
    String text =
        ReportFormatter.summary(user.getLogin(), services.stats().summary(current, period));
    try {
      Path parent = file.toAbsolutePath().getParent();
      Files.createDirectories(parent);
      Files.writeString(file, text, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new StorageException("Не удалось записать отчёт в " + file + ": " + e.getMessage(), e);
    }
    out.println("Отчёт сохранён в " + file.toAbsolutePath());
  }

  private void transfer(String args) {
    Wallet current = requireWallet();
    List<String> parts = CommandArgs.exactly(args, 2, "transfer anna 1500");
    print(
        services.transfers().transfer(current, parts.get(0), Validator.parseAmount(parts.get(1))));
  }

  private void exportCsv(String args) {
    Wallet current = requireWallet();
    Path file = Path.of(CommandArgs.required(args, "export operations.csv"));
    services.csv().write(services.stats().history(current, Period.ALL), file);
    out.println(
        "Выгружено операций: %d в %s"
            .formatted(current.getTransactions().size(), file.toAbsolutePath()));
  }

  private void importCsv(String args) {
    Wallet current = requireWallet();
    Path file = Path.of(CommandArgs.required(args, "import operations.csv"));
    CsvTransactionFile.ReadResult read = services.csv().read(file);
    FinanceService.ImportReport report =
        services.finance().importTransactions(current, read.rows());
    out.println("Импортировано операций: " + report.imported());
    int skipped = read.errors().size() + report.errors().size();
    if (skipped > 0) {
      out.println("Пропущено строк с ошибками: " + skipped);
      read.errors().forEach(error -> out.println("  " + error));
      report.errors().forEach(error -> out.println("  " + error));
    }
    print(services.notifications().overview(current));
  }

  private Wallet requireWallet() {
    if (wallet == null) {
      throw new ValidationException("Сначала войдите: login <логин> <пароль>");
    }
    return wallet;
  }

  private void requireLoggedOut() {
    if (user != null) {
      throw new ValidationException(
          "Вы уже вошли как " + user.getLogin() + ". Сначала выполните logout");
    }
  }

  private void print(List<String> messages) {
    messages.forEach(message -> out.println(message.startsWith("[!]") ? message : "  " + message));
  }

  private String readLine() {
    try {
      return in.readLine();
    } catch (IOException e) {
      return null;
    }
  }
}
