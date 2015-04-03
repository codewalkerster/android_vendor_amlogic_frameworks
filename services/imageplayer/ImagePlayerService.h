
#ifndef ANDROID_IMAGEPLAYERSERVICE_H
#define ANDROID_IMAGEPLAYERSERVICE_H

#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <utils/String16.h>
#include <utils/Vector.h>
#include <utils/Mutex.h>

#include <SkBitmap.h>
#include <SkStream.h>
//#include <binder/MemoryDealer.h>
#include <IImagePlayerService.h>


#define AM_LOLLIPOP
#define AM_KITKAT

namespace android {

typedef struct {
    char* pBuff;
    int frame_width;
    int frame_height;
    int format;
    int rotate;
}FrameInfo_t;

struct InitParameter {
    float degrees;
    float scaleX;
    float scaleY;
    int cropX;
    int cropY;
    int cropWidth;
    int cropHeight;
};

enum RetType {
    RET_OK                          = 0,
    RET_ERR_OPEN_SYSFS              = -1,
    RET_ERR_OPEN_FILE               = -2,
    RET_ERR_INVALID_OPERATION       = -3,
    RET_ERR_DECORDER                = -4,
    RET_ERR_PARAMETER               = -5,
    RET_ERR_BAD_VALUE               = -6,
    RET_ERR_NO_MEMORY               = -7
};

/*
enum ParameterKey {
    KEY_PARAMETER_SET_IMAGE_SAMPLESIZE_SURFACESIZE,
    KEY_PARAMETER_ROTATE,
    KEY_PARAMETER_SCALE,
    KEY_PARAMETER_ROTATE_SCALE,
    KEY_PARAMETER_CROP_RECT,
    KEY_PARAMETER_DECODE_NEXT,
    KEY_PARAMETER_SHOW_NEXT
};
*/

class ImagePlayerService :  public BnImagePlayerService {
  public:
    ImagePlayerService();
    virtual ~ImagePlayerService();

    virtual int init();
    virtual int setDataSource(const char* uri);
    virtual int setDataSource(int fd, int64_t offset, int64_t length);
    virtual int setSampleSurfaceSize(int sampleSize, int surfaceW, int surfaceH);
    virtual int setRotate(float degrees, int autoCrop) ;
    virtual int setScale(float sx, float sy, int autoCrop);
    virtual int setRotateScale(float degrees, float sx, float sy, int autoCrop);
    virtual int setCropRect(int cropX, int cropY, int cropWidth, int cropHeight);
    virtual int prepareBuf(const char *uri);
    virtual int showBuf();
    virtual int start();
    virtual int prepare();
    virtual int show();
    virtual int release();
    static void instantiate();

    virtual status_t dump(int fd, const Vector<String16>& args);

  private:
    void initVideoAxis();
    int convertRGBA8888toRGB(void *dst, const SkBitmap *src);
    int convertARGB8888toYUYV(void *dst, const SkBitmap *src);
    int convertRGB565toYUYV(void *dst, const SkBitmap *src);
    int convertIndex8toYUYV(void *dst, const SkBitmap *src);

    int render(int format, SkBitmap *bitmap);
    SkBitmap* decode(SkStream *stream, InitParameter *parameter);
    SkBitmap* scale(SkBitmap *srcBitmap, float sx, float sy);
    SkBitmap* rotate(SkBitmap *srcBitmap, float degrees);
    SkBitmap* rotateAndScale(SkBitmap *srcBitmap, float degrees, float sx, float sy);
    bool renderAndShow(SkBitmap *bitmap);
    bool showBitmapRect(SkBitmap *bitmap, int cropX, int cropY, int cropWidth, int cropHeight);
    SkBitmap* fillSurface(SkBitmap *bitmap);

    mutable Mutex mLock;
    int mWidth, mHeight;
    SkBitmap *mBitmap;
    SkBitmap *mBufBitmap;
    // sample-size, if set to > 1, tells the decoder to return a smaller than
    // original bitmap, sampling 1 pixel for every size pixels. e.g. if sample
    // size is set to 3, then the returned bitmap will be 1/3 as wide and high,
    // and will contain 1/9 as many pixels as the original.
    int mSampleSize;

    char *mImageUrl;
    SkBitmap *mDstBitmap;
    int mFileDescription;
    //bool isAutoCrop;
    int surfaceWidth, surfaceHeight;

    InitParameter *mParameter;
    int mDisplayFd;
};

}  // namespace android

#endif // ANDROID_IMAGEPLAYERSERVICE_H