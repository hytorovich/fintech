package org.example.core.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.core.error.ValidationException;
import org.example.core.model.Money;
import org.example.core.model.OperationType;
import org.example.core.model.Wallet;
import org.example.core.repository.UserRepository;
import org.example.core.repository.WalletRepository;

/**
 * Переводы между кошельками. У отправителя появляется расход, у получателя доход. Получатель
 * определяется по логину. Оба кошелька сохраняются сразу, потому что получатель не в сети.
 */
public class TransferService {

  public static final String OUTGOING_CATEGORY = "Переводы отправленные";
  public static final String INCOMING_CATEGORY = "Переводы полученные";

  private final UserRepository users;
  private final WalletRepository wallets;
  private final FinanceService finance;

  public TransferService(UserRepository users, WalletRepository wallets, FinanceService finance) {
    this.users = users;
    this.wallets = wallets;
    this.finance = finance;
  }

  public List<String> transfer(Wallet from, String toLogin, BigDecimal amount) {
    BigDecimal value = Validator.requireAmount(amount);
    if (toLogin == null || toLogin.isBlank()) {
      throw new ValidationException("Не указан получатель");
    }
    String receiver = toLogin.trim().toLowerCase(Locale.ROOT);
    if (receiver.equals(from.getOwner())) {
      throw new ValidationException("Нельзя перевести деньги самому себе");
    }
    if (!users.exists(receiver)) {
      throw new ValidationException("Пользователь «" + receiver + "» не найден");
    }
    if (from.balance().compareTo(value) < 0) {
      throw new ValidationException(
          "Недостаточно средств: баланс %s, сумма перевода %s"
              .formatted(Money.format(from.balance()), Money.format(value)));
    }

    Wallet to = wallets.load(receiver);
    // Проверяем обе стороны до изменений, чтобы перевод не прошёл «наполовину».
    finance.checkCategoryAccepts(from, OUTGOING_CATEGORY, OperationType.EXPENSE);
    finance.checkCategoryAccepts(to, INCOMING_CATEGORY, OperationType.INCOME);

    List<String> messages = new ArrayList<>();
    messages.add("Переведено %s пользователю %s".formatted(Money.format(value), receiver));
    messages.addAll(
        finance.addOperation(
            from, OperationType.EXPENSE, OUTGOING_CATEGORY, value, null, receiver));
    finance.addOperation(to, OperationType.INCOME, INCOMING_CATEGORY, value, null, from.getOwner());

    wallets.save(to);
    wallets.save(from);
    return messages;
  }
}
