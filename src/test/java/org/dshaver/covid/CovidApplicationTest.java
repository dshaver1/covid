package org.dshaver.covid;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
class CovidApplicationTest {

    @Test
    void testDateFormatter() {
        String dateString = "01-FEB-20";
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter();
        LocalDate localDate = LocalDate.parse(dateString, dateTimeFormatter);
        assertEquals(LocalDate.of(2020, 2, 1), localDate);
    }
}
