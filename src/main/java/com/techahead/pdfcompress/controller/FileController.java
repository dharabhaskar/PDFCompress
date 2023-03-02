package com.techahead.pdfcompress.controller;
import com.techahead.pdfcompress.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/info")
    public ResponseEntity<String> showInfo(){
        return ResponseEntity.ok("Hello");
    }

    @PostMapping("/compress")
    public ResponseEntity<String> getCompressedPDF(
            @RequestParam MultipartFile file) throws IOException {

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=img.jpg");
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

       /* return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.getSize())
                .contentType(MediaType.parseMediaType(Objects.requireNonNull(file.getContentType())))
                .body(fileService.compressPdf(file));*/
        fileService.compressPdf(file);
        return ResponseEntity.ok("Saved successfully");
    }
}
