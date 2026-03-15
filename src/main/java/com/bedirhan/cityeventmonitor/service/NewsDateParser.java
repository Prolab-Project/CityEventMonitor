package com.bedirhan.cityeventmonitor.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Haber sitelerinden gelen ham tarih metnini (rawDate) LocalDateTime'a çevirir.
 */
@Component
public class NewsDateParser {

    private static final List<DateTimeFormatter> DATE_TIME_PARSERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT),
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", new Locale("tr")),
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", new Locale("tr"))
    );

    private static final List<DateTimeFormatter> DATE_ONLY_PARSERS = List.of(
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
            DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("tr")),
            DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("tr"))
    );

    /**
     * Ham tarih string'ini parse eder. Başarısızsa null döner.
     */
    public LocalDateTime parse(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        String trimmed = rawDate.trim();
        for (DateTimeFormatter formatter : DATE_TIME_PARSERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter formatter : DATE_ONLY_PARSERS) {
            try {
                LocalDate date = LocalDate.parse(trimmed, formatter);
                return date.atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
