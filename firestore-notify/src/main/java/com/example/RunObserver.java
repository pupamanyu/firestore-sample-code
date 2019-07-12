package com.example;

import com.google.cloud.firestore.DocumentChange;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunObserver {
  public static void main(String[] args) {

    String collection = "accounts";

    Observer observer = new Observer(collection);

    Observable<DocumentChange> ob = Observable.create(observer);

    /** Set the Thread Pool Size for Schedulers */
    ThreadPoolExecutor observerThreadPoolExecutor =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    ThreadPoolExecutor subscriberThreadPoolExecutor =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    // Scheduler observerScheduler = Schedulers.computation();

    Scheduler observerScheduler = Schedulers.from(observerThreadPoolExecutor);

    // Scheduler subscriberScheduler = Schedulers.computation();
    Scheduler subscriberScheduler = Schedulers.from(subscriberThreadPoolExecutor);

    ob.observeOn(observerScheduler)
        .subscribeOn(subscriberScheduler)
        .subscribe(
            new Consumer<DocumentChange>() {
              AtomicBoolean initialUpdate = new AtomicBoolean(true);

              @Override
              public void accept(DocumentChange documentChange) throws Throwable {
                if (initialUpdate.get()) {
                  System.out.println("Ignoring Initial Full Update");
                  initialUpdate.set(false);
                } else {
                  System.out.println("Change Type: " + documentChange.getType());
                  System.out.println(
                      "Update Time: " + documentChange.getDocument().getUpdateTime());
                  System.out.println(
                      "Document Affected: " + documentChange.getDocument().getData());
                }
              }
            });

    // Blocking Observer for observing documents in a collection
    ob.blockingSubscribe();
  }
}
