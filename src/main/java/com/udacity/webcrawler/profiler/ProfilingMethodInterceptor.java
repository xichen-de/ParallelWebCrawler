/*
 * MIT License
 *
 * Copyright (c) 2021 Xi Chen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.udacity.webcrawler.profiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {
    private final Clock clock;
    private final Object delegate;
    private final ProfilingState state;


    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
        this.clock = Objects.requireNonNull(clock);
        this.delegate = delegate;
        this.state = state;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        if (isMethodProfiled(method)) {
            Instant startTime = clock.instant();
            try {
                result = method.invoke(delegate, args);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                Instant endTime = clock.instant();
                Duration duration = Duration.between(startTime, endTime);
                state.record(delegate.getClass(), method, duration);
            }
        } else {
            try {
                result = method.invoke(delegate, args);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        return result;
    }

    private boolean isMethodProfiled(Method method) {
        Annotation[] annotations = method.getAnnotations();
        boolean isProfiled = false;
        for (Annotation a : annotations) {
            if (a instanceof Profiled) {
                isProfiled = true;
                break;
            }
        }
        return isProfiled;
    }
}
