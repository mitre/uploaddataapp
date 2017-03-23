package com.example.uploaddataapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Recursive file listing under a specified directory.
 *
 * @author javapractices.com
 * @author Alex Wong
 * @author anonymous user
 */
public final class ListFiles {
    /**
     * Recursively walk a directory tree and return a List of all
     * Files found; the List is sorted using File.compareTo().
     *
     * @param aStartingDir is a valid directory, which can be read.
     */
    static public List<File> getFileListing(
            File aStartingDir
    ) throws FileNotFoundException {
        validateDirectory(aStartingDir);
        List<File> result = getFileListingNoSort(aStartingDir);
        Collections.sort(result);
        return result;
    }

    // PRIVATE //
    static private List<File> getFileListingNoSort(
            File aStartingDir
    ) throws FileNotFoundException {
        List<File> result = new ArrayList<File>();
        File[] filesAndDirs = aStartingDir.listFiles();
        //Log.d("ListFiles", filesAndDirs.toString());
        if (filesAndDirs != null) {
            List<File> filesDirs = Arrays.asList(filesAndDirs);
            for(File file : filesDirs) {
                if ( ! file.isFile() ) {
                    //must be a directory
                    //recursive call!
                    List<File> deeperList = getFileListingNoSort(file);
                    result.addAll(deeperList);
                } else {
                    // only add files to the list
                    result.add(file);
                }
            }
        }
        return result;
    }

    /**
     * Directory is valid if it exists, does not represent a file, and can be read.
     */
    static private void validateDirectory (
            File aDirectory
    ) throws FileNotFoundException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
        }
    }
}
