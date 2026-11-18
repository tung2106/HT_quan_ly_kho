package com.vatlieu.service;

import java.util.Locale;

final class MobileNumber {

  private MobileNumber() {}

  static String login(String input) {
    String name = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    String compact = name.replace(" ", "");
    if (compact.matches("(?:0|\\+84)[35789][0-9]{8}")) {
      return "0" + compact.substring(compact.length() - 9);
    }
    return name;
  }

  static String require(String input) {
    String phone = login(input);
    if (!phone.matches("0[35789][0-9]{8}")) throw new IllegalArgumentException(
      "Hãy nhập số di động Việt Nam bắt đầu bằng 0 hoặc +84."
    );
    return phone;
  }
}
