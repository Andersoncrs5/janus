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

        boolean isNewTransaction =
                transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION;

        if (isNewTransaction) {
            transactionManager.begin();
        }

        try {
            Object returned = context.proceed();

            if (isNewTransaction) {

                if (returned instanceof Result<?> result
                        && !result.isSuccess()) {

                    transactionManager.rollback();
                    return returned;
                }

                transactionManager.commit();
            }

            return returned;

        } catch (Exception ex) {

            handleException(isNewTransaction);

            throw ex;
        }
    }

    private void handleException(boolean isNewTransaction)
            throws Exception {

        int status = transactionManager.getStatus();

        if (isNewTransaction) {

            if (status != Status.STATUS_NO_TRANSACTION
                    && status != Status.STATUS_COMMITTED
                    && status != Status.STATUS_ROLLEDBACK) {

                transactionManager.rollback();
            }

            return;
        }

        /*
         * Se já existe uma transação externa, uma exception
         * deve contaminar essa transação e obrigá-la a rollback.
         */
        if (status == Status.STATUS_ACTIVE
                || status == Status.STATUS_MARKED_ROLLBACK) {

            transactionManager.setRollbackOnly();
        }
    }
}