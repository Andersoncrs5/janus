package org.janus.shared.infrastructure.persistence.tx;


import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import org.janus.shared.domain.result.Result;

@ResultTransaction
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 10)
public class ResultTransactionInterceptor {

    @Inject
    TransactionManager transactionManager;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        boolean isNewTransaction = false;

        if (transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION) {
            transactionManager.begin();
            isNewTransaction = true;
        }

        try {
            Object returned = context.proceed();

            if (returned instanceof Result<?> result && !result.isSuccess()) {
                handleRollback(isNewTransaction);
                return returned;
            }

            if (isNewTransaction) {
                transactionManager.commit();
            }

            return returned;

        } catch (Exception ex) {
            handleRollback(isNewTransaction);
            throw ex;
        }
    }

    private void handleRollback(boolean isNewTransaction) throws Exception {
        if (isNewTransaction) {
            if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
                transactionManager.rollback();
            }
        } else {
            if (transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                transactionManager.setRollbackOnly();
            }
        }
    }
}