package com.project.code_judge.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileService {
    public void unzipAndSave(MultipartFile zipFile, Path destinationDir) throws IOException{
        destinationDir = destinationDir.toAbsolutePath().normalize();
        if(!Files.exists(destinationDir)){
            Files.createDirectories(destinationDir);
        }

        try(ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null){
                Path path = destinationDir.resolve(zipEntry.getName()).normalize();
                if(!path.startsWith(destinationDir)){
                    throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }

                if(!zipEntry.isDirectory()){
                    Files.createDirectories(path.getParent());
                    Files.copy(zis, path, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
}
