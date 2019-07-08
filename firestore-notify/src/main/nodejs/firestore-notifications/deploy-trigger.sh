#!/usr/bin/env bash
# Wrapper script to deploy cloud functions for FireStore Document Watchers
#
PROJECT="<project name>"
COLLECTION="<collection name>"
RUNTIME="nodejs10"
UPDATE="providers/cloud.firestore/eventTypes/document.update"
CREATE="providers/cloud.firestore/eventTypes/document.create"
DELETE="providers/cloud.firestore/eventTypes/document.delete"
# {entity} is wildcard for watching all documents within a collection
# Documents within a subcollections inside a collections will not be watched
DOCUMENT="projects/${PROJECT}/databases/(default)/documents/${COLLECTION}/{entity}"

# Cloud Function for Updated Document Trigger
# Execute gcloud from the directory where index.js is located
gcloud functions deploy documentUpdated \
  --runtime ${RUNTIME} \
  --trigger-event ${UPDATE} \
  --trigger-resource ${DOCUMENT}

# Cloud Function for Created Document Trigger
# Execute gcloud from the directory where index.js is located
gcloud functions deploy documentCreated \
  --runtime ${RUNTIME} \
  --trigger-event ${CREATE} \
  --trigger-resource ${DOCUMENT}

# Cloud Function for Deleted Document Trigger
# Execute gcloud from the directory where index.js is located
gcloud functions deploy documentDeleted \
  --runtime ${RUNTIME} \
  --trigger-event ${DELETE} \
  --trigger-resource ${DOCUMENT}
