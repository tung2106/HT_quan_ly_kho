package com.vatlieu.service;

/** Bỏ dấu chỉ để tìm kiếm; không đổi tên hàng hay tên người đã lưu. */
public final class SearchText {

  private SearchText() {}

  public static String normalize(String text) {
    if (text == null) return "";
    return java.text.Normalizer.normalize(
      text.strip().toLowerCase(java.util.Locale.ROOT),
      java.text.Normalizer.Form.NFD
    )
      .replaceAll("\\p{M}+", "")
      .replace('đ', 'd');
  }
}
