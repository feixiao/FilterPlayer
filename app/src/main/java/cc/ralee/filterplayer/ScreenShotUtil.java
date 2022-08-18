package cc.ralee.filterplayer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class ScreenShotUtil {
    private static final String TAGS = "ScreenShotUtil";


    public static void screenShot(Context context, String path, GL10 gl, int x, int y, int w, int h) {
        Bitmap bmp = createBitmapFromGLSurface(context,x, y, w, h, gl);
        saveAndRefrash(context,path, bmp);
    }

    /**
     * x，y 指定从帧缓冲区读取的第一个像素的窗口坐标。 此位置是矩形像素块的左下角。
     *
     * width,height
     * 指定像素矩形的尺寸。 一个宽度和高度对应于单个像素。
     * */
    private static Bitmap createBitmapFromGLSurface(Context context,int x, int y, int w, int h, GL10 gl) throws OutOfMemoryError {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
                    intBuffer);

            int offset1, offset2;

            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;

                    if ( j == w-1) {
                        int color = pixel;
                        int r = Color.red(color);
                        int g = Color.green(color);
                        int b = Color.blue(color);
                        int a = Color.alpha(color);

                        String r1=Integer.toHexString(r);
                        String g1=Integer.toHexString(g);
                        String b1=Integer.toHexString(b);

                        Log.i(TAGS,"#" + r1+g1+b1);
                    }

                }
            }
        } catch (GLException e) {
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }


    private static void saveAndRefrash(Context context, String  cacheDirectory, Bitmap bitmap) {
        long l = System.currentTimeMillis();
        String filePath = cacheDirectory + l + ".png";

        File imagePath = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (Exception e) {
            Log.i(TAGS, e.toString());
        } finally {
            try {
                fos.close();
                bitmap.recycle();
                bitmap = null;

                //最后通知图库更新
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE); //扫描单个文件
                intent.setData(Uri.fromFile(imagePath)); //给图片的绝对路径
                context.sendBroadcast(intent);
            } catch (Exception e) {
                Log.i(TAGS, e.toString());
            }
        }

    }
}
