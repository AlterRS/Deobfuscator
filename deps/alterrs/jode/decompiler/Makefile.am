## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = @JAVAC@
JAVADEP = $(top_builddir)/javaDependencies.pl -subdir=$(subdir)\
   -dependdir=$(top_builddir) -classpath=$(top_builddir):$(top_srcdir) \
   -depfile=Makefile.dep
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
SUBSTCP = @SUBSTCP@
FULL_CLASSPATH := $(shell $(SUBSTCP) $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB))

MY_JAVA_FILES = \
  Analyzer.java \
  Applet.java \
  ClassAnalyzer.java \
  ClassDeclarer.java \
  DeadCodeAnalysis.java \
  Declarable.java \
  Decompiler.java \
  FieldAnalyzer.java \
  ImportHandler.java \
  LocalInfo.java \
  LocalVarEntry.java \
  LocalVariableRangeList.java \
  LocalVariableTable.java \
  Main.java \
  MethodAnalyzer.java \
  Opcodes.java \
  Options.java \
  OuterValueListener.java \
  OuterValues.java \
  ProgressListener.java \
  Scope.java \
  TabbedPrintWriter.java \
  Window.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)

@QUOTE@-include Makefile.dep

%.class: %.java
	$(JAVAC) -classpath $(FULL_CLASSPATH) -d $(top_builddir) $<

Makefile.dep: $(MY_JAVA_FILES:.java=.class)
	$(JAVADEP) $^

clean-local:
	@rm -f *.class
	@rm -f *.dep
