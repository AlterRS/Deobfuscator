/**
 * Copyright (C) <2012> <Lazaro Brito>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial 
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package alterrs.deob;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

import EDU.purdue.cs.bloat.context.BloatContext;
import EDU.purdue.cs.bloat.context.PersistentBloatContext;
import EDU.purdue.cs.bloat.editor.MemberRef;
import EDU.purdue.cs.bloat.editor.Type;
import EDU.purdue.cs.bloat.file.ClassFileLoader;
import EDU.purdue.cs.bloat.reflect.ClassInfo;
import alterrs.deob.tree.ClassNode;
import alterrs.deob.tree.FieldNode;
import alterrs.deob.tree.MethodNode;
import alterrs.deob.util.NodeVisitor;

public class Application {
	private ClassFileLoader loader;
	private PersistentBloatContext context;

	private ClassNode[] classes;

	public Application(ZipFile archive) throws ClassNotFoundException {
		loader = new ClassFileLoader();
		context = new PersistentBloatContext(loader);

		ClassInfo[] classInfos = loader.loadClassesFromZipFile(archive);
		classes = new ClassNode[classInfos.length];
		for (int i = 0; i < classes.length; i++) {
			ClassInfo info = classInfos[i];
			classes[i] = new ClassNode(info, context.editClass(info));
		}
	}

	/**
	 * Loads a single class file.
	 * @param classFile
	 * @throws ClassNotFoundException 
	 */
	public Application(String classFile) throws ClassNotFoundException {
		loader = new ClassFileLoader();
		context = new PersistentBloatContext(loader);
		
		ClassInfo info = loader.loadClass(classFile);
		classes = new ClassNode[] { new ClassNode(info, context.editClass(info)) };
	}
	
	public BloatContext context() {
		return context;
	}

	public ClassNode[] classes() {
		return classes;
	}

	public int size() {
		return classes.length;
	}

	public void accept(NodeVisitor visitor) {
		visitor.visitApplication(this);
		for (ClassNode c : classes) {
			c.accept(visitor);
		}
	}

	public void save(File file) throws IOException {
		File temp = new File("./temp" + System.currentTimeMillis() + "/").getCanonicalFile();
		
		loader.setOutputDir(temp);
		for (ClassNode c : classes) {
			for (MethodNode m : c.methods()) {
				m.releaseGraph();
				m.editor().commit();
			}
			c.editor.commit();
			c.info.commit();
		}
		
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(file));
		saveDir(temp, jos, temp);
		jos.close();
		
		deleteDir(temp);
	}
	
	private void saveDir(File dir, JarOutputStream jos, File parent) throws IOException {
		String replacement = parent.getPath() + File.separator;
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				saveDir(file, jos, parent);
				continue;
			}
			
			JarEntry je = new JarEntry(file.getPath().replace(replacement, "").replace(File.separatorChar, '/'));
			jos.putNextEntry(je);
			FileInputStream in = new FileInputStream(file);
			int read;
			while((read = in.read()) != -1) {
				jos.write(read);
			}
			in.close();
			jos.closeEntry();
		}
	}
	
	private void deleteDir(File dir) {
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				deleteDir(file);
				continue;
			}
			
			file.delete();
		}
		dir.delete();
	}

	public Chunk[] split(int d) {
		int cutoff = (classes.length / d);
		if (cutoff <= 0) {
			cutoff = 1;
		}
		LinkedList<Chunk> chunks = new LinkedList<>();
		ArrayList<ClassNode> list = new ArrayList<>();
		for (ClassNode c : classes) {
			list.add(c);
			if (list.size() == cutoff) {
				chunks.add(new Chunk(list.toArray(new ClassNode[0])));
				list.clear();
			}
		}
		return chunks.toArray(new Chunk[0]);
	}

	public FieldNode field(MemberRef ref) {
		for (ClassNode c : classes()) {
			if (ref.declaringClass().equals(Type.getType("L" + c.name() + ";"))) {
				for (FieldNode f : c.fields()) {
					if (ref.nameAndType().equals(f.editor.nameAndType())) {
						return f;
					}
				}
			}
		}
		return null;
	}
}
