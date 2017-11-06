import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;


public class Test
{
    public static void main( String[] args )
    {
        compileRunnable(
            new String[] {"java.util.function.Function", "java.util.Map", "java.util.stream.Collectors"},
            "Function<Map<?, ?>, String> mapToLines = ( map ) -> map.entrySet().stream()",
            "    .map(String::valueOf)",
            "    .collect(Collectors.joining(System.lineSeparator()));",
            "System.out.println(mapToLines.apply(System.getProperties()));",
            "System.out.println(mapToLines.apply(System.getenv()));",
            "Thread.dumpStack();",
            "System.out.printf(\"Do we have a console? %s%n\", System.console() != null);")
                .run();
    }

    private static Runnable compileRunnable( String[] imports, String... runnableCodeLines )
    {
        return compileRunnable(imports, String.join(System.lineSeparator(), runnableCodeLines));
    }

    @SuppressWarnings( "unchecked" )
    private static Runnable compileRunnable( String[] imports, String runnableCode )
    {
        final String importLines = Stream.of(imports)
            .map(imp -> String.format("import %s;", imp))
            .collect(Collectors.joining(System.lineSeparator()));
        final String className = "MemoryRunnable";
        final String classCode = String.format(
            "%s%npublic class %s implements Runnable {%npublic void run() {%n%s%n}%n}",
            importLines,
            className,
            runnableCode);

        StringSourceFile srcFile = new StringSourceFile(className, classCode);

        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        MemoryFileManager fileMgr = new MemoryFileManager(comp);
        if (!comp.getTask(null, fileMgr, null, null, null, Collections.singleton(srcFile)).call())
            throw new RuntimeException("Compilation failed!");

        ByteArrayClassFile classFile = fileMgr.getClassFile(srcFile.getClassName());
        Class<Runnable> runClass = (Class<Runnable>)new CustomClassLoader().buildClass(classFile);
        try {
            return runClass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CustomClassLoader extends ClassLoader
    {
        Class<?> buildClass( ByteArrayClassFile classFile )
        {
            byte[] arr = classFile.toByteArray();
            return defineClass(classFile.getClassName(), arr, 0, arr.length);
        }
    }

    private static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager>
    {
        private final Map<String, ByteArrayClassFile> map = new HashMap<>();

        MemoryFileManager( JavaCompiler compiler )
        {
            super(compiler.getStandardFileManager(null, null, null));
        }

        ByteArrayClassFile getClassFile( String name )
        {
            return map.get(Objects.requireNonNull(name));
        }

        @Override
        public ByteArrayClassFile getJavaFileForOutput( Location _location,
                                                        String name,
                                                        Kind _kind,
                                                        FileObject _source )
        {
            ByteArrayClassFile mc = new ByteArrayClassFile(name);
            this.map.put(name, mc);
            return mc;
        }
    }

    private static final class StringSourceFile extends SimpleJavaFileObject
    {
        private final String className;
        private final String classCode;

        StringSourceFile( String className, String classCode )
        {
            super(URI.create("memo:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.className = className;
            this.classCode = Objects.requireNonNull(classCode);
        }

        public String getClassName()
        {
            return className;
        }

        @Override
        public CharSequence getCharContent( boolean _ignoreEncodingErrors )
        {
            return classCode;
        }
    }

    private static final class ByteArrayClassFile extends SimpleJavaFileObject
    {
        private final String                className;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ByteArrayClassFile( String className )
        {
            super(URI.create("memo:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
            this.className = className;
        }

        String getClassName()
        {
            return className;
        }

        byte[] toByteArray()
        {
            return baos.toByteArray();
        }

        @Override
        public ByteArrayOutputStream openOutputStream()
        {
            return baos;
        }
    }
}
