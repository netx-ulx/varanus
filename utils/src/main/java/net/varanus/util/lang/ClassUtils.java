package net.varanus.util.lang;


import java.lang.reflect.InvocationTargetException;


/**
 * Utility methods for {@code Class} object handling.
 */
public final class ClassUtils
{
    /**
     * Returns a new instance of the provided class. The provided class must
     * contain a zero-argument public constructor.
     * 
     * @param klass
     *            The class to be instantiated
     * @return a new instance of the provided class
     * @throws ClassInstantiationException
     *             If something went wrong while instantiating the class (the
     *             problem is described by this exception's
     *             {@linkplain Throwable#getCause() cause}
     */
    public static <T> T newClassInstance( Class<T> klass ) throws ClassInstantiationException
    {
        try {
            return klass.getConstructor().newInstance();
        }
        catch (InstantiationException
               | IllegalAccessException
               | IllegalArgumentException
               | InvocationTargetException
               | NoSuchMethodException
               | SecurityException e) {
            throw new ClassInstantiationException(e);
        }
    }

    /**
     * Returns an "abbreviation" of the full name of the provided class. Only
     * the first letter of each "package level" and the simple name of the class
     * are in the abbreviated name.
     * <p>
     * For example, this is the abbreviated name of the {@code java.lang.String}
     * class: {@code "j.l.String"}
     * 
     * @param klass
     *            A class object
     * @return an "abbreviation" of the full name of the provided class
     */
    public static String getAbbreviatedName( Class<?> klass )
    {
        String fullCanonicalName = klass.getCanonicalName();
        if (fullCanonicalName == null || fullCanonicalName.isEmpty()) {
            return "?";
        }

        // the char '.' must be escaped
        String[] splitName = fullCanonicalName.split("\\.");
        int nSplits = splitName.length;
        String simpleName = splitName[nSplits - 1];

        int builderCapacity = 2 * (nSplits - 1) + simpleName.length();
        StringBuilder denseNameBr = new StringBuilder(builderCapacity);

        for (int i = 0; i < nSplits - 1; i++) {
            final String splice = splitName[i];
            if (!splice.isEmpty()) {
                denseNameBr.append(splice.charAt(0)).append('.');
            }
        }

        denseNameBr.append(simpleName);

        return denseNameBr.toString();
    }

    private ClassUtils()
    {
        // not instantiable
    }
}
