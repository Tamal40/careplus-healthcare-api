package com.careplus.patient.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

public class PathMultipartFile implements MultipartFile {


    private final Path path;

    public PathMultipartFile(Path path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return path.getFileName().toString();
    }

    @Override
    public String getContentType() {
        return "text/csv";
    }

    @Override
    public boolean isEmpty() {
        try {
            return Files.size(path) == 0;
        } catch (IOException ex) {
            return true;
        }
    }

    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0;
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(Path destination) throws IOException {
        Files.copy(path, destination);
    }

    @Override
    public void transferTo(java.io.File destination)
            throws IOException {
        Files.copy(path, destination.toPath());
    }

}
