package org.example.core.repository;

import java.util.Optional;
import org.example.core.model.User;

/** Хранилище пользователей. Реализация может быть в памяти, в файле или в базе данных. */
public interface UserRepository {

  Optional<User> findByLogin(String login);

  boolean exists(String login);

  void save(User user);
}
