package com.redis.testcontainers.support;

import java.time.Duration;
import java.util.concurrent.Callable;

public class RetryCallable<T> implements Callable<T> {

    private final Callable<T> delegate;
    private final Duration sleep;
    private final Duration timeout;

    public RetryCallable(Callable<T> delegate, Duration sleep, Duration timeout) {
        this.delegate = delegate;
        this.sleep = sleep;
        this.timeout = timeout;
    }

    public static <T> DelegateBuilder<T> delegate(Callable<T> delegate) {
        return new DelegateBuilder<>(delegate);
    }

    public static class DelegateBuilder<T> {

        private final Callable<T> delegate;

        public DelegateBuilder(Callable<T> delegate) {
            this.delegate = delegate;
        }

        public RetryCallableBuilder<T> sleep(Duration sleep) {
            return new RetryCallableBuilder<>(delegate, sleep);
        }
    }

    public static class RetryCallableBuilder<T> {
        private final Callable<T> delegate;
        private final Duration sleep;

        public RetryCallableBuilder(Callable<T> delegate, Duration sleep) {
            this.delegate = delegate;
            this.sleep = sleep;
        }

        public RetryCallable<T> timeout(Duration timeout) {
            return new RetryCallable<>(delegate, sleep, timeout);
        }
    }

    @SuppressWarnings("BusyWait")
    @Override
    public T call() throws Exception {
        Exception lastException;
        long start = System.currentTimeMillis();
        do {
            try {
                return delegate.call();
            } catch (Exception e) {
                lastException = e;
                // ignore and try again
            }
            Thread.sleep(sleep.toMillis());
        } while (System.currentTimeMillis() - start < timeout.toMillis());
        throw lastException;
    }

}
