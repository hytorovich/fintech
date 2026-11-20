package org.example.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.example.core.error.StorageException;
import org.example.core.model.Wallet;
import org.example.core.repository.WalletRepository;

/** Кошельки в отдельных JSON-файлах: {@code <папка>/<логин>.json}. */
public class JsonWalletRepository implements WalletRepository {

  private final Path directory;
  private final ObjectMapper mapper = JsonSupport.createMapper();

  public JsonWalletRepository(Path directory) {
    this.directory = directory;
  }

  @Override
  public Wallet load(String owner) {
    Path file = fileOf(owner);
    if (!Files.exists(file)) {
      return new Wallet(owner);
    }
    try {
      return mapper.readValue(file.toFile(), Wallet.class);
    } catch (IOException e) {
      throw new StorageException("Файл кошелька повреждён: " + file, e);
    }
  }

  @Override
  public void save(Wallet wallet) {
    Path file = fileOf(wallet.getOwner());
    try {
      Files.createDirectories(directory);
      mapper.writeValue(file.toFile(), wallet);
    } catch (IOException e) {
      throw new StorageException("Не удалось сохранить кошелёк в " + file, e);
    }
  }

  private Path fileOf(String owner) {
    return directory.resolve(owner + ".json");
  }
}
