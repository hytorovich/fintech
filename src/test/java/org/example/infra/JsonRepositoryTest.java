package org.example.infra;

import static org.example.support.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.example.core.error.StorageException;
import org.example.core.model.User;
import org.example.core.model.Wallet;
import org.example.core.service.FinanceService;
import org.example.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonRepositoryTest {

  @TempDir Path dir;

  @Test
  void walletSurvivesSaveAndLoad() {
    FinanceService finance = TestData.finance();
    Wallet original = TestData.exampleWallet(finance);
    JsonWalletRepository repository = new JsonWalletRepository(dir);

    repository.save(original);
    Wallet loaded = new JsonWalletRepository(dir).load("nikolay");

    assertEquals(original.getTransactions(), loaded.getTransactions());
    assertEquals(original.getCategories().size(), loaded.getCategories().size());
    assertEquals(0, money("4000").compareTo(loaded.findCategory("Еда").get().getBudget()));
    assertFalse(loaded.findCategory("Такси").get().hasBudget());
  }

  @Test
  void missingWalletFileGivesEmptyWallet() {
    Wallet wallet = new JsonWalletRepository(dir).load("new-user");

    assertEquals("new-user", wallet.getOwner());
    assertTrue(wallet.getTransactions().isEmpty());
  }

  @Test
  void usersAreReadBackAfterRestart() {
    Path file = dir.resolve("users.json");
    new JsonUserRepository(file).save(new User("anna", "salt:hash"));

    JsonUserRepository reopened = new JsonUserRepository(file);

    assertTrue(reopened.exists("anna"));
    assertEquals("salt:hash", reopened.findByLogin("anna").get().getPasswordHash());
  }

  @Test
  void corruptedWalletFileGivesClearError() throws IOException {
    Files.writeString(dir.resolve("anna.json"), "{ это не json");

    StorageException e =
        assertThrows(StorageException.class, () -> new JsonWalletRepository(dir).load("anna"));
    assertTrue(e.getMessage().contains("повреждён"));
  }
}
