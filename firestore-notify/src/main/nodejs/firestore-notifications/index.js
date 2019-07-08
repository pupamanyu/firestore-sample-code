/**
 * Triggered by a change to a Firestore document.
 *
 * @param {object} data The event payload.
 * @param {object} context The event metadata.
 */
exports.documentUpdated = (data, context) => {
  const triggerResource = context.resource;

  /**
   * Instead of console.log, we can execute any code within node.js
   */
  console.log(`Function triggered by change to: ${triggerResource}`);
  console.log(`Event type: ${context.eventType}`);

  if (data.oldValue && Object.keys(data.oldValue).length) {
    console.log(`Old value: ${JSON.stringify(data.oldValue)}`);
  }

  if (data.value && Object.keys(data.value).length) {
    console.log(`New value: ${JSON.stringify(data.value)}`);
  }
};
exports.documentCreated = (data, context) => {
  const triggerResource = context.resource;

  /**
   * Instead of console.log, we can execute any code within node.js
   */
  console.log(`Function triggered by change to: ${triggerResource}`);
  console.log(`Event type: ${context.eventType}`);

  if (data.value && Object.keys(data.value).length) {
    console.log(`Inserted value: ${JSON.stringify(data.value)}`);
  }
};
exports.documentDeleted = (data, context) => {
  const triggerResource = context.resource;

  /**
   * Instead of console.log, we can execute any code within node.js
   */
  console.log(`Function triggered by change to: ${triggerResource}`);
  console.log(`Event type: ${context.eventType}`);

  if (data.oldValue && Object.keys(data.oldValue).length) {
    console.log(`Deleted value: ${JSON.stringify(data.oldValue)}`);
  }
};
