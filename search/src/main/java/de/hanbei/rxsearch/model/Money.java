package de.hanbei.rxsearch.model;

import java.util.Currency;
import java.util.Objects;

public class Money {

    private final Double amount;
    private final Currency currency;

    public Money(Double amount, String currency) {
        this(amount, Currency.getInstance(currency));
    }

    public Money(Double amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Double getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) &&
                Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{\"Money\":{")
                .append("\"amount\":").append(amount)
                .append(", ")
                .append("\"currency\":\"").append(currency).append('"')
                .append("}}");
        return sb.toString();
    }
}
