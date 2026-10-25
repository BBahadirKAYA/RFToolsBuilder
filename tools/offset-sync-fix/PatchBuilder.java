import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.jar.*;
import java.io.*;

/** RFTools Builder 6.0.8: GUI binding sync must not mutate the client card NBT. */
public class PatchBuilder implements Opcodes {
  static final String OWNER="mcjty/rftoolsbuilder/modules/builder/blocks/BuilderTileEntity";
  static final String ANCHOR="mcjty/rftoolsbuilder/modules/builder/blocks/AnchorMode";
  static InsnList guard(String owner, String anchor, String level, String levelField, String clientField) {
    InsnList code=new InsnList(); LabelNode server=new LabelNode();
    code.add(new VarInsnNode(ALOAD,0));
    code.add(new FieldInsnNode(GETFIELD,owner,levelField,"L"+level+";"));
    code.add(new JumpInsnNode(IFNULL,server));
    code.add(new VarInsnNode(ALOAD,0));
    code.add(new FieldInsnNode(GETFIELD,owner,levelField,"L"+level+";"));
    code.add(new FieldInsnNode(GETFIELD,level,clientField,"Z"));
    code.add(new JumpInsnNode(IFEQ,server));
    code.add(new VarInsnNode(ALOAD,0)); code.add(new VarInsnNode(ALOAD,1));
    code.add(new FieldInsnNode(PUTFIELD,owner,"anchor","L"+anchor+";"));
    code.add(new InsnNode(RETURN)); code.add(server);
    code.add(new FrameNode(F_SAME,0,null,0,null));
    return code;
  }
  public static void main(String[] args) throws Exception {
    try(JarFile input=new JarFile(args[0]); JarOutputStream output=new JarOutputStream(Files.newOutputStream(Path.of(args[1])))) {
      int patched=0;
      var entries=input.entries();
      while(entries.hasMoreElements()) {
        JarEntry entry=entries.nextElement(); byte[] bytes=input.getInputStream(entry).readAllBytes();
        if(entry.getName().equals(OWNER+".class")) {
          ClassNode node=new ClassNode(); new ClassReader(bytes).accept(node,0);
          for(MethodNode method:node.methods) if(method.name.equals("setAnchor") && method.desc.equals("(L"+ANCHOR+";)V")) {
            method.instructions.insert(guard(OWNER,ANCHOR,"net/minecraft/world/level/Level","f_58857_","f_46443_")); patched++;
          }
          ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer); bytes=writer.toByteArray();
        }
        JarEntry copy=new JarEntry(entry.getName()); copy.setTime(entry.getTime()); output.putNextEntry(copy); output.write(bytes); output.closeEntry();
      }
      if(patched!=1) throw new IllegalStateException("Expected one setAnchor, found "+patched);
    }
  }
}
