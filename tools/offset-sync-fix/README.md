# Builder offset synchronization fix (1.20 / 6.0.8)

McJtyLib 8.0.7 `Sync$1.readBuf` deserializes all GUI values through their setters.
`BuilderTileEntity.setAnchor` previously recalculated Shape Card offsets on the
client. Opening the Builder screen or toggling Support could replace manually
entered offsets with the automatic anchor position. For dimensions 20,400,80,
this was observed as -20,100,-60 becoming -10,200,-39.

The source fix stores the synchronized anchor on the client and returns before
mutating the card. Explicit server-side anchor positioning remains unchanged.

These tools reproduce the same guard in the installed 6.0.8 production JAR.
Use JDK 17 and provide ASM core and ASM tree JARs on the classpath:

```sh
javac -cp "$ASM_CP" PatchBuilder.java TestGuard.java VerifyPatch.java
java -Xverify:all -cp ".:$ASM_CP" TestGuard
java -cp ".:$ASM_CP" PatchBuilder original.jar patched.jar
java -cp ".:$ASM_CP" VerifyPatch original.jar patched.jar
```

`TestGuard` verifies client, server and null-level branches on the JVM.
`VerifyPatch` checks that all other JAR entries remain identical and that removing
the guard restores the original executable class (excluding debug/frame metadata).
Both checks passed for the deployed patch. In-game verification and a full source
build of this fork have not yet been performed. Generated binaries are excluded.
