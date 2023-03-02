package com.techahead.pdfcompress.service;


import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {
    Path root = Paths.get("uploads");

    public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
        int original_width = imgSize.width;
        int original_height = imgSize.height;
        int bound_width = boundary.width;
        int bound_height = boundary.height;
        int new_width = original_width;
        int new_height = original_height;

        // first check if we need to scale width
        if (original_width > bound_width) {
            //scale width to fit
            new_width = bound_width;
            //scale height to maintain aspect ratio
            new_height = (new_width * original_height) / original_width;
        }

        // then check if we need to scale even with the new height
        if (new_height > bound_height) {
            //scale height to fit instead
            new_height = bound_height;
            //scale width to maintain aspect ratio
            new_width = (new_height * original_width) / original_height;
        }

        return new Dimension(new_width, new_height);
    }

    public void compressPdf(MultipartFile file) throws IOException {
        PDDocument pdDocument = new PDDocument();
        PDDocument original = Loader.loadPDF(file.getInputStream(), MemoryUsageSetting.setupTempFileOnly());
        PDFRenderer renderer = new PDFRenderer(original);

        int noOfPages = original.getNumberOfPages();
        for (int i = 0; i < noOfPages; i++) {
            BufferedImage img = renderer.renderImage(i);
            Boolean isHorizontal = false;
            if (img.getWidth() > img.getHeight()) {
                isHorizontal = true;
            }

            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(0.9f);

            File tmpImageFile = root.resolve("tmp.jpg").toFile();
            ImageOutputStream ios = ImageIO.createImageOutputStream(tmpImageFile);
            jpgWriter.setOutput(ios);
            jpgWriter.write(null, new IIOImage(img, null, null), jpgWriteParam);

            //byte[] data= FileUtils.readFileToByteArray(tmpImageFile);
            PDImageXObject pdImage = PDImageXObject.createFromFile(tmpImageFile.getAbsolutePath(), pdDocument);

            // scale image
            int actualPDFWidth=img.getWidth();
            int actualPDFHeight=img.getHeight();
            PDPage page = new PDPage(PDRectangle.A4);
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, page);



            Dimension scaledDim = getScaledDimension(new Dimension(pdImage.getWidth(), pdImage.getHeight()),
                    new Dimension(actualPDFWidth, actualPDFHeight)); // I'm using this function: https://stackoverflow.com/questions/23223716/scaled-image-blurry-in-pdfbox

            // if horizontal rotate 90°, calculate position and draw on page
            if (isHorizontal) {
                int x = (int) PDRectangle.A4.getWidth() - (((int) PDRectangle.A4.getWidth() - scaledDim.height) / 2);
                int y = ((int) PDRectangle.A4.getHeight() - scaledDim.width) / 2;
                AffineTransform at = new AffineTransform(scaledDim.getHeight(), 0, 0, scaledDim.getWidth(), x, y);
                at.rotate(Math.toRadians(90));
                Matrix m = new Matrix(at);
                contentStream.drawImage(pdImage, m);
                page.setRotation(0);
            } else {
                int x = ((int) PDRectangle.A4.getWidth() - scaledDim.width) / 2;
                int y = ((int) PDRectangle.A4.getHeight() - scaledDim.height) / 2;
                contentStream.drawImage(pdImage, x, y, scaledDim.width, scaledDim.height);
            }
            //contentStream.drawImage(pdImage, x, y, scaledDim.width, scaledDim.height);
            contentStream.close();


            pdDocument.addPage(page);
        }

        File targetFile = root.resolve(file.getOriginalFilename()).toFile();
        pdDocument.save(targetFile);
        pdDocument.close();
        System.out.println(targetFile.getAbsolutePath());
    }

    public InputStreamResource compressPdf2(MultipartFile file) throws IOException {
        //Add compression logic
        PDDocument pdDocument = new PDDocument();
        PDDocument original = Loader.loadPDF(file.getInputStream(), MemoryUsageSetting.setupTempFileOnly());
        PDFRenderer renderer = new PDFRenderer(original);

        PDPage page = null;
        System.out.println("No of pages: " + original.getNumberOfPages());
        for (int i = 0; i < original.getNumberOfPages(); i++) {
            page = new PDPage(PDRectangle.A4);
            pdDocument.addPage(page);

            BufferedImage bim = renderer.renderImageWithDPI(i, 72, ImageType.RGB);
            PDImageXObject pdImage = JPEGFactory.createFromImage(pdDocument, bim);
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, page);

            float newHeight = PDRectangle.A4.getHeight();
            float newWidth = PDRectangle.A4.getWidth();

            contentStream.drawImage(pdImage, 0, 0, newWidth, newHeight);
            contentStream.close();
        }

        /*ByteArrayOutputStream originalByteArrayOutputStream=new ByteArrayOutputStream();
        pdDocument.save(originalByteArrayOutputStream);
        pdDocument.close();
        // take the copy of the stream and re-write it to an InputStream
        PipedInputStream in = new PipedInputStream();
        new Thread(new Runnable() {
            public void run () {
                // try-with-resources here
                // putting the try block outside the Thread will cause the
                // PipedOutputStream resource to close before the Runnable finishes
                try (final PipedOutputStream out = new PipedOutputStream(in)) {
                    // write the original OutputStream to the PipedOutputStream
                    // note that in order for the below method to work, you need
                    // to ensure that the data has finished writing to the
                    // ByteArrayOutputStream
                    originalByteArrayOutputStream.writeTo(out);
                }
                catch (IOException e) {
                    // logging and exception handling should go here
                }
            }
        }).start();*/
        File targetFile = root.resolve(file.getOriginalFilename()).toFile();
        pdDocument.save(targetFile);
        pdDocument.close();
        System.out.println(targetFile.getAbsolutePath());


        return new InputStreamResource(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        });
    }
}
