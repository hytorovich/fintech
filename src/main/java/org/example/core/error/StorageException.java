package org.example.core.error;

/** Ошибка чтения или записи данных (файл недоступен, повреждён и т. п.). */
public class StorageException extends RuntimeException {

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
