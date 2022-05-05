package com.software.factory.expense.tracker.core.impls.operations;

import java.math.BigDecimal;
import java.util.Currency;

import com.software.factory.expense.tracker.core.abstracts.AbstractOperation;
import com.software.factory.expense.tracker.core.enums.OperationType;
import com.software.factory.expense.tracker.core.interfaces.Source;
import com.software.factory.expense.tracker.core.interfaces.Storage;

// расход
public class OutcomeOperation extends AbstractOperation {

    public OutcomeOperation() {
        super(OperationType.OUTCOME);
    }

    private Storage fromStorage; // откуда потратили
    private Source toSource; // на что потратили
    private BigDecimal fromAmount; // сумму, которую потратили
    private Currency fromCurrency; // в какой валюте потратили


    public Storage getFromStorage() {
        return fromStorage;
    }

    public void setFromStorage(Storage fromStorage) {
        this.fromStorage = fromStorage;
    }

    public Source getToSource() {
        return toSource;
    }

    public void setToSource(Source toSource) {
        this.toSource = toSource;
    }

    public BigDecimal getFromAmount() {
        return fromAmount;
    }

    public void setFromAmount(BigDecimal fromAmount) {
        this.fromAmount = fromAmount;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(Currency fromCurrency) {
        this.fromCurrency = fromCurrency;
    }
}
