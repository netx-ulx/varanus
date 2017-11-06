package net.varanus.util.io;


import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PathUtils
{
    public static Path toUnixPath( String path ) throws InvalidPathException
    {
        if (path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        }

        return Paths.get(path);
    }

    private PathUtils()
    {
        // not used
    }
}
