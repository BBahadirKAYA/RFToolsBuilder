import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute the injected guard with JVM verification; server path must continue. */
public class TestGuard implements Opcodes {
  public static class Level { public boolean client; }
  public static class Anchor {}
  static class Loader extends ClassLoader { Class<?> define(byte[] bytes) {return defineClass(null,bytes,0,bytes.length);} }
  public static void main(String[] args) throws Exception {
    ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
    String owner="GuardFixture", level="TestGuard$Level", anchor="TestGuard$Anchor";
    writer.visit(V17,ACC_PUBLIC,owner,null,"java/lang/Object",null);
    writer.visitField(ACC_PUBLIC,"level","L"+level+";",null,null).visitEnd();
    writer.visitField(ACC_PUBLIC,"anchor","L"+anchor+";",null,null).visitEnd();
    MethodVisitor ctor=writer.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
    ctor.visitCode();ctor.visitVarInsn(ALOAD,0);ctor.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(RETURN);ctor.visitMaxs(0,0);ctor.visitEnd();
    MethodNode method=new MethodNode(ACC_PUBLIC,"setAnchor","(L"+anchor+";)V",null,null);
    method.instructions.add(PatchBuilder.guard(owner,anchor,level,"level","client"));
    method.instructions.add(new TypeInsnNode(NEW,"java/lang/AssertionError"));method.instructions.add(new InsnNode(DUP));
    method.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/AssertionError","<init>","()V",false));method.instructions.add(new InsnNode(ATHROW));
    method.accept(writer);writer.visitEnd();
    Class<?> type=new Loader().define(writer.toByteArray());Object fixture=type.getConstructor().newInstance();
    Level world=new Level();world.client=true;type.getField("level").set(fixture,world);
    Anchor target=new Anchor();var setter=type.getMethod("setAnchor",Anchor.class);
    setter.invoke(fixture,target);
    if(type.getField("anchor").get(fixture)!=target)throw new AssertionError("Client anchor not synced");
    for(boolean nullWorld:new boolean[]{false,true}) {
      world.client=false;type.getField("level").set(fixture,nullWorld?null:world);
      try {setter.invoke(fixture,target);throw new Exception("Server body skipped");}
      catch(java.lang.reflect.InvocationTargetException ex){if(!(ex.getCause() instanceof AssertionError))throw ex;}
    }
    System.out.println("PASS: client sync returns after updating anchor; server and null-level paths retain original body");
  }
}
