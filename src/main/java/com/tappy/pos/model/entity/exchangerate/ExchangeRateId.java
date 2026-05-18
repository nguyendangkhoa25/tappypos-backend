package com.tappy.pos.model.entity.exchangerate;

import java.io.Serializable;
import java.util.Objects;

public class ExchangeRateId implements Serializable {

    private String currencyCode;
    private String source;

    public ExchangeRateId() {}

    public ExchangeRateId(String currencyCode, String source) {
        this.currencyCode = currencyCode;
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExchangeRateId that)) return false;
        return Objects.equals(currencyCode, that.currencyCode) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyCode, source);
    }
}
