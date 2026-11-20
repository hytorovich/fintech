package org.example.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.example.core.error.ValidationException;
import org.example.core.model.User;
import org.example.core.service.AuthService;
import org.example.support.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuthServiceTest {

  private InMemoryUserRepository users;
  private AuthService auth;

  @BeforeEach
  void setUp() {
    users = new InMemoryUserRepository();
    auth = new AuthService(users);
  }

  @Test
  void registerSavesUserWithLowercaseLogin() {
    User user = auth.register("Nikolay", "secret1");

    assertEquals("nikolay", user.getLogin());
    assertTrue(users.exists("nikolay"));
  }

  @Test
  void passwordIsStoredAsHash() {
    User user = auth.register("anna", "secret1");

    assertFalse(user.getPasswordHash().contains("secret1"));
  }

  @Test
  void registerWithExistingLoginThrows() {
    auth.register("anna", "secret1");

    assertThrows(ValidationException.class, () -> auth.register("ANNA", "other1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "ab", "имя", "with space", "-dash", "a/b/c"})
  void registerRejectsInvalidLogin(String login) {
    assertThrows(ValidationException.class, () -> auth.register(login, "secret1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abc", "two words"})
  void registerRejectsInvalidPassword(String password) {
    assertThrows(ValidationException.class, () -> auth.register("anna", password));
  }

  @Test
  void loginWithCorrectPasswordReturnsUser() {
    auth.register("anna", "secret1");

    assertEquals("anna", auth.login("Anna", "secret1").getLogin());
  }

  @Test
  void loginWithWrongPasswordThrows() {
    auth.register("anna", "secret1");

    ValidationException e =
        assertThrows(ValidationException.class, () -> auth.login("anna", "wrong"));
    assertEquals("Неверный логин или пароль", e.getMessage());
  }

  @Test
  void loginOfUnknownUserThrowsSameMessage() {
    ValidationException e =
        assertThrows(ValidationException.class, () -> auth.login("ghost", "secret1"));
    assertEquals("Неверный логин или пароль", e.getMessage());
  }

  @Test
  void severalUsersCanBeRegistered() {
    auth.register("anna", "secret1");
    auth.register("boris", "secret2");

    assertEquals("anna", auth.login("anna", "secret1").getLogin());
    assertEquals("boris", auth.login("boris", "secret2").getLogin());
  }
}
