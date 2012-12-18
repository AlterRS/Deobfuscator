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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

import EDU.purdue.cs.bloat.editor.MethodEditor;
import alterrs.deob.trans.ClassLiteralDeobfuscation;
import alterrs.deob.trans.ControlFlowDeobfuscation;
import alterrs.deob.trans.FieldDeobfuscation;
import alterrs.deob.trans.HandlerDeobfuscation;
import alterrs.deob.trans.MonitorDeobfuscation;
import alterrs.deob.trans.PrivilageDeobfuscation;
import alterrs.deob.trans.SimpleArithmeticDeobfuscation;
import alterrs.deob.trans.TryCatchDeobfuscation;
import alterrs.deob.trans.euclid.EuclideanInverseDeobfuscation;
import alterrs.deob.trans.euclid.EuclideanPairIdentifier;
import alterrs.deob.util.FileUtilities;
import alterrs.deob.util.NodeVisitor;

public class Deobfuscator {
	private static Application app = null;

	public static final NodeVisitor[] MISC_PRE_TRANSFORMERS = new NodeVisitor[] {
		new HandlerDeobfuscation(), new PrivilageDeobfuscation()
	};
	
	public static final NodeVisitor[][] TREE_TRANSFORMERS = new NodeVisitor[][] { 
		{ // Phase 1
			new EuclideanPairIdentifier(), 
			new ControlFlowDeobfuscation(), 
			new TryCatchDeobfuscation(),
			new FieldDeobfuscation(), 
			//new ClassLiteralDeobfuscation(), 
			//new SimpleArithmeticDeobfuscation(),
		},
		
		{ // Phase 2
			new EuclideanInverseDeobfuscation(), 
		}
	};
	
	public static final NodeVisitor[] MISC_POST_TRANSFORMERS = new NodeVisitor[] {
		new MonitorDeobfuscation()
	};


	static {
		MethodEditor.OPT_STACK_2 = true;
	}

	private static final Object lock = new Object();
	private static final int totalPhases = TREE_TRANSFORMERS.length;
	
	private static int phase = 0;
	private static int percent = -1;
	private static int prints = -1;
	private static double finishedChunks = 0;
	private static double totalChunks = 0;

	public static void main(String[] args) {
		try {
			System.out.println("Loading application... [" + args[0] + "]");
			app = new Application(new ZipFile(args[0]));
			//app = new Application("./bin/Test.class");
			System.out.println("Loaded " + app.size() + " classes!");
			System.out.println();

			System.out.print("Applying misc pre-transformers...");
			for (NodeVisitor visitor : MISC_PRE_TRANSFORMERS) {
				app.accept(visitor);
			}
			System.out.println(" DONE!");
			for (NodeVisitor visitor : MISC_PRE_TRANSFORMERS) {
				visitor.onFinish();
			}
			System.out.println();

			System.out.println("Executing a total of " + totalPhases + " phases!");
			ExecutorService executor = Executors.newFixedThreadPool(Runtime
					.getRuntime().availableProcessors());
			for(; phase < totalPhases; phase++) {
				System.out.println("^ Applying phase " + (phase + 1) + "...");
				
				Chunk[] chunks = app.split(32 * Runtime.getRuntime()
						.availableProcessors());
				totalChunks = chunks.length;
				for (int i = 0; i < chunks.length; i++) {
					executor.submit(chunks[i]);
				}
				
				synchronized (lock) {
					lock.wait();
				}
				
				if(phase != (totalPhases - 1)) {
					File temp = app.tempSave();
					app = new Application(new ZipFile(temp));
					temp.deleteOnExit();
				}
				
				System.gc();
				System.out.println();
			}
			System.out.println();
			for (NodeVisitor[] visitors : TREE_TRANSFORMERS) {
				for(NodeVisitor visitor : visitors) {
					visitor.onFinish();
				}
			}
			System.out.println();
			
			System.out.print("Applying misc post-transformers...");
			for (NodeVisitor visitor : MISC_POST_TRANSFORMERS) {
				app.accept(visitor);
			}
			System.out.println(" DONE!");
			for (NodeVisitor visitor : MISC_POST_TRANSFORMERS) {
				visitor.onFinish();
			}
			System.out.println();

			String output = args[1].replace("$t",
					new StringBuilder().append(System.currentTimeMillis()));
			System.out.println("Saving application... [" + output + "]");
			app.save(new File(output));
			System.out.println("DONE!");

			executor.shutdownNow();
			Runtime.getRuntime().exec("java -jar deps/proguard.jar deps/@opt.pro");
		} catch (Throwable t) {
			System.err.println("Failed to run deobfuscator!");
			t.printStackTrace();
		}
	}
	
	public static int getPhase() {
		return phase;
	}
	
	public static int getTotalPhases() {
		return totalPhases;
	}

	public static synchronized void onFinish(Chunk chunk) {
		finishedChunks++;
		int p = (int) ((finishedChunks / totalChunks) * 100);
		
		if (percent != p) {
			percent = p;

			if (prints++ == 8) {
				prints = 0;
				System.out.println();
			}

			System.out.print("\t" + p + "%");

			if (finishedChunks == totalChunks) {
				synchronized (lock) {
					lock.notifyAll();
				}
				
				if(percent == 100)
					System.out.println();
				
				finishedChunks = 0;
				percent = -1;
				prints = -1;
			}
		}
	}

	public static Application getApp() {
		return app;
	}
}
