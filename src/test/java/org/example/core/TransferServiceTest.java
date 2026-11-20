package org.example.core;

import static org.example.support.TestData.income;
import static org.example.support.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.example.core.error.ValidationException;
import org.example.core.model.Transaction;
import org.example.core.model.User;
import org.example.core.model.Wallet;
import org.example.core.service.FinanceService;
import org.example.core.service.TransferService;
import org.example.support.InMemoryUserRepository;
import org.example.support.InMemoryWalletRepository;
import org.example.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferServiceTest {

  private InMemoryWalletRepository wallets;
  private TransferService transfers;
  private Wallet sender;

  @BeforeEach
  void setUp() {
    InMemoryUserRepository users = new InMemoryUserRepository();
    users.save(new User("nikolay", "hash"));
    users.save(new User("anna", "hash"));
    wallets = new InMemoryWalletRepository();
    FinanceService finance = TestData.finance();
    transfers = new TransferService(users, wallets, finance);
    sender = wallets.load("nikolay");
    income(finance, sender, "Зарплата", "5000");
  }

  @Test
  void transferCreatesExpenseForSenderAndIncomeForReceiver() {
    transfers.transfer(sender, "Anna", money("1500"));

    Wallet receiver = wallets.load("anna");
    assertEquals(0, money("3500").compareTo(sender.balance()));
    assertEquals(0, money("1500").compareTo(receiver.balance()));
    Transaction received = receiver.getTransactions().get(0);
    assertTrue(received.isIncome());
    assertEquals("nikolay", received.counterparty());
  }

  @Test
  void transferToUnknownUserIsRejected() {
    assertThrows(ValidationException.class, () -> transfers.transfer(sender, "ghost", money("10")));
  }

  @Test
  void transferToSelfIsRejected() {
    assertThrows(
        ValidationException.class, () -> transfers.transfer(sender, "nikolay", money("10")));
  }

  @Test
  void transferMoreThanBalanceIsRejected() {
    assertThrows(
        ValidationException.class, () -> transfers.transfer(sender, "anna", money("5000.01")));
    assertEquals(1, sender.getTransactions().size());
  }
}
