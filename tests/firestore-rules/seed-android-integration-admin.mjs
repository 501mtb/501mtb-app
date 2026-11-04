import { readFile } from "node:fs/promises";
import { initializeApp, deleteApp } from "firebase/app";
import {
  connectAuthEmulator,
  createUserWithEmailAndPassword,
  getAuth,
  signInWithEmailAndPassword
} from "firebase/auth";
import { doc, setDoc } from "firebase/firestore";
import { initializeTestEnvironment } from "@firebase/rules-unit-testing";

const projectId = "demo-501mtb";
const email = "android-registry-admin@501mtb.test";
const password = "local-emulator-only-password-501mtb";

const app = initializeApp({
  apiKey: "fake-api-key",
  appId: "1:000000000000:android:501mtbtest",
  projectId
}, "android-registry-admin-seed");
const auth = getAuth(app);
connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });

let credential;
try {
  credential = await createUserWithEmailAndPassword(auth, email, password);
} catch (error) {
  if (error.code !== "auth/email-already-in-use") throw error;
  credential = await signInWithEmailAndPassword(auth, email, password);
}

const testEnv = await initializeTestEnvironment({
  projectId,
  firestore: {
    host: "127.0.0.1",
    port: 8080,
    rules: await readFile("../../firestore.rules", "utf8")
  }
});

try {
  await testEnv.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), "users", credential.user.uid), {
      uid: credential.user.uid,
      role: "ADMIN",
      approvalStatus: "PENDING"
    });
  });
  process.stdout.write("Seeded a disposable ADMIN account in the local Firebase emulators.\\n");
} finally {
  await testEnv.cleanup();
  await deleteApp(app);
}
