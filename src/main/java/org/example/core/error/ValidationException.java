package org.example.core.error;

/** Ошибка во входных данных или в логике операции. Сообщение показывается пользователю как есть. */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }
}
