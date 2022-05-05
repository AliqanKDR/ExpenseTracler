package com.software.factory.expense.tracker.core.interfaces;

// для подсчета количества ссылок на этот node
// нужно для того, чтобы не давать удалять справочные записи, на которые есть ссылки внутри операций
public interface RefNode {

    int getRefCount();

    void setRefCount(int refCount);

}
