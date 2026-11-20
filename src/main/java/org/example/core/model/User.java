package org.example.core.model;

/** Пользователь приложения. Пароль хранится только в виде хеша с солью. */
public class User {

  private String login;
  private String passwordHash;

  /** Нужен для загрузки из JSON. */
  private User() {}

  public User(String login, String passwordHash) {
    this.login = login;
    this.passwordHash = passwordHash;
  }

  public String getLogin() {
    return login;
  }

  public String getPasswordHash() {
    return passwordHash;
  }
}
