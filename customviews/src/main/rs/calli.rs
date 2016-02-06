#pragma version(1)
#pragma rs java_package_name(info.plateaukao.android.customviews.rs)

rs_allocation gIn, gOut;
float4 gbgColor;
int width, height;

uchar4 __attribute__((kernel)) contour(uchar4 v_in, uint32_t x, uint32_t y) {
  //float4 fPixel= rsUnpackColor8888(*(uchar*) rsGetElementAt(gIn, x, y));
  float4 fPixel= rsUnpackColor8888(v_in);

  if(fPixel.a < 0.3f){
    return rsPackColorTo8888(fPixel);
  }

  // black
  if(fPixel.r < 0.7f && fPixel.g < 0.7f && fPixel.b < 0.7f && fPixel.a > 0.5f) {
    float4 left = { 0, 0, 0, 0 };
    if (x > 0) {
      left = rsUnpackColor8888(*(uchar*) rsGetElementAt(gIn, x-1, y));
    }
    float4 right = { 0, 0, 0, 0 };
    if (x < width-1) {
      right = rsUnpackColor8888(*(uchar*) rsGetElementAt(gIn, x+1, y));
    }

    float4 top= { 0, 0, 0, 0 };
    if (y > 0) {
      top= rsUnpackColor8888(*(uchar*) rsGetElementAt(gIn, x, y-1));
    }
    float4 bottom = { 0, 0, 0, 0 };
    if (y < height-1) {
      bottom = rsUnpackColor8888(*(uchar*) rsGetElementAt(gIn, x, y+1));
    }

    // color change point
    //left
    if(fabs(left.r - fPixel.r) > 0.2f) {
      fPixel.r= 1.0f;
      fPixel.g = 0.0f;
      fPixel.b = 0.0f;
      fPixel.a = 1.0f;
    //right
    } else if(fabs(right.r - fPixel.r) > 0.5f ) {
      fPixel.r= 1.0f;
      fPixel.g = 0.0f;
      fPixel.b = 0.0f;
      fPixel.a = 1.0f;
    } else if(fabs(top.r - fPixel.r) > 0.5f ) {
      fPixel.r= 1.0f;
      fPixel.g = 0.0f;
      fPixel.b = 0.0f;
      fPixel.a = 1.0f;
    } else if(fabs(bottom.r - fPixel.r) > 0.5f ) {
      fPixel.r= 1.0f;
      fPixel.g = 0.0f;
      fPixel.b = 0.0f;
      fPixel.a = 1.0f;
    } else {
      fPixel = gbgColor;
    }
  } else {
    fPixel.r = fPixel.g = fPixel.b = 0.0f;
    fPixel.a = 0.0f;
  }
  return rsPackColorTo8888(fPixel);		// pack color back to uchar4
}

void transparent(const uchar4 *v_in, uchar4 *v_out,
                 const void *usrData, uint32_t x, uint32_t y) {
  //float4 f4 = rsUnpackColor8888(*(uchar*) rsGetElementAt(gIn, x, y));
  float4 f4= rsUnpackColor8888(*v_in);

  // not transparent
  if(f4.a > 0.3f) {
      //black
      if(f4.r < 0.7f && f4.g < 0.7f && f4.b < 0.7f) {
        f4.r = f4.g = f4.b = 0.0f;
        f4.a = 1.0f;
      } else { // white
        f4.r = f4.g = f4.b = 0.0f;
        f4.a = 0.0f;
      }
  }
  *v_out = rsPackColorTo8888(f4);		// pack color back to uchar4
}

