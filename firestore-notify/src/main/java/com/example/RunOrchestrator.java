package com.example;

public class RunOrchestrator {

  public static void main(String[] args) {

    String collection = "accounts";

    Orchestrator orchestrator = new Orchestrator(collection);

    orchestrator.cycleForever(orchestrator, "update");
  }
}
