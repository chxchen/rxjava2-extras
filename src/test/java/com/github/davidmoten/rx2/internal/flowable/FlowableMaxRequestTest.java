package com.github.davidmoten.rx2.internal.flowable;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;

import org.junit.Test;

import com.github.davidmoten.rx2.Consumers;
import com.github.davidmoten.rx2.FlowableTransformers;
import com.github.davidmoten.rx2.exceptions.ThrowingException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.LongConsumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableMaxRequestTest {

    @Test
    public void checkEmptyMaxRequestOneDownstreamRequestsMaxValue() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.<Integer>empty() //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(1)) //
                .test() //
                .assertNoValues() //
                .assertComplete();
        assertEquals(Arrays.asList(1L), requests);
    }

    @Test
    public void checkErrorMaxRequestOneDownstreamRequestsMaxValue() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.<Integer>error(new ThrowingException()) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(1)) //
                .test() //
                .assertNoValues() //
                .assertError(ThrowingException.class);
        assertEquals(Arrays.asList(1L), requests);
    }

    @Test
    public void checkSingleMaxRequestOneDownstreamRequestsMaxValue() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.just(1) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(1)) //
                .test() //
                .assertValue(1) //
                .assertComplete();
        assertEquals(Arrays.asList(1L, 1L), requests);
    }

    @Test
    public void checkSingleMaxRequestTwoDownstreamRequestsMaxValue() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.just(1) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(2)) //
                .test() //
                .assertValue(1) //
                .assertComplete();
        assertEquals(Arrays.asList(2L), requests);
    }

    @Test
    public void checkSingleMaxRequestMaxValueDownstreamRequestsMaxValue() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.just(1) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(Long.MAX_VALUE)) //
                .test() //
                .assertValue(1) //
                .assertComplete();
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }

    @Test
    public void checkMaxRequestOneDownstreamRequestMaxValue() {
        checkMaxRequestDownstreamRequestMaxValue(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L);
    }

    @Test
    public void checkMaxRequestTwoDownstreamRequestMaxValue() {
        checkMaxRequestDownstreamRequestMaxValue(2L, 2L, 2L, 2L, 2L, 2L, 2L);
    }

    @Test
    public void checkMaxRequestThreeDownstreamRequestMaxValue() {
        checkMaxRequestDownstreamRequestMaxValue(3L, 3L, 3L, 3L, 3L);
    }

    @Test
    public void checkMaxRequestOneDownstreamRequestTen() {
        checkMaxRequestDownstreamRequestTen(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L);
    }

    @Test
    public void checkMaxRequestTwoDownstreamRequestTen() {
        checkMaxRequestDownstreamRequestTen(2L, 2L, 2L, 2L, 2L, 2L);
    }

    @Test
    public void checkMaxRequestThreeDownstreamRequestTen() {
        checkMaxRequestDownstreamRequestTen(3L, 3L, 3L, 3L, 1L);
    }

    @Test
    public void checkRequestsWhileEmitting() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.range(1, 10) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(3)) //
                .rebatchRequests(4).test(11) //
                .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) //
                .assertComplete();
        assertEquals(Arrays.asList(3L, 1L, 3L, 3L, 3L), requests);
    }

    @Test
    public void checkCancel() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        TestSubscriber<Object> ts = Flowable.range(1, 10) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(3)) //
                .test(4).assertValues(1, 2, 3, 4); //
        ts.cancel();
        ts.requestMore(3);
        ts.assertValueCount(4);
        ts.assertNotTerminated();
        assertEquals(Arrays.asList(3L, 1L), requests);
    }

    @Test
    public void testAsync() {
        int N = 10000;
        for (int m = 1; m <= N + 1; m++) {
            Flowable.range(1, N) //
                    .compose(FlowableTransformers.maxRequest(m)) //
                    .observeOn(Schedulers.computation()) //
                    .test() //
                    .awaitDone(1, TimeUnit.MINUTES) //
                    .assertValueCount(N) //
                    .assertComplete(); //
        }
    }

    @Test
    public void testAsyncNotMaxValue() {
        int N = 10000;
        Flowable.range(1, N) //
                .compose(FlowableTransformers.maxRequest(3)) //
                .observeOn(Schedulers.computation()) //
                .test(N + 10) //
                .awaitDone(1, TimeUnit.MINUTES) //
                .assertValueCount(N) //
                .assertComplete(); //
    }

    @Test
    public void testAsyncMaxRequestIsMax() {
        int N = 10000;
        Flowable.range(1, N) //
                .compose(FlowableTransformers.maxRequest(Long.MAX_VALUE)) //
                .observeOn(Schedulers.computation()) //
                .test(N + 10) //
                .awaitDone(1, TimeUnit.MINUTES) //
                .assertValueCount(N) //
                .assertComplete(); //
    }

    @Test
    public void testMaxRequestIsMaxValue() {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.range(1, 10) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(Long.MAX_VALUE)) //
                .test() //
                .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) //
                .assertComplete();
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }

    private void checkMaxRequestDownstreamRequestMaxValue(long maxRequest, Long... expectedRequests) {
        List<Long> requests = new CopyOnWriteArrayList<Long>();
        Flowable.range(1, 10) //
                .doOnRequest(Consumers.addLongTo(requests)) //
                .compose(FlowableTransformers.maxRequest(maxRequest)) //
                .test() //
                .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) //
                .assertComplete();
        assertEquals(Arrays.asList(expectedRequests), requests);
    }

    private void checkMaxRequestDownstreamRequestTen(long maxRequest, Long... expectedRequests) {
        List<Long> list = new CopyOnWriteArrayList<Long>();
        Flowable.range(1, 10) //
                .doOnRequest(Consumers.addLongTo(list)) //
                .compose(FlowableTransformers.maxRequest(maxRequest)) //
                .test(10) //
                .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) //
                .assertComplete();
        assertEquals(Arrays.asList(expectedRequests), list);
    }

    @Test
    public void testDummyServiceCalls() {
        final ReplaySubject<Flowable<Integer>> subject = ReplaySubject.create();
        final LongFunction<Flowable<Integer>> fetch = new LongFunction<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(long n) {
                return Flowable.just((int) n).repeat(n);
            }
        };
        LongConsumer request = new LongConsumer() {
            @Override
            public void accept(long n) throws Exception {
                subject.onNext(fetch.apply(n));
            }
        };
        Flowable.concatEager(subject.serialize().toFlowable(BackpressureStrategy.BUFFER)) //
                .doOnRequest(request) //
                .doOnRequest(Consumers.printLong("request=")) //
                .compose(FlowableTransformers.rebatchRequests(2, 3)) //
                .test(1) //
                .assertValue(2) //
                .requestMore(1) //
                .assertValues(2, 2) //
                .requestMore(4) //
                .assertValues(2, 2, 3, 3, 3, 2);
    }

}
