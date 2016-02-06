package info.plateaukao.android.customviews.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v8.renderscript.Float4;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    private static int contour_color = Color.RED;
    //private int original_bgcolor = Color.WHITE;
    private static int original_fgcolor = Color.BLACK;
    //private static int gridline_color = Color.RED;
    //private static int contour_bgcolor= Color.rgb(220, 220, 220);
    private static int contour_bgcolor = Color.TRANSPARENT;

    public BitmapUtils() {
        // TODO Auto-generated constructor stub
    }

    public static String savePhoto(final Context context, String filename, Bitmap bitmap, int imageSize, boolean shouldRecyleBitmap) {
        boolean success = false;
        String tempFilename = null;

        //create folder
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + File.separator + "CalliPlus");
        if (!myDir.exists())
            myDir.mkdirs();

        tempFilename = filename;
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "CalliPlus" + File.separator + tempFilename + ".png");
        // check if file already exists, if so, increment number
        int count = 1;
        while (file.exists()) {
            tempFilename = filename + count;
            file = new File(Environment.getExternalStorageDirectory() + File.separator + "CalliPlus" + File.separator + tempFilename + ".png");
            count++;
        }

        filename = tempFilename;

        // create new one
        try {
            success = file.createNewFile();
            if (!success) {
                Log.e("SAVE", "create file failed.");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream ostream = null;
        try {
            ostream = new FileOutputStream(file);

            int width = (imageSize==0)?bitmap.getWidth():imageSize;
            int height = (imageSize==0)?bitmap.getHeight():imageSize;
            Bitmap save = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            Paint paint = new Paint();
            //paint.setColor(Color.WHITE);
            Canvas now = new Canvas(save);
            //now.drawRect(new Rect(0, 0, width, height), paint);
            now.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(0, 0, width, height), null);

            if (save == null) {
                return null;
                // print error
            }
            save.compress(Bitmap.CompressFormat.PNG, 90, ostream);
            //clean 
            ostream.flush();
            ostream.close();

            // save to media provider
            ContentValues image = new ContentValues();
            image.put(Images.Media.TITLE, filename);
            image.put(Images.Media.DISPLAY_NAME, filename);
            image.put(Images.Media.DESCRIPTION, "CalliPlus generated Image");
            image.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
            image.put(Images.Media.MIME_TYPE, "image/png");
            image.put(Images.Media.ORIENTATION, 0);
            File parent = file.getParentFile();
            image.put(Images.ImageColumns.BUCKET_ID, parent.toString().toLowerCase().hashCode());
            image.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, parent.getName().toLowerCase());
            image.put(Images.Media.SIZE, file.length());
            image.put(Images.Media.DATA, file.getAbsolutePath());
            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);

            return file.getAbsolutePath();

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (shouldRecyleBitmap)
                bitmap.recycle();
        }

        return null;
    }

    public static Bitmap createContourDrawable(Bitmap src) {
        //Need to copy to ensure that the bitmap is mutable.
        if (null == src)
            return null;

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        int previousColor = 0;
        int width = src.getWidth();
        int height = src.getHeight();
        // get outline
        // left to right, top to down
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int currentColor = src.getPixel(x, y);

                if (previousColor != currentColor) {
                    bitmap.setPixel(x, y, contour_color);
                } else {
                    if (previousColor == original_fgcolor) {
                        bitmap.setPixel(x, y, contour_bgcolor);
                    } else {
                        bitmap.setPixel(x, y, Color.TRANSPARENT);
                    }
                }
                previousColor = currentColor;
            }
            if (src.getPixel(x, height - 1) == original_fgcolor) bitmap.setPixel(x, height - 1, contour_color);
        }

        // 2nd phase
        // top to down, left to right
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int currentColor = src.getPixel(x, y);

                if (previousColor != currentColor) {
                    bitmap.setPixel(x, y, contour_color);
                }
                previousColor = currentColor;
            }
            if (src.getPixel(width - 1, y) == original_fgcolor) bitmap.setPixel(width - 1, y, contour_color);
        }

        return bitmap;
    }

    public static boolean equalsColor(int colorA, int colorB, int precise) {
        int RA = (colorA >> 16) & 0xff;     //bitwise shifting
        int GA = (colorA >> 8) & 0xff;
        int BA = colorA & 0xff;

        int RB = (colorB >> 16) & 0xff;     //bitwise shifting
        int GB = (colorB >> 8) & 0xff;
        int BB = colorB & 0xff;

        if (Math.abs(RA - RB) < precise
                && Math.abs(GA - GB) < precise
                && Math.abs(BA - BB) < precise)
            return true;
        else
            return false;
    }

    private final static int CANVAS_HEIGHT = 220;
    private final static int BITMAP_HEIGHT = 200;
    private final static int INTERVAL = 10;

    public static Float4 convertColor(int color){
        float red=Color.red(color) / 255.0f;
        float green=Color.green(color) / 255.0f;
        float blue=Color.blue(color) / 255.0f;
        float alpha=Color.alpha(color) / 255.0f;
        return new Float4(red,green,blue,alpha);
    }
}
