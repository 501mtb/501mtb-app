import { readFile } from "node:fs/promises";
import { after, before, test } from "node:test";
import assert from "node:assert/strict";
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc } from "firebase/firestore";

const projectId = "demo-501mtb";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: await readFile("../../firestore.rules", "utf8")
    }
  });
});

after(async () => {
  await testEnv?.cleanup();
});

async function seedUser(uid, role, approvalStatus = "PENDING") {
  await testEnv.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), "users", uid), {
      uid,
      role,
      approvalStatus
    });
  });
}

test("administrator can create official members and update the sequence", async () => {
  await seedUser("admin-1", "ADMIN");
  const db = testEnv.authenticatedContext("admin-1").firestore();

  await assertSucceeds(setDoc(doc(db, "members", "M-056"), {
    memberId: "M-056",
    name: "Test Rider",
    gender: "M",
    category: "ROOKIE",
    publicProfile: true,
    active: true
  }));
  await assertSucceeds(setDoc(doc(db, "counters", "memberSequence"), {
    lastIssued: 56
  }));
});

test("ordinary users cannot create members or change the sequence", async () => {
  await seedUser("user-1", "USER");
  const db = testEnv.authenticatedContext("user-1").firestore();

  await assertFails(setDoc(doc(db, "members", "M-056"), {
    memberId: "M-056",
    name: "Unauthorized Rider",
    gender: "M",
    category: "ROOKIE",
    publicProfile: true
  }));
  await assertFails(setDoc(doc(db, "counters", "memberSequence"), {
    lastIssued: 56
  }));
});

test("a user cannot promote their own account to administrator", async () => {
  await seedUser("user-2", "USER");
  const db = testEnv.authenticatedContext("user-2").firestore();

  await assertFails(setDoc(doc(db, "users", "user-2"), {
    uid: "user-2",
    role: "ADMIN",
    approvalStatus: "APPROVED"
  }));
});

test("public member profiles are readable without signing in", async () => {
  await testEnv.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), "members", "M-001"), {
      memberId: "M-001",
      name: "Public Rider",
      publicProfile: true
    });
  });

  const db = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "members", "M-001")));
});

test("private member links remain administrator-only", async () => {
  await seedUser("user-3", "USER");
  const db = testEnv.authenticatedContext("user-3").firestore();

  await assertFails(setDoc(doc(db, "memberLinks", "M-001"), {
    uid: "user-3"
  }));
});
