package com.example.exrxjava;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 에러처리
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }

            if ((e instanceof IOException) || (e instanceof SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            } if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            } if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                return;
            } if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                return;
            }

            Log.e("RxJava_HOOK", "Undeliverable exception received, not sure what to do" + e.getMessage());
        });


        Observable<String> source = Observable.create(emitter -> {
            emitter.onNext("Hello");
            emitter.onNext("1");
            emitter.onNext("2");
            emitter.onComplete();
        });

        source.subscribe(System.out::println);

        Observable<String> source2 = Observable.create(emitter -> {
            emitter.onNext("Hello");
            emitter.onNext("1");
            emitter.onError(new Throwable());
            emitter.onNext("2");
        });

        source2.subscribe(System.out::println, throwable -> System.out.println("Error"));

        testRxException();


    }

    // 일부러 에러를 낸다
    // 100 밀리세컨드 이후에 onNext 실행을 하는데 이미 dispose 되어서 InterruptedException 이 생긴다.
    // RxJavaPlugins.setErrorHandler 에러 처리로 들어가게됨.
    private void testRxException() {
        final Disposable disposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
            Thread.sleep(100);
            emitter.onNext("Response data.");
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(resp -> dispLog(resp), thr -> dispLog(thr.getMessage()));
        Observable.timer(50, TimeUnit.MILLISECONDS).subscribe(ret -> disposable.dispose());
    }

    private void dispLog(String msg) { Log.d(this.getClass().getSimpleName(), msg); }
}