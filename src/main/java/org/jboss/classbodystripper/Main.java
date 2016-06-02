package org.jboss.classbodystripper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 */
public class Main
{
    private static Path getTempFolder()
    {
        return Paths.get(System.getProperty("java.io.tmpdir")).resolve("classbodystripper");
    }

    public static void main(String[] argv) throws Exception
    {
        String inputFileString = argv[0];
        String outputJarFileString = argv[1];

        Path tempDirectory = getTempFolder().resolve(UUID.randomUUID().toString());
        try
        {
            FileUtils.deleteDirectory(tempDirectory.toFile());
            FileUtils.forceMkdir(tempDirectory.toFile());

            System.out.println("Should shrink: " + inputFileString + " to " + outputJarFileString);
            Path inputFilePath = Paths.get(inputFileString);
            Path outputFilePath = Paths.get(outputJarFileString);

            try (ZipFile zipFile = new ZipFile(inputFilePath.toFile()))
            {
                Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();
                while (zipEntryEnumeration.hasMoreElements())
                {
                    ZipEntry zipEntry = zipEntryEnumeration.nextElement();
                    if (zipEntry.isDirectory() || !zipEntry.getName().endsWith(".class"))
                        continue;

                    System.out.println("Reading: " + zipEntry);
                    ClassReader classReader = new ClassReader(zipFile.getInputStream(zipEntry));
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

                    ClassContentStripper contentStripper = new ClassContentStripper(classWriter);
                    classReader.accept(contentStripper, ClassReader.EXPAND_FRAMES);

                    Path outFilePath = tempDirectory.resolve(FilenameUtils.separatorsToSystem(zipEntry.getName()));
                    Path directory = outFilePath.getParent();
                    Files.createDirectories(directory);
                    try (FileOutputStream classOutputStream = new FileOutputStream(outFilePath.toFile()))
                    {
                        classOutputStream.write(classWriter.toByteArray());
                    }
                }
            }

            compress(tempDirectory, outputFilePath);
        }
        finally
        {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    public static void compress(Path inputDirectory, Path outputFile) throws IOException
    {
        System.out.println("Compressing: " + inputDirectory + " to " + outputFile);

        try (ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(outputFile.toFile())))
        {
            Files.walkFileTree(inputDirectory, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    String relativePath = inputDirectory.relativize(file).toString();
                    ZipEntry entry = new ZipEntry(relativePath);
                    zipFile.putNextEntry(entry);

                    FileInputStream in = new FileInputStream(file.toFile());
                    IOUtils.copy(in, zipFile);
                    IOUtils.closeQuietly(in);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
