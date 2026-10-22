package org.tensorflow.lite.examples.shravan.env;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/** Master logging class. */
public final class Logger {
  private static final int DEFAULT_MIN_LOG_LEVEL = Log.DEBUG;

  private final String tag;
  private final String messagePrefix;
  private int minLogLevel = DEFAULT_MIN_LOG_LEVEL;

  public Logger(final Class<?> clazz) {
    this(clazz.getSimpleName());
  }

  public Logger(final String tag) {
    this(tag, null);
  }

  public Logger(final String tag, final String messagePrefix) {
    this.tag = tag;
    final String prefix = messagePrefix == null ? "" : messagePrefix;
    this.messagePrefix = prefix.length() > 0 ? prefix + ": " : prefix;
  }

  public Logger() {
    this("tensorflow", null);
  }

  public void setMinLogLevel(final int minLogLevel) {
    this.minLogLevel = minLogLevel;
  }

  public boolean isLoggable(final int logLevel) {
    return logLevel >= minLogLevel || Log.isLoggable(tag, logLevel);
  }

  private String formatMessage(final String format, final Object... args) {
    return messagePrefix + (args.length > 0 ? String.format(format, args) : format);
  }

  public void v(final String format, final Object... args) {
    if (isLoggable(Log.VERBOSE)) {
      Log.v(tag, formatMessage(format, args));
    }
  }

  public void v(final Throwable t, final String format, final Object... args) {
    if (isLoggable(Log.VERBOSE)) {
      Log.v(tag, formatMessage(format, args), t);
    }
  }

  public void d(final String format, final Object... args) {
    if (isLoggable(Log.DEBUG)) {
      Log.d(tag, formatMessage(format, args));
    }
  }

  public void d(final Throwable t, final String format, final Object... args) {
    if (isLoggable(Log.DEBUG)) {
      Log.d(tag, formatMessage(format, args), t);
    }
  }

  public void i(final String format, final Object... args) {
    if (isLoggable(Log.INFO)) {
      Log.i(tag, formatMessage(format, args));
    }
  }

  public void i(final Throwable t, final String format, final Object... args) {
    if (isLoggable(Log.INFO)) {
      Log.i(tag, formatMessage(format, args), t);
    }
  }

  public void w(final String format, final Object... args) {
    if (isLoggable(Log.WARN)) {
      Log.w(tag, formatMessage(format, args));
    }
  }

  public void w(final Throwable t, final String format, final Object... args) {
    if (isLoggable(Log.WARN)) {
      Log.w(tag, formatMessage(format, args), t);
    }
  }

  public void e(final String format, final Object... args) {
    if (isLoggable(Log.ERROR)) {
      Log.e(tag, formatMessage(format, args));
    }
  }

  public void e(final Throwable t, final String format, final Object... args) {
    if (isLoggable(Log.ERROR)) {
      Log.e(tag, formatMessage(format, args), t);
    }
  }
}
