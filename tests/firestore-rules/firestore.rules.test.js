import { readFile } from "node:fs/promises";
import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} from "@firebase/rules-unit-testing";
import { doc, getDoc, getDocs, collection, runTransaction, setDoc } from "firebase/firestore";

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

beforeEach(async () => {\n  await testEnv.clearFirestore();\n});\n\nafter(async () => {
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


const officialRegister = JSON.parse(
  await readFile("../../app/src/main/assets/501mtb_member_registry.json", "utf8")
);

async function seedAdmin(uid = "admin-tx") {
  await seedUser(uid, "ADMIN");
  return testEnv.authenticatedContext(uid).firestore();
}

async function importOfficialRegister(db, register = officialRegister) {
  const parsed = register.members;
  return runTransaction(db, async tx => {
    const refs = parsed.map(member => doc(db, "members", member.memberId));
    const existing = [];
    for (const ref of refs) existing.push(await tx.get(ref));
    const counterRef = doc(db, "counters", "memberSequence");
    const counter = await tx.get(counterRef);
    if (existing.some(snapshot => snapshot.exists())) {
      throw new Error("Some official member IDs already exist.");
    }
    if (counter.exists() && (counter.data().lastIssued ?? 0) > 55) {
      throw new Error("The member sequence is already ahead.");
    }
    parsed.forEach((member, index) => {
      tx.set(refs[index], {
        ...member,
        badge: member.category,
        active: true,
        publicProfile: true
      });
    });
    tx.set(counterRef, { lastIssued: 55 });
    return parsed.length;
  });
}

async function allocateMember(db, name, gender, category) {
  return runTransaction(db, async tx => {
    const counterRef = doc(db, "counters", "memberSequence");
    const counter = await tx.get(counterRef);
    if (!counter.exists()) throw new Error("Initialize the register first.");
    const next = (counter.data().lastIssued ?? 55) + 1;
    if (next > 999) throw new Error("Member sequence exhausted.");
    const memberId = `${gender}-${String(next).padStart(3, "0")}`;
    const memberRef = doc(db, "members", memberId);
    const existing = await tx.get(memberRef);
    if (existing.exists()) throw new Error("Generated ID already exists.");
    tx.set(memberRef, {
      memberId, name, gender, category,
      badge: category, active: true, publicProfile: true
    });
    tx.update(counterRef, { lastIssued: next });
    return memberId;
  });
}

test("emulator transaction imports exactly the 55 official members", async () => {
  const db = await seedAdmin();
  assert.equal(await importOfficialRegister(db), 55);
  const members = await getDocs(collection(db, "members"));
  assert.equal(members.size, 55);
  assert.equal((await getDoc(doc(db, "counters", "memberSequence"))).data().lastIssued, 55);
});

test("a second import is refused and existing member data is preserved", async () => {
  const db = await seedAdmin();
  await importOfficialRegister(db);
  await assert.rejects(importOfficialRegister(db), /already exist/);
  const original = (await getDoc(doc(db, "members", "M-001"))).data();
  assert.equal(original.name, officialRegister.members[0].name);
  assert.equal((await getDocs(collection(db, "members"))).size, 55);
});

test("a partial existing register blocks import without overwriting", async () => {
  const db = await seedAdmin();
  await setDoc(doc(db, "members", "M-001"), {
    memberId: "M-001", name: "KEEP THIS RECORD", publicProfile: true
  });
  await assert.rejects(importOfficialRegister(db), /already exist/);
  assert.equal((await getDoc(doc(db, "members", "M-001"))).data().name, "KEEP THIS RECORD");
  assert.equal((await getDocs(collection(db, "members"))).size, 1);
});

test("new male and female members use one shared sequence", async () => {
  const db = await seedAdmin();
  await importOfficialRegister(db);
  assert.equal(await allocateMember(db, "Test Man", "M", "ROOKIE"), "M-056");
  assert.equal(await allocateMember(db, "Test Woman", "W", "ROOKIE"), "W-057");
  assert.equal((await getDoc(doc(db, "counters", "memberSequence"))).data().lastIssued, 57);
});

test("concurrent member additions receive distinct sequential IDs", async () => {
  const db = await seedAdmin();
  await importOfficialRegister(db);
  const ids = await Promise.all([
    allocateMember(db, "Concurrent A", "M", "ROOKIE"),
    allocateMember(db, "Concurrent B", "W", "ROOKIE")
  ]);
  assert.equal(new Set(ids).size, 2);
  assert.deepEqual(ids.slice().sort(), ["M-056", "W-057"].sort());
  assert.equal((await getDoc(doc(db, "counters", "memberSequence"))).data().lastIssued, 57);
});

test("adding a member before initialization is refused", async () => {
  const db = await seedAdmin();
  await assert.rejects(allocateMember(db, "Early Rider", "M", "ROOKIE"), /Initialize/);
});
