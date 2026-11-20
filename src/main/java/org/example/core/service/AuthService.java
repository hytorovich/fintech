package org.example.core.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.example.core.error.ValidationException;
import org.example.core.model.User;
import org.example.core.repository.UserRepository;

/** Регистрация и вход по логину и паролю. */
public class AuthService {

  private final UserRepository users;

  public AuthService(UserRepository users) {
    this.users = users;
  }

  public User register(String login, String password) {
    String normalized = Validator.normalizeLogin(login);
    Validator.requirePassword(password);
    if (users.exists(normalized)) {
      throw new ValidationException("Пользователь «" + normalized + "» уже существует");
    }
    User user = new User(normalized, hash(password));
    users.save(user);
    return user;
  }

  /** Возвращает пользователя. При любой ошибке сообщение одинаковое, чтобы не подсказывать. */
  public User login(String login, String password) {
    if (login == null || login.isBlank() || password == null || password.isBlank()) {
      throw new ValidationException("Введите логин и пароль: login <логин> <пароль>");
    }
    String normalized = login.trim().toLowerCase(Locale.ROOT);
    return users
        .findByLogin(normalized)
        .filter(user -> user.getPasswordHash().equals(hash(password)))
        .orElseThrow(() -> new ValidationException("Неверный логин или пароль"));
  }

  /** Пароль хранится в файле не открытым текстом, а как хеш SHA-256. */
  static String hash(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 недоступен", e);
    }
  }
}
