package com.software.factory.expense.tracker.core.dao.interfaces;

import java.util.List;

import com.software.factory.expense.tracker.core.enums.OperationType;
import com.software.factory.expense.tracker.core.interfaces.Operation;

public interface OperationDAO extends CommonDAO<Operation> {

    String OPERATION_TABLE = "operation";

    List<Operation> getList(OperationType operationType);// получить список операций определенного типа

}
