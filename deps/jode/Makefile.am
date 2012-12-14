## Input file for automake to generate the Makefile.in used by configure

SUBDIRS = util bytecode type jvm expr flow decompiler obfuscator @SWINGUI@

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
  AssertError.java \
  GlobalOptions.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)


JARFILE = jode-@VERSION@.jar
#data_DATA = $(JARFILE)

@QUOTE@-include Makefile.dep

%.class: %.java
	$(JAVAC) -classpath $(FULL_CLASSPATH) -d $(top_builddir) $<

Makefile.dep: $(MY_JAVA_FILES:.java=.class)
	$(JAVADEP) $^

clean-local:
	@rm -f *.class
	@rm -f *.dep

$(JARFILE): $(noinst_DATA)
	CLASSPATH=$(top_builddir):$(CLASSPATH) $(JAVA) -mx80m \
	    jode.obfuscator.Main --classpath=$(top_builddir) \
	                --dest=$(JARFILE) -v -v $(srcdir)/jode.jodescript
