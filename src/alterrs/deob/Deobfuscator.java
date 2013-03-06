/**
 * Copyright (C) <2012> <Lazaro Brito> <Shawn D>
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
import java.util.zip.ZipFile;

import EDU.purdue.cs.bloat.editor.MethodEditor;
import alterrs.deob.asm.Renamer;
import alterrs.deob.trans.ControlFlowDeobfuscation;
import alterrs.deob.trans.HandlerDeobfuscation;
import alterrs.deob.trans.MonitorDeobfuscation;
import alterrs.deob.trans.SimpleArithmeticDeobfuscation;
import alterrs.deob.trans.TryCatchDeobfuscation;
import alterrs.deob.trans.euclid.EuclideanDeobfuscation;
import alterrs.deob.trans.euclid.EuclideanPairIdentifier;
import alterrs.deob.util.NodeVisitor;

public class Deobfuscator {
	private static Application app = null;

	public static final NodeVisitor[] MISC_PRE_TRANSFORMERS = new NodeVisitor[] {
		new HandlerDeobfuscation(), 
	};
	
	public static final NodeVisitor[][] TREE_TRANSFORMERS = new NodeVisitor[][] { 
		{ // Phase 1
			new ControlFlowDeobfuscation(), 
			new TryCatchDeobfuscation(),
			// new FieldDeobfuscation(), 
			// new CallGraphBuilder(),
			// new ClassLiteralDeobfuscation(), 
			new SimpleArithmeticDeobfuscation(),
			new EuclideanPairIdentifier(), 
		},
		
		{ // Phase 2
			new EuclideanDeobfuscation(),
			// new CallGraphInvokeBuilder(),
		},
		
		// { // Phase 3
			// new RedundantMethodDeobfuscation()
		// }
	};
	
	public static final NodeVisitor[] MISC_POST_TRANSFORMERS = new NodeVisitor[] {
		new HandlerDeobfuscation(), new MonitorDeobfuscation()
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
			Renamer.main(new String[0]);
			
			System.out.println("Loading application...");
			app = new Application(new ZipFile("./input2.jar"));
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
				int realPhase = phase + 1;
				System.out.println("\n^ Applying phase " + realPhase + "...\n");
				
				Chunk[] chunks = app.split(32 * Runtime.getRuntime() .availableProcessors());
				totalChunks = chunks.length;
				for (int i = 0; i < chunks.length; i++) {
					executor.submit(chunks[i]);
				}
				
				System.out.println("\t^ Application split into "+chunks.length+" chunks!\n");
				
				synchronized (lock) {
					lock.wait();
				}
				
				if(phase != (totalPhases - 1)) {
					System.out.println("\n\n> Phase "+realPhase+" completed. Reloading classes to free memory and regraph.");
					File temp = app.tempSave();
					app = new Application(new ZipFile(temp));
					temp.deleteOnExit();
				}
				
				System.gc();
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

			String output = "output.jar".replace("$t",
					new StringBuilder().append(System.currentTimeMillis()));
			System.out.println("Saving application... [" + output + "]");
			app.save(new File(output));
			System.out.println("DONE!");

			executor.shutdownNow();
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
