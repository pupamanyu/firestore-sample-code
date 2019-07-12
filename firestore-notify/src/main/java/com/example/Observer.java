package com.example;

import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.EventListener;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Observer implements ObservableOnSubscribe<DocumentChange> {

  private final Firestore firestoreDB;
  private final String collection;
  private Credentials credentials;
  private static final long MIN_TIME_BETWEEN_CYCLES = 10000;
  private static final long TIMEOUT_SECONDS = 600;

  Observer(String collection) {
    this.firestoreDB = FirestoreOptions.getDefaultInstance().getService();
    this.collection = collection;
  }

  Observer(String collection, String projectId, String credentialFile) {
    setCredentials(credentialFile);
    this.firestoreDB =
        FirestoreOptions.newBuilder()
            .setProjectId(projectId)
            .setRetrySettings(getDefaultRetrySettingsBuilder().build())
            .setCredentials(this.credentials)
            .setDatabaseId("(default)")
            .build()
            .getService();
    this.collection = collection;
  }

  private void setCredentials(String credentialFile) {
    try {
      this.credentials = GoogleCredentials.fromStream(new FileInputStream(credentialFile));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static RetrySettings.Builder getDefaultRetrySettingsBuilder() {
    return RetrySettings.newBuilder()
        .setMaxAttempts(6)
        .setInitialRetryDelay(org.threeten.bp.Duration.ofMillis(1000L))
        .setMaxRetryDelay(org.threeten.bp.Duration.ofMillis(32_000L))
        .setRetryDelayMultiplier(2.0)
        .setTotalTimeout(org.threeten.bp.Duration.ofMillis(50_000L))
        .setInitialRpcTimeout(org.threeten.bp.Duration.ofMillis(50_000L))
        .setRpcTimeoutMultiplier(1.0)
        .setMaxRpcTimeout(org.threeten.bp.Duration.ofMillis(50_000L));
  }

  @Override
  public void subscribe(final ObservableEmitter<DocumentChange> emitter) throws Exception {

    Query queryRef = this.firestoreDB.collection(this.collection).whereEqualTo("type", "person");
    final EventListener<QuerySnapshot> querySnapshotEventListener =
        new EventListener<QuerySnapshot>() {
          @Override
          public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirestoreException e) {
            if (!emitter.isDisposed()) {
              if (e == null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                  emitter.onNext(dc);
                }
              } else {
                emitter.onError(e);
              }
            }
          }
        };
    ListenerRegistration registration = queryRef.addSnapshotListener(querySnapshotEventListener);
    emitter.setDisposable(
        Disposables.fromAction(
            new Action() {

              @Override
              public void run() throws Throwable {
                registration.remove();
              }
            }));
  }

  private static void cycleForever(Observer observer) {
    long lastCycleTime = Long.MIN_VALUE;
    while (true) {
      waitForMinCycleTime(lastCycleTime);
      lastCycleTime = System.currentTimeMillis();
      // observer.watch();
    }
  }

  private static void waitForMinCycleTime(long lastCycleTime) {
    long targetNextCycleTime = lastCycleTime + MIN_TIME_BETWEEN_CYCLES;

    while (System.currentTimeMillis() < targetNextCycleTime) {
      try {
        Thread.sleep(targetNextCycleTime - System.currentTimeMillis());
      } catch (InterruptedException ignore) {
      }
    }
  }
}
