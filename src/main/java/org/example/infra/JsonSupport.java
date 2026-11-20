package org.example.infra;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Общая настройка Jackson для всех JSON-хранилищ. */
final class JsonSupport {

  private JsonSupport() {}

  /**
   * Jackson читает и пишет поля классов напрямую, поэтому моделям не нужны сеттеры. Даты пишутся
   * строкой 2026-11-20, пустые поля не пишутся, JSON форматируется с отступами.
   */
  static ObjectMapper createMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT);
  }
}
