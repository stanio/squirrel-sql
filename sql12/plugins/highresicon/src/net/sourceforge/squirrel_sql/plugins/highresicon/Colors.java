package net.sourceforge.squirrel_sql.plugins.highresicon;

import java.util.Arrays;

class Colors
{
   private static final float K_B = 0.0593f; // ITU-R BT.2020 conversion
   private static final float K_R = 0.2627f; // https://en.wikipedia.org/wiki/YCbCr#ITU-R_BT.2020_conversion
   private static final float K_G = 1 - K_B - K_R;

   private static final float SCALE_B = 2 * (1 - K_B);
   private static final float SCALE_R = 2 * (1 - K_R);
   private static final float SCALE_GB = SCALE_B * K_B / K_G;
   private static final float SCALE_GR = SCALE_R * K_R / K_G;

   static float luma(int rgb)
   {
      return K_R * ((rgb >> 16) & 0xFF)
           + K_G * ((rgb >> 8)  & 0xFF)
           + K_B *  (rgb        & 0xFF);
   }

   static void rgbToYCbCr(int rgb, float[] yCbCr)
   {
      // https://en.wikipedia.org/wiki/YCbCr#YCbCr
      float y = luma(rgb);                             // KR * R + KG * G + KB * B
      float cB = (( rgb        & 0xFF) - y) / SCALE_B; // (1 / 2) * (B − Y) / (1 − KB)
      float cR = (((rgb >> 16) & 0xFF) - y) / SCALE_R; // (1 / 2) * (R − Y) / (1 − KR)

      yCbCr[0] = y;
      yCbCr[1] = cB;
      yCbCr[2] = cR;
      if (yCbCr.length > 3)
      {
         yCbCr[3] = getAlpha(rgb);
      }
   }

   static int yCbCrToRgb(float[] yCbCr)
   {
      float y = yCbCr[0];
      float cB = yCbCr[1];
      float cR = yCbCr[2];

      int r = clamp(Math.round(y + cR * SCALE_R));
      int g = clamp(Math.round(y - cB * SCALE_GB - cR * SCALE_GR));
      int b = clamp(Math.round(y + cB * SCALE_B));

      if (yCbCr.length > 3)
      {
         int a = (int) Math.round(yCbCr[3]);
         return makePixel(a, r, g, b);
      }
      return makePixel(r, g, b);
   }

   static int makePixel(int a, int r, int g, int b) { return    (a << 24) | (r << 16) | (g << 8) | b; }
   static int makePixel(       int r, int g, int b) { return (0xFF << 24) | (r << 16) | (g << 8) | b; }
   static int makePixel(int a, int rgb)             { return    (a << 24) | (rgb & 0x00FFFFFF);       }

   static int getAlpha(int pix) { return (pix >> 24) & 0xFF; }
   static int getRed  (int pix) { return (pix >> 16) & 0xFF; }
   static int getGreen(int pix) { return (pix >> 8)  & 0xFF; }
   static int getBlue (int pix) { return (pix >> 0)  & 0xFF; }

   static int clamp(int val)
   {
      return (val < 0) ? 0 : (val > 255) ? 255 : val;
   }

   static int clamp(float val)
   {
      return (int) Math.round((val < 0) ? 0 : (val > 255) ? 255 : val);
   }

   static double gamma1(int val)
   {
      return val / 255.0;
   }

   static double gamma(int val, double gamma)
   {
      return Math.pow(val / 255.0, gamma);
   }

   public static void main(String[] args)
   {
      float[] yCbCr = new float[3];
      rgbToYCbCr(0xC080FF, yCbCr);
      System.out.println(Arrays.toString(yCbCr));
      System.out.println(Integer.toHexString(yCbCrToRgb(yCbCr)).toUpperCase());
   }

   private static int calcColor(int weightFront, int weightBack, int weightSum, int colFront, int colBack)
   {
      return (colFront * weightFront + colBack * weightBack) / weightSum;
   }

  // find intermediate color between two colors with alpha channels (=> NO alpha blending!!!)
  static int blend(int M, int N, int pixBack, int pixFront) {
      //assert (0 < M && M < N && N <= 1000);

      final int weightFront = getAlpha(pixFront) * M;
      final int weightBack = getAlpha(pixBack) * (N - M);
      final int weightSum = weightFront + weightBack;
      if (weightSum == 0)
          return 0;

      return makePixel(weightSum / N,
                       calcColor(weightFront, weightBack, weightSum, getRed(pixFront), getRed(pixBack)),
                       calcColor(weightFront, weightBack, weightSum, getGreen(pixFront), getGreen(pixBack)),
                       calcColor(weightFront, weightBack, weightSum, getBlue(pixFront), getBlue(pixBack)));
  }
}
