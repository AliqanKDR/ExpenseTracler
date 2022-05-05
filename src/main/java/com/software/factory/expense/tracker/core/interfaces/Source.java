package com.software.factory.expense.tracker.core.interfaces;


import com.software.factory.expense.tracker.core.enums.OperationType;

public interface Source<T extends Source> extends TreeNode<T>{

    OperationType getOperationType();

    void setOperationType(OperationType type);

}
