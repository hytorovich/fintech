package org.example.cli;

import org.example.core.repository.WalletRepository;
import org.example.core.service.AuthService;
import org.example.core.service.FinanceService;
import org.example.core.service.NotificationService;
import org.example.core.service.StatsService;
import org.example.core.service.TransferService;
import org.example.infra.CsvTransactionFile;

/** Все сервисы, которые нужны консоли. Собираются в {@code Main} или в тестах. */
public record AppServices(
    AuthService auth,
    FinanceService finance,
    StatsService stats,
    TransferService transfers,
    NotificationService notifications,
    WalletRepository wallets,
    CsvTransactionFile csv) {}
