## Input file for automake to generate the Makefile.in used by configure

SUBDIRS = modules

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
  ClassBundle.java \
  ClassIdentifier.java \
  CodeAnalyzer.java \
  CodeTransformer.java \
  ConstantRuntimeEnvironment.java \
  FieldIdentifier.java \
  Identifier.java \
  IdentifierMatcher.java \
  LocalIdentifier.java \
  Main.java \
  MethodIdentifier.java \
  OptionHandler.java \
  PackageIdentifier.java \
  ParseException.java \
  Renamer.java \
  ScriptParser.java \
  TranslationTable.java

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
