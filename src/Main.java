import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите путь к файлу ключа");
        String keyFileName = scanner.nextLine();
        byte[] key;
        try {
            key = readFromInputStreamAsBytes(keyFileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Введите путь к файлу, который нужно зашифровать/расшифровать");
        String fileName = scanner.nextLine();
        File file = new File(fileName);
        System.out.println("Введите путь к output файлу");
        String outputFileName = scanner.nextLine();
        boolean encode;
        System.out.println("Зашифровать/расшифровать (e/d)");
        String encodeString = scanner.nextLine();
        encode = encodeString.startsWith("e");
        boolean enableCBC;
        System.out.println("Использовать CBC? (y/n)");
        String enableString = scanner.nextLine();
        enableCBC = enableString.startsWith("y");
        boolean generateCorrupt = false;
        if (encode) {
            System.out.println("Сгенерировать изображение с ошибкой (y/n)");
            String generateCorruptString = scanner.nextLine();
            generateCorrupt = generateCorruptString.startsWith("y");
        }
        Cast256 cipher = new Cast256(key);
        byte[] input;
        String[] splittedName = fileName.split("\\.");
        String format = splittedName[splittedName.length - 1];
        boolean isImage = format.equals("png") || format.equals("jpg") || format.equals("bmp") || format.equals("jpeg");
        if (isImage) {
            BufferedImage image = ImageIO.read(file);
            WritableRaster raster = image.getRaster();
            DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
            byte[] res = data.getData();
            int newLength = res.length;
            if (!encode) {
                while (res[res.length - 1] == 0) {
                    newLength--;
                    byte[] res2 = new byte[newLength];
                    System.arraycopy(res, 0, res2, 0, newLength);
                    res = res2;
                }
            }
            byte[] result = encode ? cipher.encode(res, enableCBC) : cipher.decode(res, enableCBC);
            int length = result.length;
            int cntr = 1;
            if (!encode) cntr = -1;
            if (result.length < (image.getHeight() + cntr * (enableCBC ? 8 / image.getWidth() == 0 ? 1 : 8 / image.getWidth() : 4 / image.getWidth() == 0 ? 1 : 4 / image.getWidth())) * image.getWidth() * 4)
                length = (image.getHeight() + cntr * (enableCBC ? 8 / image.getWidth() == 0 ? 1 : 8 / image.getWidth() : 4 / image.getWidth() == 0 ? 1 : 4 / image.getWidth())) * image.getWidth() * 4;
            byte[] res2 = new byte[length];
            System.arraycopy(result, 0, res2, 0, result.length);
            DataBufferByte a = new DataBufferByte(res2, length);
            Raster rstr = Raster.createRaster(image.getSampleModel().createCompatibleSampleModel(image.getWidth(), image.getHeight() + cntr * (enableCBC ? 8 / image.getWidth() == 0 ? 1 : 8 / image.getWidth() : 4 / image.getWidth() == 0 ? 1 : 4 / image.getWidth())), a, null);
            BufferedImage image1 = new BufferedImage(image.getWidth(), image.getHeight() + cntr * (enableCBC ? 8 / image.getWidth() == 0 ? 1 : 8 / image.getWidth() : 4 / image.getWidth() == 0 ? 1 : 4 / image.getWidth()), image.getType());
            image1.setData(rstr);
            ImageIO.write(image1, format, new File(outputFileName.endsWith("." + format) ? outputFileName : outputFileName + "." + format));
            int R = 0;
            int G = 0;
            int B = 0;
            for (int i = 0; i < image1.getWidth(); i++) {
                for (int j = 0; j < image1.getHeight(); j++) {
                    R += (image1.getRGB(i, j) & 0x00FF0000) >> 16;
                    G += (image1.getRGB(i, j) & 0x0000FF00) >> 8;
                    B += (image1.getRGB(i, j) & 0x000000FF);
                }
            }
            double Mr = (1 / (double) image1.getWidth()) * (1 / (double) image1.getHeight()) * R;
            double Mg =(1 / (double) image1.getWidth()) * (1 / (double) image1.getHeight()) * G;
            double Mb = (1 / (double) image1.getWidth()) * (1 / (double) image1.getHeight()) * B;
            double Mr_Mr = 0;
            double Mg_Mg = 0;
            double Mb_Mb = 0;
            double oSumR = 0;
            double oSumG = 0;
            double oSumB = 0;
            for (int i = 0; i < image1.getWidth(); i++) {
                for (int j = 0; j < image1.getHeight(); j++) {
                    int r = (image1.getRGB(i, j) & 0x00FF0000) >> 16;
                    Mr_Mr += r - Mr;
                    int g = (image1.getRGB(i, j) & 0x0000FF00) >> 8;
                    Mg_Mg += g - Mg;
                    int b = (image1.getRGB(i, j) & 0x000000FF);
                    Mb_Mb += b - Mb;
                    oSumR += Math.pow(r - Mr, 2);
                    oSumG += Math.pow(g - Mg, 2);
                    oSumB += Math.pow(b - Mb, 2);
                }
            }
            double oR = Math.sqrt((1 / ((double) image1.getWidth() *  (double) image1.getHeight() - 1)) * oSumR);
            double oG = Math.sqrt((1 / ((double) image1.getWidth() *  (double) image1.getHeight() - 1)) * oSumG);
            double oB = Math.sqrt((1 / ((double) image1.getWidth() *  (double) image1.getHeight() - 1)) * oSumB);
            System.out.println(Mr_Mr);
            System.out.println(Mg_Mg);
            System.out.println(Mb_Mb);
            System.out.println(oR);
            System.out.println(oG);
            System.out.println(oB);
            if (generateCorrupt) {
                image1.setRGB(image1.getWidth() / 2, image1.getHeight() / 2, 0x59A409);
                ImageIO.write(image1, format, new File(outputFileName.split("\\.")[0] + "_corrupt" + (enableCBC ? "_CBC" : "") + "." + format));
            }
        } else {
            try {
                input = readFromInputStreamAsBytes(fileName);
                byte[] result = encode ? cipher.encode(input, enableCBC) : cipher.decode(input, enableCBC);
                writeToOutputStreamAsBytes(outputFileName, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static byte[] readFromInputStreamAsBytes(String fileName)
            throws IOException {
        File file = new File(fileName);
        byte[] bytes;
        try (FileInputStream fis = new FileInputStream(file)) {
            bytes = fis.readAllBytes();
        }
        return bytes;
    }

    public static void writeToOutputStreamAsBytes(String fileName, byte[] bytes)
            throws IOException {
        File file = new File(fileName);
        try (FileOutputStream fis = new FileOutputStream(file)) {
            fis.write(bytes);
        }
    }
}