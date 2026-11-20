package org.example.core.repository;

import org.example.core.model.Wallet;

/** Хранилище кошельков. Один кошелёк на пользователя, ключ - логин владельца. */
public interface WalletRepository {

  /** Загружает кошелёк. Если сохранённого кошелька ещё нет, возвращает пустой. */
  Wallet load(String owner);

  void save(Wallet wallet);
}
