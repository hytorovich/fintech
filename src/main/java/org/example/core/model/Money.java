package org.example.core.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Форматирование денежных сумм для вывода: 63,000.00. */
public final class Money {

  private Money() {}

  public static String format(BigDecimal amount) {
    DecimalFormat format =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    return format.format(amount);
  }
}
