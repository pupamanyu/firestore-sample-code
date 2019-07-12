package com.example;

import com.github.javafaker.Faker;
import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.SetOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class Orchestrator {

  private final Firestore firestoreDB;
  private final String collection;
  private String document;
  private MessageDigest salt;
  private Credentials credentials;
  private final Faker faker;
  private final ImmutableList<String> types = ImmutableList.of("person", "patient", "student");

  private static final long MIN_TIME_BETWEEN_CYCLES = 10000;

  Orchestrator(String collection) {
    this.firestoreDB = FirestoreOptions.getDefaultInstance().getService();
    this.collection = collection;
    this.document = null;
    this.faker = new Faker();
    try {
      this.salt = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  Orchestrator(String collection, String document) {
    this.firestoreDB = FirestoreOptions.getDefaultInstance().getService();
    this.collection = collection;
    this.document = document;
    this.faker = new Faker();
    try {
      this.salt = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  Orchestrator(String collection, String document, String projectId, String credentialFile) {
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
    this.document = document;
    this.faker = new Faker();
    try {
      this.salt = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  public void update(String document, String firstName) {

    try {
      this.firestoreDB
          .collection(this.collection)
          .document(document)
          .set(updateData(firstName), SetOptions.merge())
          .get(); // This will block until write
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  public void insert() {

    try {
      this.firestoreDB
          .collection(this.collection)
          .document()
          .set(getData(), SetOptions.merge())
          .get(); // This will block until write
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
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

  private ImmutableMap<String, String> updateData(String firstName) {
    this.salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    return ImmutableMap.of(
        "uuid",
        BaseEncoding.base16().encode(this.salt.digest()),
        "type",
        "person",
        "firstName",
        firstName);
  }

  private ImmutableMap<String, String> getData() {
    this.salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    return ImmutableMap.of(
        "uuid", BaseEncoding.base16().encode(this.salt.digest()),
        "type", this.types.get(ThreadLocalRandom.current().nextInt(this.types.size())),
        "firstName", this.faker.name().firstName());
  }

  static void cycleForever(Orchestrator orchestrator, String operation) {
    long lastCycleTime = Long.MIN_VALUE;
    while (true) {
      waitForMinCycleTime(lastCycleTime);
      lastCycleTime = System.currentTimeMillis();
      /**
       * For testing updates, document ID is set to yZItWvz27xUcyvC3bARt, and field firstName is set
       * to Duncan for filtering This can be set to any existing document ID and any existing field
       * within the existing document
       *
       * <p>For testing inserts,
       *
       * <p>change:
       *
       * <p>orchestrator.update("yZItWvz27xUcyvC3bARt", "Duncan")
       *
       * <p>to:
       *
       * <p>orchestrator.insert();
       */
      if (operation.equals("insert")) {
        orchestrator.insert();
      } else if (operation.equals("update")) {
        orchestrator.update("yZItWvz27xUcyvC3bARt", "Duncan");
      }
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

  public static void main(String[] args) {

    String collection = "accounts";

    Orchestrator orchestrator = new Orchestrator(collection);

    cycleForever(orchestrator, "update");
  }
}
