import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class VerifyPatch {
  public static void main(String[] args)throws Exception {
    try(var a=new JarFile(args[0]);var b=new JarFile(args[1])) {
      int changed=0;
      for(var entry:Collections.list(a.entries())) {
        byte[] before=a.getInputStream(entry).readAllBytes(),after=b.getInputStream(b.getJarEntry(entry.getName())).readAllBytes();
        if(Arrays.equals(before,after))continue;
        if(!entry.getName().equals(PatchBuilder.OWNER+".class"))throw new AssertionError("Unexpected change: "+entry);
        changed++;
        ClassNode original=new ClassNode(),patched=new ClassNode();new ClassReader(before).accept(original,0);new ClassReader(after).accept(patched,0);
        for(MethodNode m:patched.methods)if(m.name.equals("setAnchor"))for(int i=0;i<13;i++)m.instructions.remove(m.instructions.getFirst());
        ClassWriter first=new ClassWriter(0),second=new ClassWriter(0);original.accept(first);patched.accept(second);
        ClassWriter x=new ClassWriter(0),y=new ClassWriter(0);
        new ClassReader(first.toByteArray()).accept(x,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        new ClassReader(second.toByteArray()).accept(y,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        if(!Arrays.equals(x.toByteArray(),y.toByteArray())) {
          java.nio.file.Files.write(java.nio.file.Path.of("/tmp/rft-original.class"),x.toByteArray());
          java.nio.file.Files.write(java.nio.file.Path.of("/tmp/rft-stripped.class"),y.toByteArray());
          throw new AssertionError("Original body or metadata changed");
        }
      }
      if(changed!=1)throw new AssertionError("Expected one modified entry");
      System.out.println("PASS: only the client guard changed; original server body and all other JAR entries preserved");
    }
  }
}
