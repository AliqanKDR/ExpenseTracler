package com.software.factory.expense.tracker.core.interfaces;

import java.util.Calendar;

import com.software.factory.expense.tracker.core.enums.OperationType;

public interface Operation extends IconNode {

    long getId();

    void setId(long id);

    OperationType getOperationType();

    Calendar getDateTime();

    String getDescription();

}
