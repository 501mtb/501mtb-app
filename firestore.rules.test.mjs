import { readFileSync } from "node:fs";
import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from "@firebase/rules-unit-testing";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  writeBatch
} from "firebase/firestore";

const projectId = "demo-501mtb";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: { rules: readFileSync("firestore.rules", "utf8") }
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

function profile(uid, memberId, overrides = {}) {
  return {
    uid,
    email: uid + "@example.test",
    displayName: uid,
    photoURL: null,
    role: "USER",
    approvalStatus: "PENDING",
    ...(memberId === undefined ? {} : { memberId }),
    createdAt: new Date(),
    ...overrides
  };
}

async function seedCounter(lastIssued, lastIssuedId, lastIssuedBy = "seed") {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "counters/memberSequence"), {
      lastIssued,
      lastIssuedId,
      lastIssuedBy
    });
  });
}

async function seedUser(uid, memberId, overrides = {}) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      doc(context.firestore(), "users", uid),
      profile(uid, memberId, overrides)
    );
  });
}

async function assignNewUser(uid, number, memberId) {
  const db = testEnv.authenticatedContext(uid).firestore();
  const batch = writeBatch(db);
  batch.set(doc(db, "users", uid), profile(uid, memberId));
  batch.set(doc(db, "counters/memberSequence"), {
    lastIssued: number,
    lastIssuedId: memberId,
    lastIssuedBy: uid
  });
  return batch.commit();
}

test("first account can atomically receive R-001 and initialize the counter", async () => {
  await assertSucceeds(assignNewUser("rider-1", 1, "R-001"));
  const db = testEnv.authenticatedContext("rider-1").firestore();
  const user = await getDoc(doc(db, "users/rider-1"));
  const counter = await getDoc(doc(db, "counters/memberSequence"));
  assert.equal(user.data().memberId, "R-001");
  assert.equal(counter.data().lastIssued, 1);
});

test("next account can atomically receive R-002", async () => {
  await seedUser("rider-1", "R-001");
  await seedCounter(1, "R-001");
  await assertSucceeds(assignNewUser("rider-2", 2, "R-002"));
});

test("numeric counter must match the zero-padded ID exactly", async () => {
  await seedUser("rider-1", "R-001");
  await seedCounter(1, "R-001");
  await assertFails(assignNewUser("attacker", 2, "R-003"));
});

test("counter cannot skip sequence numbers", async () => {
  await seedUser("rider-1", "R-001");
  await seedCounter(1, "R-001");
  await assertFails(assignNewUser("attacker", 3, "R-003"));
});

test("existing account without memberId can receive the next ID", async () => {
  await seedUser("legacy-user", undefined);
  await seedUser("rider-1", "R-001");
  await seedCounter(1, "R-001");

  const db = testEnv.authenticatedContext("legacy-user").firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, "users/legacy-user"), { memberId: "R-002" });
  batch.update(doc(db, "counters/memberSequence"), {
    lastIssued: 2,
    lastIssuedId: "R-002",
    lastIssuedBy: "legacy-user"
  });
  await assertSucceeds(batch.commit());
});

test("legacy account with blank memberId can receive the next ID", async () => {
  await seedUser("legacy-user", "");
  await seedUser("rider-1", "R-001");
  await seedCounter(1, "R-001");

  const db = testEnv.authenticatedContext("legacy-user").firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, "users/legacy-user"), { memberId: "R-002" });
  batch.update(doc(db, "counters/memberSequence"), {
    lastIssued: 2,
    lastIssuedId: "R-002",
    lastIssuedBy: "legacy-user"
  });
  await assertSucceeds(batch.commit());
});

test("an already assigned memberId cannot be changed by its owner", async () => {
  await seedUser("rider-1", "R-001");
  await seedCounter(1, "R-001");

  const db = testEnv.authenticatedContext("rider-1").firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, "users/rider-1"), { memberId: "R-002" });
  batch.update(doc(db, "counters/memberSequence"), {
    lastIssued: 2,
    lastIssuedId: "R-002",
    lastIssuedBy: "rider-1"
  });
  await assertFails(batch.commit());
});

test("two concurrent attempts cannot issue the same next ID", async () => {
  await seedUser("rider-1", "R-001");
  await seedCounter(1, "R-001");

  const results = await Promise.allSettled([
    assignNewUser("rider-2", 2, "R-002"),
    assignNewUser("rider-3", 2, "R-002")
  ]);
  assert.equal(results.filter((result) => result.status === "fulfilled").length, 1);
  assert.equal(results.filter((result) => result.status === "rejected").length, 1);
});

test("counter cannot be deleted by a regular user", async () => {
  await seedCounter(1, "R-001");
  const db = testEnv.authenticatedContext("rider").firestore();
  const { deleteDoc } = await import("firebase/firestore");
  await assertFails(deleteDoc(doc(db, "counters/memberSequence")));
});

test("regular users cannot change role or approval status", async () => {
  await seedUser("rider", "R-001");
  const db = testEnv.authenticatedContext("rider").firestore();
  await assertFails(updateDoc(doc(db, "users/rider"), {
    role: "ADMIN",
    approvalStatus: "APPROVED"
  }));
});

test("public reads remain available for events and rides", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "events", "public-event"), { title: "Ride day" });
    await setDoc(doc(context.firestore(), "rides", "public-ride"), { title: "Group ride" });
  });

  const db = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "events/public-event")));
  await assertSucceeds(getDoc(doc(db, "rides/public-ride")));
});

test("admin-only event writes remain protected", async () => {
  await seedUser("admin", undefined, { role: "ADMIN", approvalStatus: "APPROVED" });
  const normalDb = testEnv.authenticatedContext("rider").firestore();
  const adminDb = testEnv.authenticatedContext("admin").firestore();

  await assertFails(setDoc(doc(normalDb, "events/blocked"), { title: "Blocked" }));
  await assertSucceeds(setDoc(doc(adminDb, "events/allowed"), { title: "Allowed" }));
});

test("member IDs and the counter are not readable by anonymous users", async () => {
  await seedUser("rider", "R-001");
  await seedCounter(1, "R-001");
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "users/rider")));
  await assertFails(getDoc(doc(db, "counters/memberSequence")));
});
