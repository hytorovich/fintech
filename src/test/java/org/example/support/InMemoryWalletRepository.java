package org.example.support;

import java.util.HashMap;
import java.util.Map;
import org.example.core.model.Wallet;
import org.example.core.repository.WalletRepository;

/** Хранилище кошельков в памяти для тестов сервисов без файлов. */
public class InMemoryWalletRepository implements WalletRepository {

  private final Map<String, Wallet> wallets = new HashMap<>();

  @Override
  public Wallet load(String owner) {
    return wallets.computeIfAbsent(owner, Wallet::new);
  }

  @Override
  public void save(Wallet wallet) {
    wallets.put(wallet.getOwner(), wallet);
  }
}
