package com.eprocure.pr.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void should_normalize_amount_to_four_decimals() {
        Money money = new Money(new BigDecimal("35000000"), "vnd");

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("35000000.0000"));
        assertThat(money.currency()).isEqualTo("VND");
    }

    @Test
    void should_add_money_when_currency_matches() {
        Money first = new Money(new BigDecimal("1000.5000"), "VND");
        Money second = new Money(new BigDecimal("2000.2500"), "VND");

        assertThat(first.add(second).amount()).isEqualByComparingTo(new BigDecimal("3000.7500"));
    }

    @Test
    void should_throw_when_scale_exceeds_four_decimals() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1000.12345"), "VND"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scale");
    }

    @Test
    void should_throw_when_currency_mismatches() {
        Money vnd = new Money(new BigDecimal("1000.0000"), "VND");
        Money usd = new Money(new BigDecimal("1000.0000"), "USD");

        assertThatThrownBy(() -> vnd.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }
}
