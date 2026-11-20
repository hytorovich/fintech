package org.example.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.example.core.model.User;
import org.example.core.repository.UserRepository;

/** Хранилище пользователей в памяти для тестов сервисов без файлов. */
public class InMemoryUserRepository implements UserRepository {

  private final Map<String, User> users = new HashMap<>();

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
  }
}
