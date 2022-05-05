package com.software.factory.expense.tracker.core.dao.interfaces;

import java.util.List;

import com.software.factory.expense.tracker.core.enums.OperationType;
import com.software.factory.expense.tracker.core.interfaces.Source;

public interface SourceDAO extends CommonDAO<Source> {

    String SOURCE_TABLE = "source";

    List<Source> getList(OperationType operationType);// получить список корневых элементов деревьев для определенного типа операции
    int getRefCount(Source source);


}
