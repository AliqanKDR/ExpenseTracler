package com.software.factory.expense.tracker.core.dao.interfaces;

import java.math.BigDecimal;
import java.util.Currency;

import com.software.factory.expense.tracker.core.exceptions.CurrencyException;
import com.software.factory.expense.tracker.core.interfaces.Storage;

public interface StorageDAO extends CommonDAO<Storage> {

    String CURRENCY_AMOUNT_TABLE = "currency_amount";
    String STORAGE_TABLE = "storage";

    // boolean - чтобы удостовериться, что операция прошла успешно
    boolean addCurrency(Storage storage, Currency currency, BigDecimal initAmount) throws CurrencyException;
    boolean deleteCurrency(Storage storage, Currency currency) throws CurrencyException;
    boolean updateAmount(Storage storage, Currency currency, BigDecimal amount);// сюда входит прибавить, отнять и обновить
    int getRefCount(Storage storage);
}
