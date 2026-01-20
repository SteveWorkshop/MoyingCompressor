package rachel.materialapps.imgcompressor.logic.utils;

import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

public final class ImageCompressorUtil {

    public enum ScaleMode {
        FIXED_SIZE, FIXED_SHORT_EDGE, FIXED_LONG_EDGE
    }

    public static class Options {
        public ScaleMode mode = ScaleMode.FIXED_SHORT_EDGE;
        public int targetWidth  = 0;
        public int targetHeight = 0;
        public int targetShortEdge = 1080;
        public int targetLongEdge  = 1920;
        public int quality = 90;
        public int dpiX = 72;   // 新增
        public int dpiY = 72;   // 新增
    }

    /* ---------- 对外入口 ---------- */
    public static void compressDirectory(String src, String dst, Options opt) throws IOException {
        Path srcPath = Paths.get(src);
        Path dstPath = Paths.get(dst);
        Files.createDirectories(dstPath);

        Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File in = file.toFile();
                if (!isImage(in)) return FileVisitResult.CONTINUE;

                Path rel = srcPath.relativize(file);
                Path out = dstPath.resolve(rel);
                Files.createDirectories(out.getParent());

                compressImage(in, out.toFile(), opt);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /* ---------- 单张压缩 + 修改 DPI ---------- */
    public static void compressImage(File in, File out, Options opts) throws IOException {
        /* 1. 解码尺寸 */
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(in.getAbsolutePath(), o);
        int w = o.outWidth, h = o.outHeight;

        /* 2. 计算目标尺寸 */
        int[] dim = calcDim(w, h, opts);
        int nw = dim[0], nh = dim[1];

        /* 3. 采样 + 缩放 */
        o.inSampleSize = calculateInSampleSize(o, nw, nh);
        o.inJustDecodeBounds = false;
        Bitmap bmp = BitmapFactory.decodeFile(in.getAbsolutePath(), o);
        Bitmap scaled = Bitmap.createScaledBitmap(bmp, nw, nh, true);

        /* 4. 旋转 */
        int rotation = getRotation(in);
        if (rotation != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotation);
            Bitmap tmp = Bitmap.createBitmap(scaled, 0, 0, nw, nh, m, true);
            scaled.recycle();
            scaled = tmp;
        }

        /* 5. 写入临时文件 */
        File tmp = new File(out.getParent(), out.getName() + ".tmp");
        FileOutputStream fos = new FileOutputStream(tmp);
        scaled.compress(Bitmap.CompressFormat.JPEG, opts.quality, fos);
        fos.flush();
        fos.close();
        if (!bmp.isRecycled()) bmp.recycle();
        if (!scaled.isRecycled()) scaled.recycle();

        /* 6. 复制 EXIF + 修改 DPI */
        copyExifAndSetDpi(in, tmp, out, opts.dpiX, opts.dpiY);
        tmp.delete(); // 删除临时文件
    }

    /* ---------- 辅助 ---------- */
    private static boolean isImage(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
    }

    private static int[] calcDim(int w, int h, Options opts) {
        switch (opts.mode) {
            case FIXED_SIZE:
                return new int[]{opts.targetWidth, opts.targetHeight};
            case FIXED_SHORT_EDGE:
                if (w < h) {
                    int nw = opts.targetShortEdge;
                    return new int[]{nw, (int) ((double) h * nw / w)};
                } else {
                    int nh = opts.targetShortEdge;
                    return new int[]{(int) ((double) w * nh / h), nh};
                }
            case FIXED_LONG_EDGE:
                if (w > h) {
                    int nw = opts.targetLongEdge;
                    return new int[]{nw, (int) ((double) h * nw / w)};
                } else {
                    int nh = opts.targetLongEdge;
                    return new int[]{(int) ((double) w * nh / h), nh};
                }
        }
        throw new IllegalArgumentException("unknown mode");
    }

    private static int calculateInSampleSize(BitmapFactory.Options o, int reqW, int reqH) {
        int h = o.outHeight, w = o.outWidth;
        int inSample = 1;
        while (h / inSample > reqH || w / inSample > reqW) {
            inSample *= 2;
        }
        return inSample;
    }

    private static int getRotation(File f) {
        try {
            ExifInterface ex = new ExifInterface(f);
            int o = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (o) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
            }
        } catch (IOException ignore) {}
        return 0;
    }

    /* 复制 EXIF，并只改 DPI */
    private static void copyExifAndSetDpi(File src, File tmp, File dst, int dpiX, int dpiY) throws IOException {
        // 先把临时文件重命名为目标文件
        tmp.renameTo(dst);

        ExifInterface ex = new ExifInterface(dst);
        // 原有 EXIF 先全部复制（Glide/Luban 会丢，这里手动保）
        ExifInterface old = new ExifInterface(src);
        for (String tag : Arrays.asList(
                ExifInterface.TAG_DATETIME, ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL, ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE)) {
            String val = old.getAttribute(tag);
            if (val != null) ex.setAttribute(tag, val);
        }

        // 修改 DPI（EXIF 使用 rational，单位是 1/72 inch）
        ex.setAttribute(ExifInterface.TAG_X_RESOLUTION, dpiX + "/1");
        ex.setAttribute(ExifInterface.TAG_Y_RESOLUTION, dpiY + "/1");
        ex.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2"); // 2 = inch
        ex.saveAttributes();
    }
}
