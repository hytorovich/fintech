package org.example.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.core.error.StorageException;
import org.example.core.model.User;
import org.example.core.repository.UserRepository;

/**
 * Пользователи в одном JSON-файле. При запуске файл читается в память, при регистрации нового
 * пользователя перезаписывается целиком.
 */
public class JsonUserRepository implements UserRepository {

  private final Path file;
  private final ObjectMapper mapper = JsonSupport.createMapper();
  private final Map<String, User> users = new HashMap<>();

  public JsonUserRepository(Path file) {
    this.file = file;
    if (Files.exists(file)) {
      try {
        List<User> loaded = mapper.readValue(file.toFile(), new TypeReference<List<User>>() {});
        loaded.forEach(user -> users.put(user.getLogin(), user));
      } catch (IOException e) {
        throw new StorageException("Файл пользователей повреждён: " + file, e);
      }
    }
  }

  @Override
  public Optional<User> findByLogin(String login) {
    return Optional.ofNullable(users.get(login));
  }

  @Override
  public boolean exists(String login) {
    return users.containsKey(login);
  }

  @Override
  public void save(User user) {
    users.put(user.getLogin(), user);
    try {
      Files.createDirectories(file.toAbsolutePath().getParent());
      mapper.writeValue(file.toFile(), new ArrayList<>(users.values()));
    } catch (IOException e) {
      throw new StorageException("Не удалось сохранить пользователей в " + file, e);
    }
  }
}
