package com.droidlogic.app.tv;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.SystemProperties;

import android.util.Log;
import android.view.View;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.graphics.ImageFormat;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;

import android.media.audiofx.Srs;
import android.media.audiofx.Hpeq;

import static com.droidlogic.app.tv.TvControlCommand.*;

public class TvControlManager
{
    private static final String TAG = "TvControlManager";
    private final String OPENTVLOGFLG="open.libtv.log.flg";
    private boolean tvLogFlg =false;
    private Parcel mListener_ext;

    //--source detect msg define  -------------
    public final int MSG_SOURCE_CHANGE  =  0x0111;
    public final int MSG_SIGNAL_STATUS_UPDATED = 0x0112;
    public final int MSG_SIGNAL_AUTO_SWITCH = 0x113;
    public final int MSG_CURRENT_SOURCE_PLUG_OUT = 0x114;

    /* DTMB manunal search param */
    public String[] searchModulation = {
        "qpsk",
        "qam16",
        "qam32",
        "qam64",
        "qam128",
        "qam256",
        "qamauto",
        "vsb8",
        "vsb16",
        "psk8",
        "apsk16",
        "apsk32",
        "dqpsk"
    };

    //---------------------------------------
    public static class L_TvPlayer
    {
        public final static String   StartTvPlayer_EnterType_INT           =    "StartTvPlayer_EnterType_INT ";
        public final static String   SourceInput_SwitchType_INT            =    "SourceInput_SwitchType_INT" ;
        public final static String   SourceListView_ShowType_BOOLEAN       =    "SourceListView_ShowType_BOOLEAN";
        public final static String   SPECIFY_SOURCE_INI                    =    "SPECIFY_SOURCE_INI";
        public final static String   FACTORY_PROGRAM_NUM                   =    "Factory_Program_Num_INT";
        public final static String   FACTORY_MENU_START                    =    "Factory_Menu_Start_BOOLEAN";
        public final static String   FACTORY_AGING_ENABLE                  =    "Factory_Aging_Enable_BOOLEAN";

        public enum StartTvPlayer_EnterType {
            BootAuto_Enter(0),
                SourceKey_Enter(1),
                ScaleTvWindow_Enter(2),
                SchedueProgram_Enter(3),
                FactoryTest_Enter(4);
            private int val;
            private StartTvPlayer_EnterType(int val) {
                this.val = val;
            }

            public int toInt() {
                return this.val;
            }
        };
        public enum SourceInput_SwitchType {
            NONEED_SOURCE_INPUT_SWITCH(0),
                LAST_SOURCE_INPUT_SWITCH(1),
                SPECIFY_SOURCE_INPUT_SWITCH(2),
                AGING_TEST(3),
                SCREEN_COLOR_SWITCH(4);
            private int val;
            private SourceInput_SwitchType(int val) {
                this.val = val;
            }

            public int toInt() {
                return this.val;
            }
        };

    }

    public static enum CC_ATV_AUDIO_STANDARD {
        CC_ATV_AUDIO_STD_DK(0),
        CC_ATV_AUDIO_STD_I(1),
        CC_ATV_AUDIO_STD_BG(2),
        CC_ATV_AUDIO_STD_M(3),
        CC_ATV_AUDIO_STD_L(4),
        CC_ATV_AUDIO_STD_AUTO(5);

        private int val;

        CC_ATV_AUDIO_STANDARD(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public static  enum CC_ATV_VIDEO_STANDARD {

        CC_ATV_VIDEO_STD_AUTO(0),
        CC_ATV_VIDEO_STD_PAL(1),
        CC_ATV_VIDEO_STD_NTSC(2),
        CC_ATV_VIDEO_STD_SECAM(3);

        private int val;

        CC_ATV_VIDEO_STANDARD(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_COM_SWITCH_STATUS {
        CC_SWITCH_OFF(0), CC_SWITCH_ON(1);

        private int val;

        CC_COM_SWITCH_STATUS(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public static enum tvin_trans_fmt {
        TVIN_TFMT_2D,
            TVIN_TFMT_3D_LRH_OLOR,
            TVIN_TFMT_3D_LRH_OLER,
            TVIN_TFMT_3D_LRH_ELOR,
            TVIN_TFMT_3D_LRH_ELER,
            TVIN_TFMT_3D_TB,
            TVIN_TFMT_3D_FP,
            TVIN_TFMT_3D_FA,
            TVIN_TFMT_3D_LA,
            TVIN_TFMT_3D_LRF,
            TVIN_TFMT_3D_LD,
            TVIN_TFMT_3D_LDGD,
            TVIN_TFMT_3D_DET_TB,
            TVIN_TFMT_3D_DET_LR,
            TVIN_TFMT_3D_DET_INTERLACE,
            TVIN_TFMT_3D_DET_CHESSBOARD,
            TVIN_TFMT_3D_MAX,
    };

    /* tvin signal format table */
    public static enum tvin_sig_fmt_e {
        TVIN_SIG_FMT_NULL(0),
            //VGA Formats
            TVIN_SIG_FMT_VGA_512X384P_60HZ_D147    (0x001),
            TVIN_SIG_FMT_VGA_560X384P_60HZ_D147    (0x002),
            TVIN_SIG_FMT_VGA_640X200P_59HZ_D924    (0x003),
            TVIN_SIG_FMT_VGA_640X350P_85HZ_D080    (0x004),
            TVIN_SIG_FMT_VGA_640X400P_59HZ_D940    (0x005),
            TVIN_SIG_FMT_VGA_640X400P_85HZ_D080    (0x006),
            TVIN_SIG_FMT_VGA_640X400P_59HZ_D638    (0x007),
            TVIN_SIG_FMT_VGA_640X400P_56HZ_D416    (0x008),
            TVIN_SIG_FMT_VGA_640X480P_66HZ_D619    (0x009),
            TVIN_SIG_FMT_VGA_640X480P_66HZ_D667    (0x00a),
            TVIN_SIG_FMT_VGA_640X480P_59HZ_D940    (0x00b),
            TVIN_SIG_FMT_VGA_640X480P_60HZ_D000    (0x00c),
            TVIN_SIG_FMT_VGA_640X480P_72HZ_D809    (0x00d),
            TVIN_SIG_FMT_VGA_640X480P_75HZ_D000_A  (0x00e),
            TVIN_SIG_FMT_VGA_640X480P_85HZ_D008    (0x00f),
            TVIN_SIG_FMT_VGA_640X480P_59HZ_D638    (0x010),
            TVIN_SIG_FMT_VGA_640X480P_75HZ_D000_B  (0x011),
            TVIN_SIG_FMT_VGA_640X870P_75HZ_D000    (0x012),
            TVIN_SIG_FMT_VGA_720X350P_70HZ_D086    (0x013),
            TVIN_SIG_FMT_VGA_720X400P_85HZ_D039    (0x014),
            TVIN_SIG_FMT_VGA_720X400P_70HZ_D086    (0x015),
            TVIN_SIG_FMT_VGA_720X400P_87HZ_D849    (0x016),
            TVIN_SIG_FMT_VGA_720X400P_59HZ_D940    (0x017),
            TVIN_SIG_FMT_VGA_720X480P_59HZ_D940    (0x018),
            TVIN_SIG_FMT_VGA_768X480P_59HZ_D896    (0x019),
            TVIN_SIG_FMT_VGA_800X600P_56HZ_D250    (0x01a),
            TVIN_SIG_FMT_VGA_800X600P_60HZ_D000    (0x01b),
            TVIN_SIG_FMT_VGA_800X600P_60HZ_D000_A  (0x01c),
            TVIN_SIG_FMT_VGA_800X600P_60HZ_D317    (0x01d),
            TVIN_SIG_FMT_VGA_800X600P_72HZ_D188    (0x01e),
            TVIN_SIG_FMT_VGA_800X600P_75HZ_D000    (0x01f),
            TVIN_SIG_FMT_VGA_800X600P_85HZ_D061    (0x020),
            TVIN_SIG_FMT_VGA_832X624P_75HZ_D087    (0x021),
            TVIN_SIG_FMT_VGA_848X480P_84HZ_D751    (0x022),
            TVIN_SIG_FMT_VGA_960X600P_59HZ_D635    (0x023),
            TVIN_SIG_FMT_VGA_1024X768P_59HZ_D278   (0x024),
            TVIN_SIG_FMT_VGA_1024X768P_60HZ_D000   (0x025),
            TVIN_SIG_FMT_VGA_1024X768P_60HZ_D000_A (0x026),
            TVIN_SIG_FMT_VGA_1024X768P_60HZ_D000_B (0x027),
            TVIN_SIG_FMT_VGA_1024X768P_74HZ_D927   (0x028),
            TVIN_SIG_FMT_VGA_1024X768P_60HZ_D004   (0x029),
            TVIN_SIG_FMT_VGA_1024X768P_70HZ_D069   (0x02a),
            TVIN_SIG_FMT_VGA_1024X768P_75HZ_D029   (0x02b),
            TVIN_SIG_FMT_VGA_1024X768P_84HZ_D997   (0x02c),
            TVIN_SIG_FMT_VGA_1024X768P_74HZ_D925   (0x02d),
            TVIN_SIG_FMT_VGA_1024X768P_60HZ_D020   (0x02e),
            TVIN_SIG_FMT_VGA_1024X768P_70HZ_D008   (0x02f),
            TVIN_SIG_FMT_VGA_1024X768P_75HZ_D782   (0x030),
            TVIN_SIG_FMT_VGA_1024X768P_77HZ_D069   (0x031),
            TVIN_SIG_FMT_VGA_1024X768P_71HZ_D799   (0x032),
            TVIN_SIG_FMT_VGA_1024X1024P_60HZ_D000  (0x033),
            TVIN_SIG_FMT_VGA_1152X864P_60HZ_D000   (0x034),
            TVIN_SIG_FMT_VGA_1152X864P_70HZ_D012   (0x035),
            TVIN_SIG_FMT_VGA_1152X864P_75HZ_D000   (0x036),
            TVIN_SIG_FMT_VGA_1152X864P_84HZ_D999   (0x037),
            TVIN_SIG_FMT_VGA_1152X870P_75HZ_D062   (0x038),
            TVIN_SIG_FMT_VGA_1152X900P_65HZ_D950   (0x039),
            TVIN_SIG_FMT_VGA_1152X900P_66HZ_D004   (0x03a),
            TVIN_SIG_FMT_VGA_1152X900P_76HZ_D047   (0x03b),
            TVIN_SIG_FMT_VGA_1152X900P_76HZ_D149   (0x03c),
            TVIN_SIG_FMT_VGA_1280X720P_59HZ_D855   (0x03d),
            TVIN_SIG_FMT_VGA_1280X720P_60HZ_D000_A (0x03e),
            TVIN_SIG_FMT_VGA_1280X720P_60HZ_D000_B (0x03f),
            TVIN_SIG_FMT_VGA_1280X720P_60HZ_D000_C (0x040),
            TVIN_SIG_FMT_VGA_1280X720P_60HZ_D000_D (0x041),
            TVIN_SIG_FMT_VGA_1280X768P_59HZ_D870   (0x042),
            TVIN_SIG_FMT_VGA_1280X768P_59HZ_D995   (0x043),
            TVIN_SIG_FMT_VGA_1280X768P_60HZ_D100   (0x044),
            TVIN_SIG_FMT_VGA_1280X768P_85HZ_D000   (0x045),
            TVIN_SIG_FMT_VGA_1280X768P_74HZ_D893   (0x046),
            TVIN_SIG_FMT_VGA_1280X768P_84HZ_D837   (0x047),
            TVIN_SIG_FMT_VGA_1280X800P_59HZ_D810   (0x048),
            TVIN_SIG_FMT_VGA_1280X800P_59HZ_D810_A (0x049),
            TVIN_SIG_FMT_VGA_1280X800P_60HZ_D000   (0x04a),
            TVIN_SIG_FMT_VGA_1280X800P_85HZ_D000   (0x04b),
            TVIN_SIG_FMT_VGA_1280X960P_60HZ_D000   (0x04c),
            TVIN_SIG_FMT_VGA_1280X960P_60HZ_D000_A (0x04d),
            TVIN_SIG_FMT_VGA_1280X960P_75HZ_D000   (0x04e),
            TVIN_SIG_FMT_VGA_1280X960P_85HZ_D002   (0x04f),
            TVIN_SIG_FMT_VGA_1280X1024P_60HZ_D020  (0x050),
            TVIN_SIG_FMT_VGA_1280X1024P_60HZ_D020_A(0x051),
            TVIN_SIG_FMT_VGA_1280X1024P_75HZ_D025  (0x052),
            TVIN_SIG_FMT_VGA_1280X1024P_85HZ_D024  (0x053),
            TVIN_SIG_FMT_VGA_1280X1024P_59HZ_D979  (0x054),
            TVIN_SIG_FMT_VGA_1280X1024P_72HZ_D005  (0x055),
            TVIN_SIG_FMT_VGA_1280X1024P_60HZ_D002  (0x056),
            TVIN_SIG_FMT_VGA_1280X1024P_67HZ_D003  (0x057),
            TVIN_SIG_FMT_VGA_1280X1024P_74HZ_D112  (0x058),
            TVIN_SIG_FMT_VGA_1280X1024P_76HZ_D179  (0x059),
            TVIN_SIG_FMT_VGA_1280X1024P_66HZ_D718  (0x05a),
            TVIN_SIG_FMT_VGA_1280X1024P_66HZ_D677  (0x05b),
            TVIN_SIG_FMT_VGA_1280X1024P_76HZ_D107  (0x05c),
            TVIN_SIG_FMT_VGA_1280X1024P_59HZ_D996  (0x05d),
            TVIN_SIG_FMT_VGA_1280X1024P_60HZ_D000  (0x05e),
            TVIN_SIG_FMT_VGA_1360X768P_59HZ_D799   (0x05f),
            TVIN_SIG_FMT_VGA_1360X768P_60HZ_D015   (0x060),
            TVIN_SIG_FMT_VGA_1360X768P_60HZ_D015_A (0x061),
            TVIN_SIG_FMT_VGA_1360X850P_60HZ_D000   (0x062),
            TVIN_SIG_FMT_VGA_1360X1024P_60HZ_D000  (0x063),
            TVIN_SIG_FMT_VGA_1366X768P_59HZ_D790   (0x064),
            TVIN_SIG_FMT_VGA_1366X768P_60HZ_D000   (0x065),
            TVIN_SIG_FMT_VGA_1400X1050P_59HZ_D978  (0x066),
            TVIN_SIG_FMT_VGA_1440X900P_59HZ_D887   (0x067),
            TVIN_SIG_FMT_VGA_1440X1080P_60HZ_D000  (0x068),
            TVIN_SIG_FMT_VGA_1600X900P_60HZ_D000   (0x069),
            TVIN_SIG_FMT_VGA_1600X1024P_60HZ_D000  (0x06a),
            TVIN_SIG_FMT_VGA_1600X1200P_59HZ_D869  (0x06b),
            TVIN_SIG_FMT_VGA_1600X1200P_60HZ_D000  (0x06c),
            TVIN_SIG_FMT_VGA_1600X1200P_65HZ_D000  (0x06d),
            TVIN_SIG_FMT_VGA_1600X1200P_70HZ_D000  (0x06e),
            TVIN_SIG_FMT_VGA_1680X1050P_59HZ_D954  (0x06f),
            TVIN_SIG_FMT_VGA_1680X1080P_60HZ_D000  (0x070),
            TVIN_SIG_FMT_VGA_1920X1080P_49HZ_D929  (0x071),
            TVIN_SIG_FMT_VGA_1920X1080P_59HZ_D963_A(0x072),
            TVIN_SIG_FMT_VGA_1920X1080P_59HZ_D963  (0x073),
            TVIN_SIG_FMT_VGA_1920X1080P_60HZ_D000  (0x074),
            TVIN_SIG_FMT_VGA_1920X1200P_59HZ_D950  (0x075),
            TVIN_SIG_FMT_VGA_1024X768P_60HZ_D000_C (0x076),
            TVIN_SIG_FMT_VGA_1024X768P_60HZ_D000_D (0x077),
            TVIN_SIG_FMT_VGA_1920X1200P_59HZ_D988  (0x078),
            TVIN_SIG_FMT_VGA_1400X900P_60HZ_D000   (0x079),
            TVIN_SIG_FMT_VGA_1680X1050P_60HZ_D000  (0x07a),
            TVIN_SIG_FMT_VGA_800X600P_60HZ_D062    (0x07b),
            TVIN_SIG_FMT_VGA_800X600P_60HZ_317_B   (0x07c),
            TVIN_SIG_FMT_VGA_RESERVE8              (0x07d),
            TVIN_SIG_FMT_VGA_RESERVE9              (0x07e),
            TVIN_SIG_FMT_VGA_RESERVE10             (0x07f),
            TVIN_SIG_FMT_VGA_RESERVE11             (0x080),
            TVIN_SIG_FMT_VGA_RESERVE12             (0x081),
            TVIN_SIG_FMT_VGA_MAX                   (0x082),
            TVIN_SIG_FMT_VGA_THRESHOLD             (0x200),
            //Component Formats
            TVIN_SIG_FMT_COMP_480P_60HZ_D000       (0x201),
            TVIN_SIG_FMT_COMP_480I_59HZ_D940       (0x202),
            TVIN_SIG_FMT_COMP_576P_50HZ_D000       (0x203),
            TVIN_SIG_FMT_COMP_576I_50HZ_D000       (0x204),
            TVIN_SIG_FMT_COMP_720P_59HZ_D940       (0x205),
            TVIN_SIG_FMT_COMP_720P_50HZ_D000       (0x206),
            TVIN_SIG_FMT_COMP_1080P_23HZ_D976      (0x207),
            TVIN_SIG_FMT_COMP_1080P_24HZ_D000      (0x208),
            TVIN_SIG_FMT_COMP_1080P_25HZ_D000      (0x209),
            TVIN_SIG_FMT_COMP_1080P_30HZ_D000      (0x20a),
            TVIN_SIG_FMT_COMP_1080P_50HZ_D000      (0x20b),
            TVIN_SIG_FMT_COMP_1080P_60HZ_D000      (0x20c),
            TVIN_SIG_FMT_COMP_1080I_47HZ_D952      (0x20d),
            TVIN_SIG_FMT_COMP_1080I_48HZ_D000      (0x20e),
            TVIN_SIG_FMT_COMP_1080I_50HZ_D000_A    (0x20f),
            TVIN_SIG_FMT_COMP_1080I_50HZ_D000_B    (0x210),
            TVIN_SIG_FMT_COMP_1080I_50HZ_D000_C    (0x211),
            TVIN_SIG_FMT_COMP_1080I_60HZ_D000      (0x212),
            TVIN_SIG_FMT_COMP_MAX                  (0x213),
            TVIN_SIG_FMT_COMP_THRESHOLD            (0x400),
            //HDMI Formats
            TVIN_SIG_FMT_HDMI_640X480P_60HZ        (0x401),
            TVIN_SIG_FMT_HDMI_720X480P_60HZ        (0x402),
            TVIN_SIG_FMT_HDMI_1280X720P_60HZ       (0x403),
            TVIN_SIG_FMT_HDMI_1920X1080I_60HZ      (0x404),
            TVIN_SIG_FMT_HDMI_1440X480I_60HZ       (0x405),
            TVIN_SIG_FMT_HDMI_1440X240P_60HZ       (0x406),
            TVIN_SIG_FMT_HDMI_2880X480I_60HZ       (0x407),
            TVIN_SIG_FMT_HDMI_2880X240P_60HZ       (0x408),
            TVIN_SIG_FMT_HDMI_1440X480P_60HZ       (0x409),
            TVIN_SIG_FMT_HDMI_1920X1080P_60HZ      (0x40a),
            TVIN_SIG_FMT_HDMI_720X576P_50HZ        (0x40b),
            TVIN_SIG_FMT_HDMI_1280X720P_50HZ       (0x40c),
            TVIN_SIG_FMT_HDMI_1920X1080I_50HZ_A    (0x40d),
            TVIN_SIG_FMT_HDMI_1440X576I_50HZ       (0x40e),
            TVIN_SIG_FMT_HDMI_1440X288P_50HZ       (0x40f),
            TVIN_SIG_FMT_HDMI_2880X576I_50HZ       (0x410),
            TVIN_SIG_FMT_HDMI_2880X288P_50HZ       (0x411),
            TVIN_SIG_FMT_HDMI_1440X576P_50HZ       (0x412),
            TVIN_SIG_FMT_HDMI_1920X1080P_50HZ      (0x413),
            TVIN_SIG_FMT_HDMI_1920X1080P_24HZ      (0x414),
            TVIN_SIG_FMT_HDMI_1920X1080P_25HZ      (0x415),
            TVIN_SIG_FMT_HDMI_1920X1080P_30HZ      (0x416),
            TVIN_SIG_FMT_HDMI_2880X480P_60HZ       (0x417),
            TVIN_SIG_FMT_HDMI_2880X576P_60HZ       (0x418),
            TVIN_SIG_FMT_HDMI_1920X1080I_50HZ_B    (0x419),
            TVIN_SIG_FMT_HDMI_1920X1080I_100HZ     (0x41a),
            TVIN_SIG_FMT_HDMI_1280X720P_100HZ      (0x41b),
            TVIN_SIG_FMT_HDMI_720X576P_100HZ       (0x41c),
            TVIN_SIG_FMT_HDMI_1440X576I_100HZ      (0x41d),
            TVIN_SIG_FMT_HDMI_1920X1080I_120HZ     (0x41e),
            TVIN_SIG_FMT_HDMI_1280X720P_120HZ      (0x41f),
            TVIN_SIG_FMT_HDMI_720X480P_120HZ       (0x420),
            TVIN_SIG_FMT_HDMI_1440X480I_120HZ      (0x421),
            TVIN_SIG_FMT_HDMI_720X576P_200HZ       (0x422),
            TVIN_SIG_FMT_HDMI_1440X576I_200HZ      (0x423),
            TVIN_SIG_FMT_HDMI_720X480P_240HZ       (0x424),
            TVIN_SIG_FMT_HDMI_1440X480I_240HZ      (0x425),
            TVIN_SIG_FMT_HDMI_1280X720P_24HZ       (0x426),
            TVIN_SIG_FMT_HDMI_1280X720P_25HZ       (0x427),
            TVIN_SIG_FMT_HDMI_1280X720P_30HZ       (0x428),
            TVIN_SIG_FMT_HDMI_1920X1080P_120HZ     (0x429),
            TVIN_SIG_FMT_HDMI_1920X1080P_100HZ     (0x42a),
            TVIN_SIG_FMT_HDMI_1280X720P_60HZ_FRAME_PACKING  (0x42b),
            TVIN_SIG_FMT_HDMI_1280X720P_50HZ_FRAME_PACKING  (0x42c),
            TVIN_SIG_FMT_HDMI_1280X720P_24HZ_FRAME_PACKING  (0x42d),
            TVIN_SIG_FMT_HDMI_1280X720P_30HZ_FRAME_PACKING  (0x42e),
            TVIN_SIG_FMT_HDMI_1920X1080I_60HZ_FRAME_PACKING (0x42f),
            TVIN_SIG_FMT_HDMI_1920X1080I_50HZ_FRAME_PACKING (0x430),
            TVIN_SIG_FMT_HDMI_1920X1080P_24HZ_FRAME_PACKING (0x431),
            TVIN_SIG_FMT_HDMI_1920X1080P_30HZ_FRAME_PACKING (0x432),
            TVIN_SIG_FMT_HDMI_800X600_00HZ                  (0x433),
            TVIN_SIG_FMT_HDMI_1024X768_00HZ                 (0x434),
            TVIN_SIG_FMT_HDMI_720X400_00HZ                  (0x435),
            TVIN_SIG_FMT_HDMI_1280X768_00HZ                 (0x436),
            TVIN_SIG_FMT_HDMI_1280X800_00HZ                 (0x437),
            TVIN_SIG_FMT_HDMI_1280X960_00HZ                 (0x438),
            TVIN_SIG_FMT_HDMI_1280X1024_00HZ                (0x439),
            TVIN_SIG_FMT_HDMI_1360X768_00HZ                 (0x43a),
            TVIN_SIG_FMT_HDMI_1366X768_00HZ                 (0x43b),
            TVIN_SIG_FMT_HDMI_1600X1200_00HZ                (0x43c),
            TVIN_SIG_FMT_HDMI_1920X1200_00HZ                (0x43d),
            TVIN_SIG_FMT_HDMI_1440X900_00HZ                 (0x43e),
            TVIN_SIG_FMT_HDMI_1400X1050_00HZ                (0x43f),
            TVIN_SIG_FMT_HDMI_1680X1050_00HZ                (0x440),
            TVIN_SIG_FMT_HDMI_1920X1080I_60HZ_ALTERNATIVE   (0x441),
            TVIN_SIG_FMT_HDMI_1920X1080I_50HZ_ALTERNATIVE   (0x442),
            TVIN_SIG_FMT_HDMI_1920X1080P_24HZ_ALTERNATIVE   (0x443),
            TVIN_SIG_FMT_HDMI_1920X1080P_30HZ_ALTERNATIVE   (0x444),
            TVIN_SIG_FMT_HDMI_3840X2160_00HZ                (0x445),
            TVIN_SIG_FMT_HDMI_4096X2160_00HZ                (0x446),
            TVIN_SIG_FMT_HDMI_RESERVE7                      (0x447),
            TVIN_SIG_FMT_HDMI_RESERVE8                      (0x448),
            TVIN_SIG_FMT_HDMI_RESERVE9                      (0x449),
            TVIN_SIG_FMT_HDMI_RESERVE10                     (0x44a),
            TVIN_SIG_FMT_HDMI_RESERVE11                     (0x44b),
            TVIN_SIG_FMT_HDMI_720X480P_60HZ_FRAME_PACKING   (0x44c),
            TVIN_SIG_FMT_HDMI_720X576P_50HZ_FRAME_PACKING   (0x44d),
            TVIN_SIG_FMT_HDMI_MAX                           (0x44e),
            TVIN_SIG_FMT_HDMI_THRESHOLD                     (0x600),
            //Video Formats
            TVIN_SIG_FMT_CVBS_NTSC_M                        (0x601),
            TVIN_SIG_FMT_CVBS_NTSC_443                      (0x602),
            TVIN_SIG_FMT_CVBS_PAL_I                         (0x603),
            TVIN_SIG_FMT_CVBS_PAL_M                         (0x604),
            TVIN_SIG_FMT_CVBS_PAL_60                        (0x605),
            TVIN_SIG_FMT_CVBS_PAL_CN                        (0x606),
            TVIN_SIG_FMT_CVBS_SECAM                         (0x607),
            TVIN_SIG_FMT_CVBS_MAX                           (0x608),
            TVIN_SIG_FMT_CVBS_THRESHOLD                     (0x800),
            //656 Formats
            TVIN_SIG_FMT_BT656IN_576I_50HZ                  (0x801),
            TVIN_SIG_FMT_BT656IN_480I_60HZ                  (0x802),
            //601 Formats
            TVIN_SIG_FMT_BT601IN_576I_50HZ                  (0x803),
            TVIN_SIG_FMT_BT601IN_480I_60HZ                  (0x804),
            //Camera Formats
            TVIN_SIG_FMT_CAMERA_640X480P_30HZ               (0x805),
            TVIN_SIG_FMT_CAMERA_800X600P_30HZ               (0x806),
            TVIN_SIG_FMT_CAMERA_1024X768P_30HZ              (0x807),
            TVIN_SIG_FMT_CAMERA_1920X1080P_30HZ             (0x808),
            TVIN_SIG_FMT_CAMERA_1280X720P_30HZ              (0x809),
            TVIN_SIG_FMT_BT601_MAX                          (0x80a),
            TVIN_SIG_FMT_BT601_THRESHOLD                    (0xa00),
            TVIN_SIG_FMT_MAX(0xFFFFFFFF);

        private int val;

        tvin_sig_fmt_e(int val) {
            this.val = val;
        }

        public static tvin_sig_fmt_e valueOf(int value) {
            for (tvin_sig_fmt_e fmt : tvin_sig_fmt_e.values()) {
                if (fmt.toInt() == value) {
                    return fmt;
                }
            }
            return TVIN_SIG_FMT_MAX;
        }

        public int toInt() {
            return this.val;
        }
    }

    // tvin signal status
    public enum tvin_sig_status_t {
        TVIN_SIG_STATUS_NULL,
            TVIN_SIG_STATUS_NOSIG,
            TVIN_SIG_STATUS_UNSTABLE,
            TVIN_SIG_STATUS_NOTSUP,
            TVIN_SIG_STATUS_STABLE,
    }
    // tvin signal information
    public class tvin_info_t
    {
        public tvin_trans_fmt trans_fmt;
        public tvin_sig_fmt_e fmt;
        public tvin_sig_status_t status;
        public int reserved;
    }

    private int mNativeContext; // accessed by native methods
    private EventHandler mEventHandler;
    private ErrorCallback mErrorCallback;
    private SigInfoChangeListener mSigInfoChangeLister = null;
    private SigChannelSearchListener mSigChanSearchListener = null;
    private VGAAdjustChangeListener mVGAChangeListener = null;
    private Status3DChangeListener mStatus3DChangeListener = null;
    private StatusTVChangeListener mStatusTVChangeListener = null;
    private DreamPanelChangeListener mDreamPanelChangeListener = null;
    private AdcCalibrationListener mAdcCalibrationListener = null;
    private SourceSwitchListener mSourceSwitchListener = null;
    private ChannelSelectListener mChannelSelectListener = null;
    private SerialCommunicationListener mSerialCommunicationListener = null;
    private CloseCaptionListener mCloseCaptionListener = null;
    private StatusSourceConnectListener mSourceConnectChangeListener = null;
    private HDMIRxCECListener mHDMIRxCECListener = null;
    private UpgradeFBCListener mUpgradeFBCListener  = null;
    private SubtitleUpdateListener mSubtitleListener = null;
    private ScannerEventListener mScannerListener = null;
    private VframBMPEventListener mVframBMPListener = null;
    private EpgEventListener mEpgListener = null;
    private AVPlaybackListener mAVPlaybackListener = null;
    private VchipLockStatusListener mLockStatusListener = null;
    // new Tv obj
    public static TvControlManager CreateTv() {
        return new TvControlManager();
    }

    private static TvControlManager tvInstance;
    // new Tv obj
    public static TvControlManager open() {
        if (tvInstance == null)
            tvInstance = new TvControlManager();
        return tvInstance;
    }

    public TvControlManager() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }
        native_setup(new WeakReference<TvControlManager>(this));
        String LogFlg=TvMiscConfigGet(OPENTVLOGFLG,null);
        if ("log_open".equals(TvMiscConfigGet(OPENTVLOGFLG,null)))
            tvLogFlg =true;
    }

    protected void finalize() {
        //native_release();
    }

    // when app exit, need release manual
    public final void release() {
        libtv_log_open();
        native_release();
    }

    public enum SourceInput {
        TV(0),
            AV1(1),
            AV2(2),
            YPBPR1(3),
            YPBPR2(4),
            HDMI1(5),
            HDMI2(6),
            HDMI3(7),
            VGA(8),
            XXXX(9),//not use MPEG source
            DTV(10),
            SVIDEO(11),
            HDMI4K2K(12),
            USB4K2K(13),
            IPTV(14),
            DUMMY(15),
            MAX(16);
        private int val;

        SourceInput(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum SourceInput_Type {
        SOURCE_TYPE_TV(0),
            SOURCE_TYPE_AV(1),
            SOURCE_TYPE_COMPONENT(2),
            SOURCE_TYPE_HDMI(3),
            SOURCE_TYPE_VGA(4),
            SOURCE_TYPE_MPEG(5),//only use for vpp, for display ,not a source
            SOURCE_TYPE_DTV(6),
            SOURCE_TYPE_SVIDEO(7),
            SOURCE_TYPE_HDMI_4K2K(8),
            SOURCE_TYPE_USB_4K2K(9),
            SOURCE_TYPE_MAX(7);

        private int val;

        SourceInput_Type(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum tvin_color_system_e {
        COLOR_SYSTEM_AUTO(0),
            COLOR_SYSTEM_PAL(1),
            COLOR_SYSTEM_NTSC(2),
            COLOR_SYSTEM_SECAM(3),
            COLOR_SYSTEM_MAX(4);
        private int val;

        tvin_color_system_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum tv_program_type {//program_type
        TV_PROGRAM_UNKNOWN(0),
            TV_PROGRAM_DTV(1),
            TV_PROGRAM_DRADIO(2),
            TV_PROGRAM_ATV(3);
        private int val;

        tv_program_type(int val)
        {
            this.val = val;
        }

        public int toInt()
        {
            return this.val;
        }
    }

    public enum program_skip_type_e {
        TV_PROGRAM_SKIP_NO(0),
            TV_PROGRAM_SKIP_YES(1),
            TV_PROGRAM_SKIP_UNKNOWN(2);

        private int val;

        program_skip_type_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum atsc_attenna_type_t {
        AM_ATSC_ATTENNA_TYPE_MIX(0),
            AM_ATSC_ATTENNA_TYPE_AIR(1),
            AM_ATSC_ATTENNA_TYPE_CABLE_STD(2),
            AM_ATSC_ATTENNA_TYPE_CABLE_IRC(3),
            AM_ATSC_ATTENNA_TYPE_CABLE_HRC(4),
            AM_ATSC_ATTENNA_TYPE_MAX(5);

        private int val;
        atsc_attenna_type_t(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum atv_audio_std_e {
        ATV_AUDIO_STD_DK(0),
            ATV_AUDIO_STD_I(1),
            ATV_AUDIO_STD_BG(2),
            ATV_AUDIO_STD_M(3),
            ATV_AUDIO_STD_L(4),
            ATV_AUDIO_STD_AUTO(5),
            ATV_AUDIO_STD_MUTE(6);

        private int val;

        atv_audio_std_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
        public static  atv_audio_std_e  valueOf(int val) {    //  int to enum
            switch (val) {
                case 0:
                    return ATV_AUDIO_STD_DK;
                case 1:
                    return ATV_AUDIO_STD_I;
                case 2:
                    return ATV_AUDIO_STD_BG;
                case 3:
                    return ATV_AUDIO_STD_M;
                case 4:
                    return ATV_AUDIO_STD_L;
                case 5:
                    return ATV_AUDIO_STD_AUTO;
                case 6:
                    return ATV_AUDIO_STD_MUTE;
                default:
                    return null;
            }
        }

        public int value() {
            return this.val;
        }
    }

    public enum atv_video_std_e {
        ATV_VIDEO_STD_AUTO(0),
            ATV_VIDEO_STD_PAL(1),
            ATV_VIDEO_STD_NTSC(2),
            ATV_VIDEO_STD_SECAM(3);

        private int val;

        atv_video_std_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }

        public static  atv_video_std_e  valueOf(int val) {    //  int to enum
            switch (val) {
                case 0:
                    return ATV_VIDEO_STD_AUTO;
                case 1:
                    return ATV_VIDEO_STD_PAL;
                case 2:
                    return ATV_VIDEO_STD_NTSC;
                case 3:
                    return ATV_VIDEO_STD_SECAM;
                default:
                    return null;
            }
        }

        public int value() {
            return this.val;
        }
    }

    // Tv function
    // public int OpenTv();

    /**
     * @Function: CloseTv
     * @Description: Close Tv module
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int CloseTv() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(CLOSE_TV);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int StartTv() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(START_TV);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum TvRunStatus_s {
        TV_INIT_ED(-1),
            TV_OPEN_ED (0),
            TV_START_ED(1),
            TV_RESUME_ED(2),
            TV_PAUSE_ED(3),
            TV_STOP_ED(4),
            TV_CLOSE_ED(5);

        private int val;

        TvRunStatus_s(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }

        public static  TvRunStatus_s  valueOf(int val) {    //  int to enum
            switch (val) {
                case -1:
                    return TV_INIT_ED;
                case 0:
                    return TV_OPEN_ED;
                case 1:
                    return TV_START_ED;
                case 2:
                    return TV_RESUME_ED;
                case 3:
                    return TV_PAUSE_ED;
                case 4:
                    return TV_STOP_ED;
                case 5:
                    return TV_CLOSE_ED;
                default:
                    return null;
            }
        }
    }

    public TvRunStatus_s GetTvRunStatus()
    {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_TV_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return TvRunStatus_s.valueOf(ret);
    }
    /**
     * @Function: StopTv
     * @Description: Stop Tv module
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int StopTv() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(STOP_TV);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetLastSourceInput
     * @Description: Get last source input
     * @Param:
     * @Return: refer to enum SourceInput
     */
    public int GetLastSourceInput() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(GET_LAST_SOURCE_INPUT);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: GetCurrentSourceInput
     * @Description: Get current source input
     * @Param:
     * @Return: refer to enum SourceInput
     */
    public int GetCurrentSourceInput() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CURRENT_SOURCE_INPUT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurrentSourceInputType
     * @Description: Get current source input type
     * @Param:
     * @Return: refer to enum SourceInput_Type
     */
    public SourceInput_Type GetCurrentSourceInputType() {
        libtv_log_open();
        int source_input = GetCurrentSourceInput();
        if (source_input == SourceInput.TV.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_TV;
        } else if (source_input == SourceInput.AV1.toInt() || source_input == SourceInput.AV2.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_AV;
        } else if (source_input == SourceInput.YPBPR1.toInt() || source_input == SourceInput.YPBPR2.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_COMPONENT;
        } else if (source_input == SourceInput.VGA.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_VGA;
        } else if (source_input == SourceInput.HDMI1.toInt() || source_input == SourceInput.HDMI2.toInt() || source_input == SourceInput.HDMI3.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_HDMI;
        } else if (source_input == SourceInput.DTV.toInt()) {
            return SourceInput_Type.SOURCE_TYPE_DTV;
        } else {
            return SourceInput_Type.SOURCE_TYPE_MPEG;
        }
    }

    /**
     * @Function: GetCurrentSignalInfo
     * @Description: Get current signal infomation
     * @Param:
     * @Return: refer to class tvin_info_t
     */
    public tvin_info_t GetCurrentSignalInfo() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CURRENT_SIGNAL_INFO);
        sendCmdToTv(cmd, r);
        tvin_info_t sig_info = new tvin_info_t();
        sig_info.trans_fmt = tvin_trans_fmt.values()[r.readInt()];
        sig_info.fmt = tvin_sig_fmt_e.valueOf(r.readInt());
        sig_info.status = tvin_sig_status_t.values()[r.readInt()];
        sig_info.reserved = r.readInt();
        return sig_info;
    }

    /**
     * @Function: SetSourceInput
     * @Description: Set source input to switch source,
     * @Param: source_input, refer to enum SourceInput; win_pos, refer to class window_pos_t
     * @Return: 0 success, -1 fail
     */
    public int SetSourceInput(SourceInput source_input) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_SOURCE_INPUT);
        cmd.writeInt(source_input.toInt());
        /*int tmp_res_info = GetDisplayResolutionInfo();
          cmd.writeInt(0);
          cmd.writeInt(0);
          cmd.writeInt(((tmp_res_info >> 16) & 0xFFFF) - 1);
          cmd.writeInt(((tmp_res_info >> 0) & 0xFFFF) - 1);*/
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: IsDviSignal
     * @Description: To check if current signal is dvi signal
     * @Param:
     * @Return: true, false
     */
    public boolean IsDviSignal() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(IS_DVI_SIGNAL);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ((ret == 1) ? true:false);
    }

    /**
     * @Function: IsPcFmtTiming
     * @Description: To check if current hdmi signal is pc signal
     * @Param:
     * @Return: true, false
     */
    public boolean IsPcFmtTiming() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(IS_VGA_TIMEING_IN_HDMI);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ((ret == 1) ? true:false);
    }

    /**
     * @Function: GetVideoStreamStatus
     * @Description: Get video stream status to check decoder is actvie or inactive.
     * @Param:
     * @Return: 1 active, 0 inactive
     */
    public int GetVideoStreamStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_VIDEO_STREAM_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum first_start_type  {
        CC_FIRST_START_TYPE_UI_SWITCH_NONE(0),
            CC_FIRST_START_TYPE_UI_SWITCH_HOME(1),
            CC_FIRST_START_TYPE_UI_SWITCH_SOURCE(2);
        private int val;

        first_start_type(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }
    /**
     * @Function: GetFirstStartSwitchType
     * @Description: Get first start switch type.
     * @Param:
     * @Return: reference as enum first_start_type
     */
    public int GetFirstStartSwitchType() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_FIRST_START_SWITCH_TYPE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    /**
     * @Function: SetPreviewWindow
     * @Description: Set source input preview window axis
     * @Param: win_pos, refer to class window_pos_t
     * @Return: 0 success, -1 fail
     */
    public int SetPreviewWindow(int x1, int y1, int x2, int y2) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_PREVIEW_WINDOW);
        cmd.writeInt(x1);
        cmd.writeInt(y1);
        cmd.writeInt(x2);
        cmd.writeInt(y2);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetDisableVideo
     * @Description: to enable/disable video
     * @Param: value 0/1
     * @Return: 0 success, -1 fail
     */
    public int SetDisableVideo(int arg0) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_VIDEO_DISABLE);
        cmd.writeInt(arg0);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum tv_source_connect_status_t {
        CC_SOURCE_PLUG_OUT(0),
            CC_SOURCE_PLUG_IN(1);

        private int val;

        tv_source_connect_status_t(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: GetSourceConnectStatus
     * @Description: Get source connect status
     * @Param: source_input, refer to enum SourceInput
     * @Return: refer to enum tv_source_connect_status_t
     */
    public int GetSourceConnectStatus(SourceInput source_input) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_SOURCE_CONNECT_STATUS);
        cmd.writeInt(source_input.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    // Tv function END

    // VGA

    /**
     * @Function: RunVGAAutoAdjust
     * @Description: Do vag auto adjustment
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int RunVGAAutoAdjust() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(RUN_VGA_AUTO_ADJUST);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetVGAAutoAdjustStatus
     * @Description: Get vag auto adjust status
     * @Param:
     * @Return: refer to enum tvin_process_status_t
     */
    public int GetVGAAutoAdjustStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_VGA_AUTO_ADJUST_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: IsVGAAutoAdjustDone
     * @Description: To check if vag auto adjustment is done.
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int IsVGAAutoAdjustDone(tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(IS_VGA_AUTO_ADJUST_DONE);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetVGAHPos
     * @Description: Adjust vag h pos
     * @Param: value h pos, fmt current signal fmt
     * @Return: 0 success, -1 fail
     */
    public int SetVGAHPos(int value, tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_VGA_HPOS);

        cmd.writeInt(value);
        cmd.writeInt(fmt.toInt());

        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetVGAHPos
     * @Description: Get vag h pos
     * @Param: fmt current signal fmt
     * @Return: h pos
     */
    public int GetVGAHPos(tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_VGA_HPOS);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetVGAVPos
     * @Description: Adjust vag v pos
     * @Param: value v pos, fmt current signal fmt
     * @Return: 0 success, -1 fail
     */
    public int SetVGAVPos(int value, tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_VGA_VPOS);
        cmd.writeInt(value);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetVGAVPos
     * @Description: Get vag v pos
     * @Param: fmt current signal fmt
     * @Return: v pos
     */
    public int GetVGAVPos(tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_VGA_VPOS);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetVGAClock
     * @Description: Adjust vag clock
     * @Param: value clock, fmt current signal fmt
     * @Return: 0 success, -1 fail
     */
    public int SetVGAClock(int value, tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_VGA_CLOCK);
        cmd.writeInt(value);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetVGAClock
     * @Description: Get vag clock
     * @Param: fmt current signal fmt
     * @Return: vga clock
     */
    public int GetVGAClock(tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_VGA_CLOCK);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetVGAPhase
     * @Description: Adjust vag phase
     * @Param: value clock, fmt current signal fmt
     * @Return: 0 success, -1 fail
     */
    public int SetVGAPhase(int value, tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_VGA_PHASE);
        cmd.writeInt(value);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetVGAPhase
     * @Description: Get vag phase
     * @Param: fmt current signal fmt
     * @Return: vga phase
     */
    public int GetVGAPhase(tvin_sig_fmt_e fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_VGA_PHASE);
        cmd.writeInt(fmt.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetVGAParamDefault
     * @Description: reset vag param
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SetVGAParamDefault() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_VGAPARAM_DEFAULT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    // VGA END

    // 3D
    public enum Mode_3D {
        MODE_3D_CLOSE(0),
            MODE_3D_AUTO(1),
            //        MODE_3D_2D_TO_3D(2),
            MODE_3D_LEFT_RIGHT(2),
            MODE_3D_UP_DOWN(3),
            MODE_3D_LINE_ALTERNATIVE(4),
            MODE_3D_FRAME_ALTERNATIVE(5),
            MODE_3D_MAX(6);

        private int val;

        Mode_3D(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum Tvin_3d_Status {
        STATUS3D_DISABLE(0),
            STATUS3D_AUTO(1),
            //        STATUS3D_2D_TO_3D(2),
            STATUS3D_LR(2),
            STATUS3D_BT(3),
            STATUS3D_LINE_ALTERNATIVE(4),
            STATUS3D_FRAME_ALTERNATIVE(5),
            STATUS3D_MAX(6);
        private int val;

        Tvin_3d_Status(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum Mode_3D_2D {
        MODE_3D_2D_CLOSE(0),
            MODE_3D_2D_LEFT(1),
            MODE_3D_2D_RIGHT(2);

        private int val;

        Mode_3D_2D(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public int Get3DStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_3D_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Set3DMode(Mode_3D mode, Tvin_3d_Status status) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_3D_MODE);
        cmd.writeInt(mode.toInt());
        cmd.writeInt(status.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }


    public int Get3DMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_3D_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Set3DLRSwith(int on_off, Tvin_3d_Status status) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_3D_LR_SWITH);
        cmd.writeInt(on_off);
        cmd.writeInt(status.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Get3DLRSwith() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_3D_LR_SWITH);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Set3DTo2DMode(Mode_3D_2D mode, Tvin_3d_Status status) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_3D_TO_2D_MODE);
        cmd.writeInt(mode.toInt());
        cmd.writeInt(status.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Get3DTo2DMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_3D_TO_2D_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Set3DDepth(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_3D_DEPTH);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Get3DDepth() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_3D_DEPTH);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    // 3D END

    // PQ

    /**
     * @Function: SetBrightness
     * @Description: Set current source brightness value
     * @Param: value brightness, source refer to enum SourceInput_Type, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetBrightness(int value, SourceInput_Type source,  int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_BRIGHTNESS);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetBrightness
     * @Description: Get current source brightness value
     * @Param: source refer to enum SourceInput_Type
     * @Return: value brightness
     */
    public int GetBrightness(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_BRIGHTNESS);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveBrightness
     * @Description: Save current source brightness value
     * @Param: value brightness, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveBrightness(int value, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_BRIGHTNESS);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetContrast
     * @Description: Set current source contrast value
     * @Param: value contrast, source refer to enum SourceInput_Type, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetContrast(int value, SourceInput_Type source, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_CONTRAST);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetContrast
     * @Description: Get current source contrast value
     * @Param: source refer to enum SourceInput_Type
     * @Return: value contrast
     */
    public int GetContrast(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CONTRAST);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveContrast
     * @Description: Save current source contrast value
     * @Param: value contrast, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveContrast(int value, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CONTRAST);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetSatuation
     * @Description: Set current source saturation value
     * @Param: value saturation, source refer to enum SourceInput_Type, fmt current fmt refer to tvin_sig_fmt_e, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetSaturation(int value, SourceInput_Type source, tvin_sig_fmt_e fmt, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_SATURATION);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        cmd.writeInt(fmt.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetSatuation
     * @Description: Get current source saturation value
     * @Param: source refer to enum SourceInput_Type
     * @Return: value saturation
     */
    public int GetSaturation(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_SATURATION);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveSaturation
     * @Description: Save current source saturation value
     * @Param: value saturation, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveSaturation(int value, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_SATURATION);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetHue
     * @Description: Set current source hue value
     * @Param: value saturation, source refer to enum SourceInput_Type, fmt current fmt refer to tvin_sig_fmt_e, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetHue(int value, SourceInput_Type source, tvin_sig_fmt_e fmt, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_HUE);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        cmd.writeInt(fmt.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetHue
     * @Description: Get current source hue value
     * @Param: source refer to enum SourceInput_Type
     * @Return: value hue
     */
    public int GetHue(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_HUE);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveHue
     * @Description: Save current source hue value
     * @Param: value hue, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveHue(int value, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_HUE);
        cmd.writeInt(value);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum Scene_Mode {
        SCENE_MODE_STANDARD(0),
            SCENE_MODE_GAME(1),
            SCENE_MODE_FILM(2),
            SCENE_MODE_USER(3),
            SCENE_MODE_MAX(4);

        private int val;
        Scene_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public int SetSceneMode(Scene_Mode scene_mode,int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_SCENEMODE);
        cmd.writeInt(scene_mode.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd,r);
        int ret = r.readInt();
        return ret;
    }

    public int GetSCENEMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r  = Parcel.obtain();
        cmd.writeInt(GET_SCENEMODE);
        sendCmdToTv(cmd,r);
        int ret = r.readInt();
        return ret;
    }

    public enum Pq_Mode {
        PQ_MODE_STANDARD(0),
            PQ_MODE_BRIGHT(1),
            PQ_MODE_SOFTNESS(2),
            PQ_MODE_USER(3),
            PQ_MODE_MOVIE(4),
            PQ_MODE_COLORFUL(5);

        private int val;

        Pq_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SetPQMode
     * @Description: Set current source picture mode
     * @Param: value mode refer to enum Pq_Mode, source refer to enum SourceInput_Type, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetPQMode(Pq_Mode pq_mode, SourceInput_Type source, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_PQMODE);
        cmd.writeInt(pq_mode.toInt());
        cmd.writeInt(source.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetPQMode
     * @Description: Get current source picture mode
     * @Param: source refer to enum SourceInput_Type
     * @Return: picture mode refer to enum Pq_Mode
     */
    public int GetPQMode(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_PQMODE);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SavePQMode
     * @Description: Save current source picture mode
     * @Param: picture mode refer to enum Pq_Mode, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SavePQMode(Pq_Mode pq_mode, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_PQMODE);
        cmd.writeInt(pq_mode.toInt());
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetSharpness
     * @Description: Set current source sharpness value
     * @Param: value saturation, source_type refer to enum SourceInput_Type, is_enable set 1 as default
     * @Param: status_3d refer to enum Tvin_3d_Status, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetSharpness(int value, SourceInput_Type source_type, int is_enable, int status_3d, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_SHARPNESS);
        cmd.writeInt(value);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(is_enable);
        cmd.writeInt(status_3d);
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetSharpness
     * @Description: Get current source sharpness value
     * @Param: source refer to enum SourceInput_Type
     * @Return: value sharpness
     */
    public int GetSharpness(SourceInput_Type source_type) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_SHARPNESS);
        cmd.writeInt(source_type.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveSharpness
     * @Description: Save current source sharpness value
     * @Param: value sharpness, source refer to enum SourceInput_Type, isEnable set 1 enable as default
     * @Return: 0 success, -1 fail
     */
    public int SaveSharpness(int value, SourceInput_Type sourceType, int isEnable) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_SHARPNESS);
        cmd.writeInt(value);
        cmd.writeInt(sourceType.toInt());
        cmd.writeInt(1);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetBacklight
     * @Description: Set current source backlight value
     * @Param: value backlight, source refer to enum SourceInput_Type, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetBacklight(int value, SourceInput_Type source_type, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_BACKLIGHT);
        cmd.writeInt(value);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetBacklight
     * @Description: Get current source backlight value
     * @Param: source refer to enum SourceInput_Type
     * @Return: value backlight
     */
    public int GetBacklight(SourceInput_Type source_type) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_BACKLIGHT);
        cmd.writeInt(source_type.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetBacklight_Switch
     * @Description: Set current backlight switch
     * @Param: value onoff
     * @Return: 0 success, -1 fail
     */
    public int SetBacklight_Switch(int onoff) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_BACKLIGHT_SWITCH);
        cmd.writeInt(onoff);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetBacklight_Switch
     * @Description: Get current backlight switch
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int GetBacklight_Switch() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_BACKLIGHT_SWITCH);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveBacklight
     * @Description: Save current source backlight value
     * @Param: value backlight, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveBacklight(int value, SourceInput_Type source_type) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_BACKLIGHT);
        cmd.writeInt(value);
        cmd.writeInt(source_type.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum color_temperature {
        COLOR_TEMP_STANDARD(0),
            COLOR_TEMP_WARM(1),
            COLOR_TEMP_COLD(2),
            COLOR_TEMP_MAX(3);
        private int val;

        color_temperature(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SetColorTemperature
     * @Description: Set current source color temperature mode
     * @Param: value mode refer to enum color_temperature, source refer to enum SourceInput_Type, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetColorTemperature(color_temperature mode, SourceInput_Type source, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_COLOR_TEMPERATURE);
        cmd.writeInt(mode.toInt());
        cmd.writeInt(source.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetColorTemperature
     * @Description: Get current source color temperature mode
     * @Param: source refer to enum SourceInput_Type
     * @Return: color temperature refer to enum color_temperature
     */
    public int GetColorTemperature(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_COLOR_TEMPERATURE);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveColorTemperature
     * @Description: Save current source color temperature mode
     * @Param: color temperature mode refer to enum color_temperature, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveColorTemp(color_temperature mode, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_COLOR_TEMPERATURE);
        cmd.writeInt(mode.toInt());
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum Display_Mode {
        DISPLAY_MODE_169(0),
            DISPLAY_MODE_PERSON(1),
            DISPLAY_MODE_MOVIE(2),
            DISPLAY_MODE_CAPTION(3),
            DISPLAY_MODE_MODE43(4),
            DISPLAY_MODE_FULL(5),
            DISPLAY_MODE_NORMAL(6),
            DISPLAY_MODE_NOSCALEUP(7),
            DISPLAY_MODE_CROP_FULL(8),
            DISPLAY_MODE_CROP(9),
            DISPLAY_MODE_ZOOM(10),
            DISPLAY_MODE_MAX(11);
        private int val;

        Display_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SetDisplayMode
     * @Description: Set current source display mode
     * @Param: value mode refer to enum Display_Mode, source refer to enum SourceInput_Type, fmt refer to tvin_sig_fmt_e, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetDisplayMode(Display_Mode display_mode, SourceInput_Type source, tvin_sig_fmt_e fmt, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_DISPLAY_MODE);
        cmd.writeInt(display_mode.toInt());
        cmd.writeInt(source.toInt());
        cmd.writeInt(fmt.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetDisplayMode
     * @Description: Get current source display mode
     * @Param: source refer to enum SourceInput_Type
     * @Return: display mode refer to enum Display_Mode
     */
    public int GetDisplayMode(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_DISPLAY_MODE);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveDisplayMode
     * @Description: Save current source display mode
     * @Param: display mode refer to enum Display_Mode, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveDisplayMode(Display_Mode display_mode, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_DISPLAY_MODE);
        cmd.writeInt(display_mode.toInt());
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum Noise_Reduction_Mode {
        REDUCE_NOISE_CLOSE(0),
            REDUCE_NOISE_WEAK(1),
            REDUCE_NOISE_MID(2),
            REDUCE_NOISE_STRONG(3),
            REDUCTION_MODE_AUTO(4);

        private int val;

        Noise_Reduction_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SetNoiseReductionMode
     * @Description: Set current source noise reduction mode
     * @Param: noise reduction mode refer to enum Noise_Reduction_Mode, source refer to enum SourceInput_Type, is_save 1 to save
     * @Return: 0 success, -1 fail
     */
    public int SetNoiseReductionMode(Noise_Reduction_Mode nr_mode, SourceInput_Type source, int is_save) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_NOISE_REDUCTION_MODE);
        cmd.writeInt(nr_mode.toInt());
        cmd.writeInt(source.toInt());
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetNoiseReductionMode
     * @Description: Get current source noise reduction mode
     * @Param: source refer to enum SourceInput_Type
     * @Return: noise reduction mode refer to enum Noise_Reduction_Mode
     */
    public int GetNoiseReductionMode(SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_NOISE_REDUCTION_MODE);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveNoiseReductionMode
     * @Description: Save current source noise reduction mode
     * @Param: noise reduction mode refer to enum Noise_Reduction_Mode, source refer to enum SourceInput_Type
     * @Return: 0 success, -1 fail
     */
    public int SaveNoiseReductionMode(Noise_Reduction_Mode nr_mode, SourceInput_Type source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_NOISE_REDUCTION_MODE);
        cmd.writeInt(nr_mode.toInt());
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // PQ END

    // FACTORY
    public enum TEST_PATTERN {
        TEST_PATTERN_NONE(0),
            TEST_PATTERN_RED(1),
            TEST_PATTERN_GREEN(2),
            TEST_PATTERN_BLUE(3),
            TEST_PATTERN_WHITE(4),
            TEST_PATTERN_BLACK(5),
            TEST_PATTERN_MAX(6);

        private int val;

        TEST_PATTERN(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum NOLINE_PARAMS_TYPE {
        NOLINE_PARAMS_TYPE_BRIGHTNESS(0),
            NOLINE_PARAMS_TYPE_CONTRAST(1),
            NOLINE_PARAMS_TYPE_SATURATION(2),
            NOLINE_PARAMS_TYPE_HUE(3),
            NOLINE_PARAMS_TYPE_SHARPNESS(4),
            NOLINE_PARAMS_TYPE_VOLUME(5),
            NOLINE_PARAMS_TYPE_MAX(6);

        private int val;

        NOLINE_PARAMS_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public class noline_params_t
    {
        public int osd0;
        public int osd25;
        public int osd50;
        public int osd75;
        public int osd100;
    }

    public class tvin_cutwin_t
    {
        public int hs;
        public int he;
        public int vs;
        public int ve;
    }

    /**
     * @Function: FactorySetPQMode_Brightness
     * @Description: Adjust brightness value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode, brightness brightness value
     * @Return: 0 success, -1 fail
     */
    public int FactorySetPQMode_Brightness(SourceInput_Type source_type, int pq_mode, int brightness) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETPQMODE_BRIGHTNESS);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        cmd.writeInt(brightness);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetPQMode_Brightness
     * @Description: Get brightness value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode
     * @Return: 0 success, -1 fail
     */
    public int FactoryGetPQMode_Brightness(SourceInput_Type source_type, int pq_mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETPQMODE_BRIGHTNESS);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetPQMode_Contrast
     * @Description: Adjust contrast value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode, contrast contrast value
     * @Return: contrast value
     */
    public int FactorySetPQMode_Contrast(SourceInput_Type source_type, int pq_mode, int contrast) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETPQMODE_CONTRAST);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        cmd.writeInt(contrast);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetPQMode_Contrast
     * @Description: Get contrast value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode
     * @Return: 0 success, -1 fail
     */
    public int FactoryGetPQMode_Contrast(SourceInput_Type source_type, int pq_mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETPQMODE_CONTRAST);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetPQMode_Saturation
     * @Description: Adjust saturation value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode, saturation saturation value
     * @Return: 0 success, -1 fail
     */
    public int FactorySetPQMode_Saturation(SourceInput_Type source_type, int pq_mode, int saturation) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETPQMODE_SATURATION);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        cmd.writeInt(saturation);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetPQMode_Saturation
     * @Description: Get saturation value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode
     * @Return: saturation value
     */
    public int FactoryGetPQMode_Saturation(SourceInput_Type source_type, int pq_mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETPQMODE_SATURATION);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetPQMode_Hue
     * @Description: Adjust hue value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode, hue hue value
     * @Return: 0 success, -1 fail
     */
    public int FactorySetPQMode_Hue(SourceInput_Type source_type, int pq_mode, int hue) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETPQMODE_HUE);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        cmd.writeInt(hue);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetPQMode_Hue
     * @Description: Get hue value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode
     * @Return: hue value
     */
    public int FactoryGetPQMode_Hue(SourceInput_Type source_type, int pq_mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETPQMODE_HUE);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetPQMode_Sharpness
     * @Description: Adjust sharpness value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode, sharpness sharpness value
     * @Return: 0 success, -1 fail
     */
    public int FactorySetPQMode_Sharpness(SourceInput_Type source_type, int pq_mode, int sharpness) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETPQMODE_SHARPNESS);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        cmd.writeInt(sharpness);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetPQMode_Sharpness
     * @Description: Get sharpness value in corresponding pq mode for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, pq_mode refer to enum Pq_Mode
     * @Return: sharpness value
     */
    public int FactoryGetPQMode_Sharpness(SourceInput_Type source_type, int pq_mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETPQMODE_SHARPNESS);
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(pq_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetTestPattern
     * @Description: Set test patten for factory menu conctrol
     * @Param: pattern refer to enum TEST_PATTERN
     * @Return: 0 success, -1 fail
     */
    public int FactorySetTestPattern(int pattern) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETTESTPATTERN);
        cmd.writeInt(pattern);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetTestPattern
     * @Description: Get current test patten for factory menu conctrol
     * @Param:
     * @Return: patten value refer to enum TEST_PATTERN
     */
    public int FactoryGetTestPattern() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETTESTPATTERN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryResetPQMode
     * @Description: Reset all values of PQ mode for factory menu conctrol
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int FactoryResetPQMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_RESETPQMODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryResetColorTemp
     * @Description: Reset all values of color temperature mode for factory menu conctrol
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int FactoryResetColorTemp() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_RESETCOLORTEMP);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetParamsDefault
     * @Description: Reset all values of pq mode and color temperature mode for factory menu conctrol
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int FactorySetParamsDefault() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_RESETPAMAMSDEFAULT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetDDRSSC
     * @Description: Set ddr ssc level for factory menu conctrol
     * @Param: step ddr ssc level
     * @Return: 0 success, -1 fail
     */
    public int FactorySetDDRSSC(int step) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETDDRSSC);
        cmd.writeInt(step);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetDDRSSC
     * @Description: Get ddr ssc level for factory menu conctrol
     * @Param:
     * @Return: ddr ssc level
     */
    public int FactoryGetDDRSSC() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETDDRSSC);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetLVDSSSC
     * @Description: Set lvds ssc level for factory menu conctrol
     * @Param: step lvds ssc level
     * @Return: 0 success, -1 fail
     */
    public int FactorySetLVDSSSC(int step) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETLVDSSSC);
        cmd.writeInt(step);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetLVDSSSC
     * @Description: Get lvds ssc level for factory menu conctrol
     * @Param:
     * @Return: lvds ssc level
     */
    public int FactoryGetLVDSSSC() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETLVDSSSC);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetNolineParams
     * @Description: Nonlinearize the params of corresponding nolinear param type for factory menu conctrol
     * @Param: noline_params_type refer to enum NOLINE_PARAMS_TYPE, source_type refer to SourceInput_Type, params params value refer to class noline_params_t
     * @Return: 0 success, -1 fail
     */
    public int FactorySetNolineParams(NOLINE_PARAMS_TYPE noline_params_type, SourceInput_Type source_type,
            noline_params_t params) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETNOLINEPARAMS);
        cmd.writeInt(noline_params_type.toInt());
        cmd.writeInt(source_type.toInt());
        cmd.writeInt(params.osd0);
        cmd.writeInt(params.osd25);
        cmd.writeInt(params.osd50);
        cmd.writeInt(params.osd75);
        cmd.writeInt(params.osd100);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetNolineParams
     * @Description: Nonlinearize the params of corresponding nolinear param type for factory menu conctrol
     * @Param: noline_params_type refer to enum NOLINE_PARAMS_TYPE, source_type refer to SourceInput_Type
     * @Return: params value refer to class noline_params_t
     */
    public noline_params_t FactoryGetNolineParams(NOLINE_PARAMS_TYPE noline_params_type, SourceInput_Type source_type) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETNOLINEPARAMS);
        cmd.writeInt(noline_params_type.toInt());
        cmd.writeInt(source_type.toInt());
        sendCmdToTv(cmd, r);
        noline_params_t noline_params = new noline_params_t();
        noline_params.osd0 = r.readInt();
        noline_params.osd25 = r.readInt();
        noline_params.osd50 = r.readInt();
        noline_params.osd75 = r.readInt();
        noline_params.osd100 = r.readInt();
        return noline_params;
    }

    /**
     * @Function: FactorySetOverscanParams
     * @Description: Set overscan params of corresponding source type and fmt for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, fmt refer to enum tvin_sig_fmt_e, status_3d refer to enum Tvin_3d_Status
     * @Param: trans_fmt refer to enum tvin_trans_fmt, cutwin_t refer to class tvin_cutwin_t
     * @Return: 0 success, -1 fail
     */
    public int FactorySetOverscanParams(SourceInput_Type source_type, tvin_sig_fmt_e fmt, Tvin_3d_Status status_3d,
            tvin_trans_fmt trans_fmt, tvin_cutwin_t cutwin_t) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETOVERSCAN);
        cmd.writeInt(source_type.ordinal());
        cmd.writeInt(fmt.toInt());
        cmd.writeInt(status_3d.ordinal());
        cmd.writeInt(trans_fmt.ordinal());
        cmd.writeInt(cutwin_t.hs);
        cmd.writeInt(cutwin_t.he);
        cmd.writeInt(cutwin_t.vs);
        cmd.writeInt(cutwin_t.ve);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetOverscanParams
     * @Description: Get overscan params of corresponding source type and fmt for factory menu conctrol
     * @Param: source_type refer to enum SourceInput_Type, fmt refer to enum tvin_sig_fmt_e, status_3d refer to enum Tvin_3d_Status
     * @Param: trans_fmt refer to enum tvin_trans_fmt
     * @Return: cutwin_t value for overscan refer to class tvin_cutwin_t
     */
    public tvin_cutwin_t FactoryGetOverscanParams(SourceInput_Type source_type, tvin_sig_fmt_e fmt,
            Tvin_3d_Status status_3d, tvin_trans_fmt trans_fmt) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETOVERSCAN);
        cmd.writeInt(source_type.ordinal());
        cmd.writeInt(fmt.toInt());
        cmd.writeInt(status_3d.ordinal());
        cmd.writeInt(trans_fmt.ordinal());
        sendCmdToTv(cmd, r);
        tvin_cutwin_t cutwin_t = new tvin_cutwin_t();
        cutwin_t.hs = r.readInt();
        cutwin_t.he = r.readInt();
        cutwin_t.vs = r.readInt();
        cutwin_t.ve = r.readInt();
        return cutwin_t;
    }

    /**
     * @Function: FactorySSMSetOutDefault
     * @Description: Reset all factory params in SSM
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int FactorySSMSetOutDefault() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SET_OUT_DEFAULT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryGetGlobal_OGO_Offset_RGain() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETGLOBALOGO_RGAIN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryGetGlobal_OGO_Offset_GGain() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETGLOBALOGO_GGAIN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryGetGlobal_OGO_Offset_BGain() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETGLOBALOGO_BGAIN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryGetGlobal_OGO_Offset_ROffset() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETGLOBALOGO_ROFFSET);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryGetGlobal_OGO_Offset_GOffset() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETGLOBALOGO_GOFFSET);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryGetGlobal_OGO_Offset_BOffset() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETGLOBALOGO_BOFFSET);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactorySetGlobal_OGO_Offset_RGain(int rgain) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETGLOBALOGO_RGAIN);
        cmd.writeInt(rgain);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactorySetGlobal_OGO_Offset_GGain(int ggain) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETGLOBALOGO_GGAIN);
        cmd.writeInt(ggain);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactorySetGlobal_OGO_Offset_BGain(int bgain) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETGLOBALOGO_BGAIN);
        cmd.writeInt(bgain);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactorySetGlobal_OGO_Offset_ROffset(int roffset) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETGLOBALOGO_ROFFSET);
        cmd.writeInt(roffset);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactorySetGlobal_OGO_Offset_GOffset(int goffset) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETGLOBALOGO_GOFFSET);
        cmd.writeInt(goffset);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactorySetGlobal_OGO_Offset_BOffset(int boffset) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETGLOBALOGO_BOFFSET);
        cmd.writeInt(boffset);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryCleanAllTableForProgram() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_CLEAN_ALL_TABLE_FOR_PROGRAM);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGetAdbdStatus
     * @Description: factory get adbd status
     * @Param: none
     * @Return: 0 off ; 1 on ; -1 error
     */
    public int FactoryGetAdbdStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GETADBD_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySetAdbdSwitch
     * @Description: factory set adbd switch
     * @Param: adbd_switch(0 : off ; 1 : on)
     * @Return: 0 success ; -1 failed ; -2 param error
     */
    public int FactorySetAdbdSwitch(int adbd_switch) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETADBD_SWITCH);
        cmd.writeInt(adbd_switch);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }


    public int FactorySetPatternYUV(int mask, int y, int u, int v)
    {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SETPATTERN_YUV);
        cmd.writeInt(mask);
        cmd.writeInt(y);
        cmd.writeInt(u);
        cmd.writeInt(v);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // FACTORY END

    // AUDIO
    // Audio macro declare
    public enum CC_AUDIO_MUTE_STATUS {
        CC_MUTE_ON(0),
            CC_MUTE_OFF(1);

        private int val;

        CC_AUDIO_MUTE_STATUS(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }
    public enum CC_AUDIO_SWITCH_STATUS {
        CC_SWITCH_OFF(0),
            CC_SWITCH_ON(1);

        private int val;

        CC_AUDIO_SWITCH_STATUS(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum Sound_Mode {
        SOUND_MODE_STD(0),
            SOUND_MODE_MUSIC(1),
            SOUND_MODE_NEWS(2),
            SOUND_MODE_THEATER(3),
            SOUND_MODE_USER(4);

        private int val;

        Sound_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum EQ_Mode {
        EQ_MODE_NORMAL(0),
            EQ_MODE_POP(1),
            EQ_MODE_JAZZ(2),
            EQ_MODE_ROCK(3),
            EQ_MODE_CLASSIC(4),
            EQ_MODE_DANCE(5),
            EQ_MODE_PARTY(6),
            EQ_MODE_BASS(7),
            EQ_MODE_TREBLE(8),
            EQ_MODE_CUSTOM(9);

        private int val;

        EQ_Mode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_AUD_SPDIF_MODE {
        CC_SPDIF_MODE_PCM(0),
            CC_SPDIF_MODE_SOURCE(1);

        private int val;

        CC_AUD_SPDIF_MODE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_AMAUDIO_OUT_MODE {
        CC_AMAUDIO_OUT_MODE_DIRECT(0),
            CC_AMAUDIO_OUT_MODE_MIX(1);

        private int val;

        CC_AMAUDIO_OUT_MODE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    // Audio Mute

    /**
     * @Function: SetAudioMuteKeyStatus
     * @Description: Set audio mute or unmute according to mute key press up or press down
     * @Param: KeyStatus refer to enum CC_AUDIO_MUTE_KEY_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioMuteKeyStatus(int KeyStatus) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_MUTEKEY_STATUS);
        cmd.writeInt(KeyStatus);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioMuteKeyStatus
     * @Description: Get audio mute or unmute key
     * @Param:
     * @Return: KeyStatus value refer to enum CC_AUDIO_MUTE_KEY_STATUS
     */
    public int GetAudioMuteKeyStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_MUTEKEY_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAudioForceMuteStatus
     * @Description: Set audio mute or unmute by force
     * @Param: ForceMuteStatus refer to enum CC_AUDIO_MUTE_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioForceMuteStatus(int ForceMuteStatus) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_FORCE_MUTE_STATUS);
        cmd.writeInt(ForceMuteStatus);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioForceMuteStatus
     * @Description: Get audio mute status
     * @Param:
     * @Return: mute status value refer to enum CC_AUDIO_MUTE_STATUS
     */
    public int GetAudioForceMuteStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_FORCE_MUTE_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAudioAVoutMute
     * @Description: Set av out mute
     * @Param: AvoutMuteStatus refer to enum CC_AUDIO_MUTE_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioAVoutMute(int AvoutMuteStatus) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_AVOUT_MUTE_STATUS);
        cmd.writeInt(AvoutMuteStatus);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioAVoutMute
     * @Description: Get av out mute status
     * @Param:
     * @Return: av out mute status value refer to enum CC_AUDIO_MUTE_STATUS
     */
    public int GetAudioAVoutMute() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_AVOUT_MUTE_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAudioSPDIFMute
     * @Description: Set spdif mute
     * @Param: SPDIFMuteStatus refer to enum CC_AUDIO_MUTE_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSPDIFMute(int SPDIFMuteStatus) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SPDIF_MUTE_STATUS);
        cmd.writeInt(SPDIFMuteStatus);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSPDIFMute
     * @Description: Get spdif mute status
     * @Param:
     * @Return: spdif mute status value refer to enum CC_AUDIO_MUTE_STATUS
     */
    public int GetAudioSPDIFMute() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SPDIF_MUTE_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio Master Volume

    /**
     * @Function: SetAudioMasterVolume
     * @Description: Set audio master volume
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SetAudioMasterVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_MASTER_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioMasterVolume
     * @Description: Get audio master volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioMasterVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_MASTER_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioMasterVolume
     * @Description: Save audio master volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioMasterVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_MASTER_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioMasterVolume
     * @Description: Get audio master volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioMasterVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_MASTER_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio Balance

    /**
     * @Function: SetAudioBalance
     * @Description: Set audio banlance
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SetAudioBalance(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_BALANCE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioBalance
     * @Description: Get audio balance
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioBalance() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_BALANCE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioBalance
     * @Description: Save audio balance(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioBalance(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_BALANCE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioBalance
     * @Description: Get audio balance(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioBalance() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_BALANCE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio SupperBass Volume

    /**
     * @Function: SetAudioSupperBassVolume
     * @Description: Get audio supperbass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int SetAudioSupperBassVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SUPPER_BASS_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSupperBassVolume
     * @Description: Get audio supperbass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioSupperBassVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SUPPER_BASS_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSupperBassVolume
     * @Description: Save audio supperbass volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSupperBassVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SUPPER_BASS_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSupperBassVolume
     * @Description: Get audio supperbass volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioSupperBassVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SUPPER_BASS_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio SupperBass Switch

    /**
     * @Function: SetAudioSupperBassSwitch
     * @Description: Set audio supperbass switch
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSupperBassSwitch(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SUPPER_BASS_SWITCH);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSupperBassSwitch
     * @Description: Get audio supperbass switch
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetSaveAudioSupperBassSwitch() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SUPPER_BASS_SWITCH);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSupperBassSwitch
     * @Description: Save audio supperbass switch(stored in flash)
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSupperBassSwitch(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SUPPER_BASS_SWITCH);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSupperBassSwitch
     * @Description: Get audio supperbass switch(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetCurAudioSupperBassSwitch() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SUPPER_BASS_SWITCH);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio SRS Surround switch

    /**
     * @Function: SetAudioSrsSurround
     * @Description: Set audio SRS Surround switch
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSrsSurround(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SRS_SURROUND);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSrsSurround
     * @Description: Get audio SRS Surround switch
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetSaveAudioSrsSurround() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SRS_SURROUND);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSrsSurround
     * @Description: Save audio SRS Surround switch(stored in flash)
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSrsSurround(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SRS_SURROUND);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSrsSurround
     * @Description: Get audio SRS Surround switch(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetCurAudioSrsSurround() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SRS_SURROUND);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio SRS Dialog Clarity

    /**
     * @Function: SetAudioSrsDialogClarity
     * @Description: Set audio SRS Dialog Clarity switch
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSrsDialogClarity(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SRS_DIALOG_CLARITY);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSrsDialogClarity
     * @Description: Get audio SRS Dialog Clarity switch
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetSaveAudioSrsDialogClarity() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SRS_DIALOG_CLARITY);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSrsDialogClarity
     * @Description: Save audio SRS Dialog Clarity switch(stored in flash)
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSrsDialogClarity(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SRS_DIALOG_CLARITY);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSrsDialogClarity
     * @Description: Get audio SRS Dialog Clarity switch(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetCurAudioSrsDialogClarity() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SRS_DIALOG_CLARITY);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio SRS Trubass

    /**
     * @Function: SetAudioSrsTruBass
     * @Description: Set audio SRS TruBass switch
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSrsTruBass(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SRS_TRU_BASS);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSrsTruBass
     * @Description: Get audio SRS TruBass switch
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetSaveAudioSrsTruBass() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SRS_TRU_BASS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSrsTruBass
     * @Description: Save audio SRS TruBass switch(stored in flash)
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSrsTruBass(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SRS_TRU_BASS);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSrsTruBass
     * @Description: Get audio SRS TruBass switch(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetCurAudioSrsTruBass() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SRS_TRU_BASS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio Bass

    /**
     * @Function: SetAudioBassVolume
     * @Description: Get audio bass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int SetAudioBassVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_BASS_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioBassVolume
     * @Description: Get audio bass volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioBassVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_BASS_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioBassVolume
     * @Description: Save audio bass volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioBassVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_BASS_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioBassVolume
     * @Description: Get audio bass volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioBassVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_BASS_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio Treble

    /**
     * @Function: SetAudioTrebleVolume
     * @Description: Get audio Treble volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int SetAudioTrebleVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_TREBLE_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioTrebleVolume
     * @Description: Get audio Treble volume
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetSaveAudioTrebleVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_TREBLE_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioTrebleVolume
     * @Description: Save audio Treble volume(stored in flash)
     * @Param: value between 0 and 100
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioTrebleVolume(int tmp_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_TREBLE_VOLUME);
        cmd.writeInt(tmp_vol);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioTrebleVolume
     * @Description: Get audio Treble volume(stored in flash)
     * @Param:
     * @Return: value between 0 and 100
     */
    public int GetCurAudioTrebleVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_TREBLE_VOLUME);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio Sound Mode

    /**
     * @Function: SetAudioSoundMode
     * @Description: Get audio sound mode
     * @Param:
     * @Return: value refer to enum Sound_Mode
     */
    public int SetAudioSoundMode(Sound_Mode tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SOUND_MODE);
        cmd.writeInt(tmp_val.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSoundMode
     * @Description: Get audio sound mode
     * @Param:
     * @Return: value refer to enum Sound_Mode
     */
    public int GetSaveAudioSoundMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SOUND_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSoundMode
     * @Description: Save audio sound mode(stored in flash)
     * @Param: value refer to enum Sound_Mode
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSoundMode(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SOUND_MODE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSoundMode
     * @Description: Get audio sound mode(stored in flash)
     * @Param:
     * @Return: value refer to enum Sound_Mode
     */
    public int GetCurAudioSoundMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SOUND_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio Wall Effect
    /**
     * @Function: SetAudioWallEffect
     * @Description: Set audio Wall Effect switch
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioWallEffect(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_WALL_EFFECT);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioWallEffect
     * @Description: Get audio Wall Effect switch
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetSaveAudioWallEffect() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_WALL_EFFECT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioWallEffect
     * @Description: Save audio Wall Effect switch(stored in flash)
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioWallEffect(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_WALL_EFFECT);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioWallEffect
     * @Description: Get audio Wall Effect switch(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetCurAudioWallEffect() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_WALL_EFFECT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio EQ Mode
    /**
     * @Function: SetAudioEQMode
     * @Description: Set audio EQ Mode
     * @Param: value refer to enum EQ_Mode
     * @Return: 0 success, -1 fail
     */
    public int SetAudioEQMode(EQ_Mode tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_EQ_MODE);
        cmd.writeInt(tmp_val.toInt());
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioEQMode
     * @Description: Get audio EQ Mode
     * @Param:
     * @Return: value refer to enum EQ_Mode
     */
    public int GetSaveAudioEQMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_EQ_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioEQMode
     * @Description: Save audio EQ Mode(stored in flash)
     * @Param: value refer to enum EQ_Mode
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioEQMode(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_EQ_MODE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioEQMode
     * @Description: Get audio EQ Mode(stored in flash)
     * @Param:
     * @Return: value refer to enum EQ_Mode
     */
    public int GetCurAudioEQMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_EQ_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    // Audio EQ Gain
    /**
     * @Function: GetAudioEQRange
     * @Description: Get audio EQ Range
     * @Param:
     * @Return: value -128~127
     */
    public int GetAudioEQRange(int range_buf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_EQ_RANGE);
        sendCmdToTv(cmd, r);
        range_buf[0] = r.readInt();
        range_buf[1] = r.readInt();
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioEQBandCount
     * @Description: Get audio EQ band count
     * @Param:
     * @Return: value 0~255
     */
    public int GetAudioEQBandCount() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_EQ_BAND_COUNT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAudioEQGain
     * @Description: Set audio EQ Gain
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int SetAudioEQGain(int gain_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_EQ_GAIN);

        tmp_buf_size = gain_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(gain_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioEQGain
     * @Description: Get audio EQ gain
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int GetAudioEQGain(int gain_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_EQ_GAIN);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();
        for (i = 0; i < tmp_buf_size; i++) {
            gain_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioEQGain
     * @Description: Get audio EQ Gain(stored in flash)
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioEQGain(int gain_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_EQ_GAIN);

        tmp_buf_size = gain_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(gain_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurEQGain
     * @Description: Save audio EQ Gain(stored in flash)
     * @Param: value buffer of eq gain. (range --- get by GetAudioEQRange function)
     * @Return: 0 success, -1 fail
     */
    public int GetCurEQGain(int gain_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_EQ_GAIN);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();

        for (i = 0; i < tmp_buf_size; i++) {
            gain_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAudioEQSwitch
     * @Description: Set audio EQ switch
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioEQSwitch(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_EQ_SWITCH);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    //Audio SPDIF Switch
    /**
     * @Function: SetAudioSPDIFSwitch
     * @Description: Set audio SPDIF Switch
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSPDIFSwitch(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SPDIF_SWITCH);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSPDIFSwitch
     * @Description: Get audio SPDIF Switch
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetSaveAudioSPDIFSwitch() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SPDIF_SWITCH);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSPDIFSwitch
     * @Description: Save audio SPDIF Switch(stored in flash)
     * @Param: value refer to enum CC_AUDIO_SWITCH_STATUS
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSPDIFSwitch(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SPDIF_SWITCH);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSPDIFSwitch
     * @Description: Get audio SPDIF Switch(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUDIO_SWITCH_STATUS
     */
    public int GetCurAudioSPDIFSwitch() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SPDIF_SWITCH);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    //Audio SPDIF Mode
    /**
     * @Function: SetAudioSPDIFMode
     * @Description: Set audio SPDIF Mode
     * @Param: value refer to enum CC_AUD_SPDIF_MODE
     * @Return: 0 success, -1 fail
     */
    public int SetAudioSPDIFMode(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_SPDIF_MODE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetAudioSPDIFMode
     * @Description: Get audio SPDIF Mode
     * @Param:
     * @Return: value refer to enum CC_AUD_SPDIF_MODE
     */
    public int GetSaveAudioSPDIFMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_SPDIF_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SaveCurAudioSPDIFMode
     * @Description: Save audio SPDIF Mode(stored in flash)
     * @Param: value refer to enum CC_AUD_SPDIF_MODE
     * @Return: 0 success, -1 fail
     */
    public int SaveCurAudioSPDIFMode(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_CUR_AUDIO_SPDIF_MODE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: GetCurAudioSPDIFMode
     * @Description: Get audio SPDIF Mode(stored in flash)
     * @Param:
     * @Return: value refer to enum CC_AUD_SPDIF_MODE
     */
    public int GetCurAudioSPDIFMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_CUR_AUDIO_SPDIF_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: OpenAmAudio
     * @Description: Open amaudio module
     * @Param: sr, input sample rate
     * @Return: 0 success, -1 fail
     */
    public int OpenAmAudio(int sr) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(OPEN_AMAUDIO);
        cmd.writeInt(sr);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: CloseAmAudio
     * @Description: Close amaudio module
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int CloseAmAudio() {
        libtv_log_open();
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(CLOSE_AMAUDIO);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAmAudioInputSr
     * @Description: set amaudio input sample rate
     * @Param: sr, input sample rate
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioInputSr(int sr) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AMAUDIO_INPUT_SR);
        cmd.writeInt(sr);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAmAudioOutputMode
     * @Description: set amaudio output mode
     * @Param: mode, amaudio output mode
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioOutputMode(int mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AMAUDIO_OUTPUT_MODE);
        cmd.writeInt(mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAmAudioMusicGain
     * @Description: set amaudio music gain
     * @Param: gain, gain value
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioMusicGain(int gain) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AMAUDIO_MUSIC_GAIN);
        cmd.writeInt(gain);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAmAudioLeftGain
     * @Description: set amaudio left gain
     * @Param: gain, gain value
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioLeftGain(int gain) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AMAUDIO_LEFT_GAIN);
        cmd.writeInt(gain);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetAmAudioRightGain
     * @Description: set amaudio right gain
     * @Param: gain, gain value
     * @Return: 0 success, -1 fail
     */
    public int SetAmAudioRightGain(int gain) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AMAUDIO_RIGHT_GAIN);
        cmd.writeInt(gain);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: AudioHandleHeadsetPlugIn
     * @Description: Audio Handle Headset PlugIn
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int AudioHandleHeadsetPlugIn(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HANDLE_AUDIO_HEADSET_PLUG_IN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: AudioHandleHeadsetPullOut
     * @Description: Audio Handle Headset PullOut
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int AudioHandleHeadsetPullOut(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HANDLE_AUDIO_HEADSET_PULL_OUT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SetCurProgVolumeCompesition
     * @Description: SET Audio Volume Compesition
     * @Param: 0~10
     * @Return: 0 success, -1 fail
     */
    public int SetCurProgVolumeCompesition(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUDIO_VOL_COMP);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * empty api--------------------------------
     * @Function: ATVSaveVolumeCompesition
     * @Description: ATV SAVE Audio Volume Compesition
     * @Param: 0~10
     * @Return: 0 success, -1 fail
     */
    public int ATVSaveVolumeCompesition(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SAVE_AUDIO_VOL_COMP);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: ATVGetVolumeCompesition
     * @Description: Audio Handle Headset PullOut
     * @Param:
     * @Return: 0~10
     */
    public int GetVolumeCompesition() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUDIO_VOL_COMP);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SelectLineInChannel
     * @Description: select line in channel
     * @Param: value 0~7
     * @Return: 0 success, -1 fail
     */
    public int SelectLineInChannel(int channel) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SELECT_LINE_IN_CHANNEL);
        cmd.writeInt(channel);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();

        return ret;
    }

    /**
     * @Function: SetLineInCaptureVolume
     * @Description: set line in capture volume
     * @Param: left chanel volume(0~84)  right chanel volume(0~84)
     * @Return: 0 success, -1 fail
     */
    public int SetLineInCaptureVolume(int l_vol, int r_vol) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SET_LINE_IN_CAPTURE_VOL);
        cmd.writeInt(l_vol);
        cmd.writeInt(r_vol);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SetNoiseGateThreshold
     * @Description: set noise gate threshold
     * @Param: value (0~255)
     * @Return: 0 success, -1 fail
     */
    public void SetNoiseGateThreshold(int thresh) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_NOISE_GATE_THRESHOLD);
        cmd.writeInt(thresh);
        sendCmdToTv(cmd, r);
    }
    // AUDIO END

    // SSM
    public enum CC_SSM_BUS_STATUS {
        CC_SSM_BUS_ON(0),
            CC_SSM_BUS_OFF(1);

        private int val;

        CC_SSM_BUS_STATUS(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SSMInitDevice
     * @Description: Init ssm device
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMInitDevice() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_INIT_DEVICE);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMWriteOneByte
     * @Description: Write one byte to ssm
     * @Param: offset pos in ssm for this byte, val one byte value
     * @Return: 0 success, -1 fail
     */
    public int SSMWriteOneByte(int offset, int val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_ONE_BYTE);
        cmd.writeInt(offset);
        cmd.writeInt(val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadOneByte
     * @Description: Read one byte from ssm
     * @Param: offset pos in ssm for this byte to read
     * @Return: one byte read value
     */
    public int SSMReadOneByte(int offset) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_ONE_BYTE);
        cmd.writeInt(offset);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMWriteNByte
     * @Description: Write n bytes to ssm
     * @Param: offset pos in ssm for the bytes, data_len how many bytes, data_buf n bytes write buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMWriteNBytes(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_SAVE_N_BYTES);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);
        for (i = 0; i < data_len; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadNByte
     * @Description: Read one byte from ssm
     * @Param: offset pos in ssm for the bytes, data_len how many bytes, data_buf n bytes read buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMReadNBytes(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_data_len = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_READ_N_BYTES);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);

        sendCmdToTv(cmd, r);

        data_len = r.readInt();
        for (i = 0; i < data_len; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    public enum POWERON_SOURCE_TYPE  {
        POWERON_SOURCE_TYPE_NONE(0),
            POWERON_SOURCE_TYPE_LAST(1),
            POWERON_SOURCE_TYPE_SETTING(2);
        private int val;

        POWERON_SOURCE_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }


    /**
     * @Function: SSMSavePowerOnOffChannel
     * @Description: Save power on off channel num to ssm for last channel play
     * @Param: channel_type last channel value refer to enum POWERON_SOURCE_TYPE
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePowerOnOffChannel(int channel_type) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_POWER_ON_OFF_CHANNEL);
        cmd.writeInt(channel_type);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadPowerOnOffChannel
     * @Description: Read last channel num from ssm
     * @Param:
     * @Return: last channel num
     */
    public int SSMReadPowerOnOffChannel() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_POWER_ON_OFF_CHANNEL);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveSourceInput
     * @Description: Save current source input to ssm for power on last source select
     * @Param: source_input refer to enum SourceInput.
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSourceInput(int source_input) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_SOURCE_INPUT);
        cmd.writeInt(source_input);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadSourceInput
     * @Description: Read last source input from ssm
     * @Param:
     * @Return: source input value refer to enum SourceInput
     */
    public int SSMReadSourceInput() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_SOURCE_INPUT);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveLastSelectSourceInput
     * @Description: Save last source input to ssm for power on last source select
     * @Param: source_input refer to enum SourceInput, if you wanna save as last source input, just set it as SourceInput.DUMMY.
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveLastSelectSourceInput(int source_input) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_LAST_SOURCE_INPUT);
        cmd.writeInt(source_input);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadLastSelectSourceInput
     * @Description: Read last source input from ssm
     * @Param:
     * @Return: source input value refer to enum SourceInput
     */
    public int SSMReadLastSelectSourceInput() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_LAST_SOURCE_INPUT);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveSystemLanguage
     * @Description: Save system language
     * @Param: tmp_val language id
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSystemLanguage(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_SYS_LANGUAGE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadSystemLanguage
     * @Description: Read last source input from ssm
     * @Param:
     * @Return: language id value
     */
    public int SSMReadSystemLanguage() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_SYS_LANGUAGE);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public int SSMSaveAgingMode(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_AGING_MODE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public int SSMReadAgingMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_AGING_MODE);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSavePanelType
     * @Description: Save panle type for multi-panel select
     * @Param: tmp_val panel type id
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePanelType(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_PANEL_TYPE);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadPanelType
     * @Description: Read panle type id
     * @Param:
     * @Return: panel type id
     */
    public int SSMReadPanelType() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_PANEL_TYPE);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveMacAddress
     * @Description: Save mac address
     * @Param: data_buf write buffer for mac address
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveMacAddress(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_MAC_ADDR);

        tmp_buf_size = data_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadMacAddress
     * @Description: Read mac address
     * @Param: data_buf read buffer for mac address
     * @Return: 0 success, -1 fail
     */
    public int SSMReadMacAddress(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_MAC_ADDR);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();

        for (i = 0; i < tmp_buf_size; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveBarCode
     * @Description: Save bar code
     * @Param: data_buf write buffer for bar code
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveBarCode(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_BAR_CODE);

        tmp_buf_size = data_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadBarCode
     * @Description: Read bar code
     * @Param: data_buf read buffer for bar code
     * @Return: 0 success, -1 fail
     */
    public int SSMReadBarCode(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_BAR_CODE);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();

        for (i = 0; i < tmp_buf_size; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSavePowerOnMusicSwitch
     * @Description: Save power on music on/off flag
     * @Param: tmp_val on off flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePowerOnMusicSwitch(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_POWER_ON_MUSIC_SWITCH);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadPowerOnMusicSwitch
     * @Description: Read power on music on/off flag
     * @Param:
     * @Return: on off flag
     */
    public int SSMReadPowerOnMusicSwitch() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_POWER_ON_MUSIC_SWITCH);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSavePowerOnMusicVolume
     * @Description: Save power on music volume value
     * @Param: tmp_val volume value
     * @Return: 0 success, -1 fail
     */
    public int SSMSavePowerOnMusicVolume(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_POWER_ON_MUSIC_VOL);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadPowerOnMusicVolume
     * @Description: Read power on music volume value
     * @Param:
     * @Return: volume value
     */
    public int SSMReadPowerOnMusicVolume() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_POWER_ON_MUSIC_VOL);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveSystemSleepTimer
     * @Description: Save system sleep timer value
     * @Param: tmp_val sleep timer value
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSystemSleepTimer(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_SYS_SLEEP_TIMER);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadSystemSleepTimer
     * @Description: Read system sleep timer value
     * @Param:
     * @Return: volume value
     */
    public int SSMReadSystemSleepTimer() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_SYS_SLEEP_TIMER);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSetBusStatus
     * @Description: Set i2c bus status
     * @Param: tmp_val bus status value
     * @Return: 0 success, -1 fail
     */
    public int SSMSetBusStatus(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SET_BUS_STATUS);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMGetBusStatus
     * @Description: Get i2c bus status value
     * @Param:
     * @Return: status value
     */
    public int SSMGetBusStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_GET_BUS_STATUS);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveInputSourceParentalControl
     * @Description: Save parental control flag to corresponding source input
     * @Param: source_input refer to enum SourceInput, ctl_flag enable or disable this source input
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveInputSourceParentalControl(int source_input, int ctl_flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_INPUT_SRC_PARENTAL_CTL);
        cmd.writeInt(source_input);
        cmd.writeInt(ctl_flag);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadInputSourceParentalControl
     * @Description: Read parental control flag of corresponding source input
     * @Param: source_input refer to enum SourceInput
     * @Return: parental control flag
     */
    public int SSMReadInputSourceParentalControl(int source_input) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_INPUT_SRC_PARENTAL_CTL);
        cmd.writeInt(source_input);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveInputSourceParentalControl
     * @Description: Save parental control on off flag
     * @Param: switch_flag on off flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveParentalControlSwitch(int switch_flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_PARENTAL_CTL_SWITCH);
        cmd.writeInt(switch_flag);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadParentalControlSwitch
     * @Description: Read parental control on off flag
     * @Param:
     * @Return: on off flag
     */
    public int SSMReadParentalControlSwitch() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_PARENTAL_CTL_SWITCH);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveParentalControlPassWord
     * @Description: Save parental control password
     * @Param: pass_wd_str password string
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveParentalControlPassWord(String pass_wd_str) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_PARENTAL_CTL_PASS_WORD);
        cmd.writeString(pass_wd_str);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadParentalControlPassWord
     * @Description: Read parental control password
     * @Param:
     * @Return: password string
     */
    public String SSMReadParentalControlPassWord() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_PARENTAL_CTL_PASS_WORD);
        sendCmdToTv(cmd, r);
        return r.readString();
    }

    /**
     * @Function: SSMSaveUsingDefaultHDCPKeyFlag
     * @Description: Save use default HDCP key flag
     * @Param: switch_flag enable or disable default key
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveUsingDefaultHDCPKeyFlag(int switch_flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_SAVE_USING_DEF_HDCP_KEY_FLAG);
        cmd.writeInt(switch_flag);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMReadUsingDefaultHDCPKeyFlag
     * @Description: Read use default HDCP key flag
     * @Param:
     * @Return: use flag
     */
    public int SSMReadUsingDefaultHDCPKeyFlag() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_USING_DEF_HDCP_KEY_FLAG);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMGetCustomerDataStart
     * @Description: Get ssm customer data segment start pos
     * @Param:
     * @Return: start offset pos in ssm data segment
     */
    public int SSMGetCustomerDataStart() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_GET_CUSTOMER_DATA_START);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMGetCustomerDataLen
     * @Description: Get ssm customer data segment length
     * @Param:
     * @Return: length
     */
    public int SSMGetCustomerDataLen() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_GET_CUSTOMER_DATA_LEN);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMSaveStandbyMode
     * @Description: Save standby mode, suspend/resume mode or reboot mode
     * @Param: flag standby mode flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveStandbyMode(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_STANDBY_MODE);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadStandbyMode
     * @Description: Read standby mode, suspend/resume mode or reboot mode
     * @Param:
     * @Return: standby mode flag
     */
    public int SSMReadStandbyMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_STANDBY_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveLogoOnOffFlag
     * @Description: Save standby logo on off flag
     * @Param: flag on off
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveLogoOnOffFlag(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_LOGO_ON_OFF_FLAG);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadStandbyMode
     * @Description: Read standby logo on off flag
     * @Param:
     * @Return: on off flag
     */
    public int SSMReadLogoOnOffFlag() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_LOGO_ON_OFF_FLAG);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveHDMIEQMode
     * @Description: Save hdmi eq mode
     * @Param: flag eq mode
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveHDMIEQMode(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_HDMIEQ_MODE);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadHDMIEQMode
     * @Description: Read hdmi eq mode
     * @Param:
     * @Return: hdmi eq mode
     */
    public int SSMReadHDMIEQMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_HDMIEQ_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveHDMIInternalMode
     * @Description: Save hdmi internal mode
     * @Param: flag internal mode
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveHDMIInternalMode(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_HDMIINTERNAL_MODE);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadHDMIInternalMode
     * @Description: Read hdmi internal mode
     * @Param:
     * @Return: hdmi internal mode
     */
    public int SSMReadHDMIInternalMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_HDMIINTERNAL_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveDisable3D
     * @Description: Save disable 3D flag
     * @Param: flag 3d disable flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveDisable3D(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_DISABLE_3D);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadDisable3D
     * @Description: Read disable 3D flag
     * @Param:
     * @Return: disable flag
     */
    public int SSMReadDisable3D() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_DISABLE_3D);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveGlobalOgoEnable
     * @Description: Save enable global ogo flag
     * @Param: flag enable flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveGlobalOgoEnable(int enable) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_GLOBAL_OGOENABLE);
        cmd.writeInt(enable);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadDisable3D
     * @Description: Read enable global ogo flag
     * @Param:
     * @Return: enable flag
     */
    public int SSMReadGlobalOgoEnable() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_GLOBAL_OGOENABLE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveAdbSwitchStatus
     * @Description: Save adb debug enable flag
     * @Param: flag enable flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveAdbSwitchStatus(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_ADB_SWITCH_STATUS);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveSerialCMDSwitchValue
     * @Description: Save serial cmd switch value
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveSerialCMDSwitchValue(int switch_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_SERIAL_CMD_SWITCH_STATUS);
        cmd.writeInt(switch_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadSerialCMDSwitchValue
     * @Description: Save serial cmd switch value
     * @Param:
     * @Return: enable flag
     */
    public int SSMReadSerialCMDSwitchValue() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_SERIAL_CMD_SWITCH_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSetHDCPKey
     * @Description: Save hdmi hdcp key
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMSetHDCPKey() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SET_HDCP_KEY);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMRefreshHDCPKey
     * @Description: Refresh hdmi hdcp key after burn
     * @Param:
     * @Return: 0 success, -1 fail
     */
    public int SSMRefreshHDCPKey()
    {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_REFRESH_HDCPKEY);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveChromaStatus
     * @Description: Save chroma status
     * @Param: flag chroma status on off
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveChromaStatus(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_CHROMA_STATUS);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveCABufferSize
     * @Description: Save dtv ca buffer size
     * @Param: buffersize ca buffer size
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveCABufferSize(int buffersize) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_CA_BUFFER_SIZE);
        cmd.writeInt(buffersize);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadCABufferSize
     * @Description: Read dtv ca buffer size
     * @Param:
     * @Return: size
     */
    public int SSMReadCABufferSize() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_CA_BUFFER_SIZE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveNoiseGateThreshold
     * @Description: Save noise gate threshold
     * @Param: flag noise gate threshold flag
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveNoiseGateThreshold(int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_NOISE_GATE_THRESHOLD_STATUS);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadNoiseGateThreshold
     * @Description: Read noise gate threshold flag
     * @Param:
     * @Return: flag
     */
    public int SSMReadNoiseGateThreshold() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_NOISE_GATE_THRESHOLD_STATUS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum CC_TV_TYPE {
        TV_TYPE_ATV(0),
            TV_TYPE_DVBC(1),
            TV_TYPE_DTMB(2),
            TV_TYPE_ATSC(3),
            TV_TYPE_ATV_DVBC(4),
            TV_TYPE_ATV_DTMB(5),
            TV_TYPE_DVBC_DTMB(6),
            TV_TYPE_ATV_DVBC_DTMB(7);

        private int val;

        CC_TV_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SSMSaveProjectID
     * @Description: Save project id
     * @Param: project id
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveProjectID(int tmp_id) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_PROJECT_ID);
        cmd.writeInt(tmp_id);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadProjectID
     * @Description: Read project id
     * @Param:
     * @Return: return project id
     */
    public int SSMReadProjectID() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_PROJECT_ID);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMSaveHDCPKey
     * @Description: save hdcp key
     * @Param: data_buf write buffer hdcp key
     * @Return: 0 success, -1 fail
     */
    public int SSMSaveHDCPKey(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_SAVE_HDCPKEY);

        tmp_buf_size = data_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMReadHDCPKey
     * @Description: read hdcp key
     * @Param: data_buf read buffer hdcp key
     * @Return: 0 success, -1 fail
     */
    public int SSMReadHDCPKey(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SSM_READ_HDCPKEY);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();

        for (i = 0; i < tmp_buf_size; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }
    // SSM END

    //MISC

    /**
     * @Function: TvMiscPropertySet
     * @Description: Set android property
     * @Param: key_str property name string, value_str property set value string
     * @Return: 0 success, -1 fail
     */
    public int TvMiscPropertySet(String key_str, String value_str) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_PROP_SET);
        cmd.writeString(key_str);
        cmd.writeString(value_str);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: TvMiscPropertySet
     * @Description: Get android property
     * @Param: key_str property name string, value_str property get value string
     * @Return: 0 success, -1 fail
     */
    public String TvMiscPropertyGet(String key_str, String def_str) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_PROP_GET);
        cmd.writeString(key_str);
        cmd.writeString(def_str);
        sendCmdToTv(cmd, r);
        return r.readString();
    }

    /**
     * @Function: TvMiscConfigSet
     * @Description: Set tv config
     * @Param: key_str tv config name string, value_str tv config set value string
     * @Return: 0 success, -1 fail
     */
    public int TvMiscConfigSet(String key_str, String value_str) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_CFG_SET);
        cmd.writeString(key_str);
        cmd.writeString(value_str);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: TvMiscConfigGet
     * @Description: Get tv config
     * @Param: key_str tv config name string, value_str tv config get value string
     * @Return: 0 success, -1 fail
     */
    public String TvMiscConfigGet(String key_str, String def_str) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_CFG_GET);
        cmd.writeString(key_str);
        cmd.writeString(def_str);
        sendCmdToTv(cmd, r);
        return r.readString();
    }

    /**
     * @Function: TvMiscSetGPIOCtrl
     * @Description: Set gpio level
     * @Param: op_cmd_str gpio set cmd string
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetGPIOCtrl(String op_cmd_str) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_SET_GPIO_CTL);
        cmd.writeString(op_cmd_str);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: TvMiscGetGPIOCtrl
     * @Description: Get gpio level
     * @Param: key_str gpio read cmd string, def_str gpio read status string
     * @Return: 0 success, -1 fail
     */
    public int TvMiscGetGPIOCtrl(String key_str, String def_str) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_GET_GPIO_CTL);
        sendCmdToTv(cmd, r);
        return r.readInt();
    }

    /**
     * @Function: TvMiscReadADCVal
     * @Description: Read adc channel status
     * @Param: chan_num for adc channel select
     * @Return: adc read value
     */
    public int TvMiscReadADCVal(int chan_num) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_READ_ADC_VAL);
        cmd.writeInt(chan_num);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: TvMiscSetUserCounter
     * @Description: Enable user counter
     * @Param: counter 1 enable or 0 disable user counter
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetUserCounter(int counter) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_SET_WDT_USER_PET);
        cmd.writeInt(counter);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: TvMiscSetUserCounterTimeOut
     * @Description: Set user counter timeout
     * @Param: counter_timer_out time out number
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetUserCounterTimeOut(int counter_timer_out) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_SET_WDT_USER_COUNTER);
        cmd.writeInt(counter_timer_out);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: TvMiscSetUserPetResetEnable
     * @Description: Enable or disable user pet reset
     * @Param: enable 1 enable or 0 disable
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetUserPetResetEnable(int enable) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_SET_WDT_USER_PET_RESET_ENABLE);
        cmd.writeInt(enable);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum CC_I2C_BUS_STATUS {
        CC_I2C_BUS_ON(0),
            CC_I2C_BUS_OFF(1);

        private int val;

        CC_I2C_BUS_STATUS(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: TvMiscSetI2CBusStatus
     * @Description: Enable or disable i2c bus
     * @Param: tmp_val 1 enable or 0 disable
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetI2CBusStatus(int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_SET_I2C_BUS_STATUS);
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: TvMiscGetI2CBusStatus
     * @Description: Get i2c bus status
     * @Param:
     * @Return: value 1 enable or 0 disable
     */
    public int TvMiscGetI2CBusStatus() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_GET_I2C_BUS_STATUS);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    // tv version info
    public class android_ver_info
    {
        public String build_release_ver;
        public String build_number_ver;
    }

    public class uboot_ver_info
    {
        public String ver_info;
    }

    public static class kernel_ver_info
    {
        public String linux_ver_info;
        public String build_usr_info;
        public String build_time_info;
    }

    public class tvapi_ver_info
    {
        public String git_branch_info;
        public String git_commit_info;
        public String last_change_time_info;
        public String build_time_info;
        public String build_usr_info;
    }

    public class dvb_ver_info
    {
        public String git_branch_info;
        public String git_commit_info;
        public String last_change_time_info;
        public String build_time_info;
        public String build_usr_info;
    }

    public class version_info
    {
        public android_ver_info android_ver;
        public uboot_ver_info uboot_ver;
        public kernel_ver_info kernel_ver;
        public tvapi_ver_info tvapi_ver;
        public dvb_ver_info dvb_ver;
    }

    public class project_info
    {
        public String version;
        public String panel_type;
        public String panel_outputmode;
        public String panel_rev;
        public String panel_name;
        public String amp_curve_name;
    }

    /**
     * @Function: TvMiscGetAndroidVersion
     * @Description: Get android version
     * @Param: none
     * @Return: android_ver_info
     */
    public android_ver_info TvMiscGetAndroidVersion() {
        libtv_log_open();
        android_ver_info tmpInfo = new android_ver_info();

        tmpInfo.build_release_ver = Build.VERSION.RELEASE;
        tmpInfo.build_number_ver = Build.DISPLAY;

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetUbootVersion
     * @Description: Get uboot version
     * @Param: none
     * @Return: uboot_ver_info
     */
    public uboot_ver_info TvMiscGetUbootVersion() {
        libtv_log_open();
        String tmp_str;
        uboot_ver_info tmpInfo = new uboot_ver_info();

        tmp_str = TvMiscPropertyGet("ro.ubootenv.varible.prefix",
                "ubootenv.var");
        tmpInfo.ver_info = TvMiscPropertyGet(tmp_str + "." + "ubootversion",
                "VERSION_ERROR");

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetKernelVersion
     * @Description: Get kernel version
     * @Param: none
     * @Return: kernel_ver_info
     */
    public kernel_ver_info TvMiscGetKernelVersion() {
        libtv_log_open();
        kernel_ver_info tmpInfo = new kernel_ver_info();
        String info = "";
        InputStream inputStream = null;

        tmpInfo.linux_ver_info = "unkown";
        tmpInfo.build_usr_info = "unkown";
        tmpInfo.build_time_info = "unkown";

        try {
            inputStream = new FileInputStream("/proc/version");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Regex did not match on /proc/version: " + info);
            return tmpInfo;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream), 8 * 1024);
        try {
            info = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return tmpInfo;
        }
        finally {
            try {
                reader.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return tmpInfo;
            }
        }

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "([^\\s]+)\\s+" +         /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(info);

        if (!m.matches()) {
            Log.e(TAG, "Regex did not match on /proc/version: " + info);
            return tmpInfo;
        } else if (m.groupCount() < 4) {
            Log.e(TAG,
                    "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return tmpInfo;
        }

        tmpInfo.linux_ver_info = m.group(1);
        tmpInfo.build_usr_info = m.group(2) + " " + m.group(3);
        tmpInfo.build_time_info = m.group(4);

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetTVAPIVersion
     * @Description: Get TV API version
     * @Param: none
     * @Return: tvapi_ver_info
     */
    public tvapi_ver_info TvMiscGetTVAPIVersion() {
        libtv_log_open();
        tvapi_ver_info tmpInfo = new tvapi_ver_info();

        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_GET_TV_API_VERSION);
        sendCmdToTv(cmd, r);

        tmpInfo.git_branch_info = r.readString();
        tmpInfo.git_commit_info = r.readString();
        tmpInfo.last_change_time_info = r.readString();
        tmpInfo.build_time_info = r.readString();
        tmpInfo.build_usr_info = r.readString();

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetDVBAPIVersion
     * @Description: Get DVB API version
     * @Param: none
     * @Return: dvb_ver_info
     */
    public dvb_ver_info TvMiscGetDVBAPIVersion() {
        libtv_log_open();
        dvb_ver_info tmpInfo = new dvb_ver_info();

        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_GET_DVB_API_VERSION);
        sendCmdToTv(cmd, r);

        tmpInfo.git_branch_info = r.readString();
        tmpInfo.git_commit_info = r.readString();
        tmpInfo.last_change_time_info = r.readString();
        tmpInfo.build_time_info = r.readString();
        tmpInfo.build_usr_info = r.readString();

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetVersion
     * @Description: Get version
     * @Param: none
     * @Return: version_info
     */
    public version_info TvMiscGetVersion() {
        libtv_log_open();
        version_info tmpInfo = new version_info();

        tmpInfo.android_ver = TvMiscGetAndroidVersion();
        tmpInfo.uboot_ver = TvMiscGetUbootVersion();
        tmpInfo.kernel_ver = TvMiscGetKernelVersion();
        tmpInfo.tvapi_ver = TvMiscGetTVAPIVersion();
        tmpInfo.dvb_ver = TvMiscGetDVBAPIVersion();

        return tmpInfo;
    }

    /**
     * @Function: TvMiscGetProjectInfo
     * @Description: Get project info
     * @Param: none
     * @Return: project_info
     */
    public project_info TvMiscGetProjectInfo() {
        libtv_log_open();
        project_info tmpInfo = new project_info();

        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_GET_PROJECT_INFO);
        sendCmdToTv(cmd, r);

        tmpInfo.version = r.readString();
        tmpInfo.panel_type = r.readString();
        tmpInfo.panel_outputmode = r.readString();
        tmpInfo.panel_rev = r.readString();
        tmpInfo.panel_name = r.readString();
        tmpInfo.amp_curve_name = r.readString();

        return tmpInfo;
    }

    public enum CC_PLATFROM_TYPE {
        CC_PLATFROM_T868_NO_FBC(0),
            CC_PLATFROM_T866_HAS_FBC(1);

        private int val;

        CC_PLATFROM_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: TvMiscGetPlatformType
     * @Description: Get platform type
     * @Param: none
     * @Return: refer as CC_PLATFROM_TYPE
     */
    public int TvMiscGetPlatformType() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_GET_PLATFORM_TYPE);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public enum SerialDeviceID {
        SERIAL_A(0),
            SERIAL_B(1),
            SERIAL_C(2);

        private int val;

        SerialDeviceID(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: SetSerialSwitch
     * @Description: Set speical serial switch
     * @Param: dev_id, refer to enum SerialDeviceID
     *         tmp_val, 1 is enable speical serial, 0 is disable speical serial
     * @Return: 0 success, -1 fail
     */
    public int SetSerialSwitch(SerialDeviceID dev_id, int tmp_val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(MISC_SERIAL_SWITCH);
        cmd.writeInt(dev_id.toInt());
        cmd.writeInt(tmp_val);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SendSerialData
     * @Description: send serial data
     * @Param: dev_id, refer to enum SerialDeviceID
     *         data_len, the length will be send
     *         data_buf, the data will be send
     * @Return: 0 success, -1 fail
     */
    public int SendSerialData(SerialDeviceID dev_id, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        if (data_len > data_buf.length) {
            return -1;
        }

        cmd.writeInt(MISC_SERIAL_SEND_DATA);

        cmd.writeInt(dev_id.toInt());
        cmd.writeInt(data_len);
        for (i = 0; i < data_len; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: TvMiscDeleteDirFiles
     * @Description: Delete dir files
     * @Param: par_str dir path string, flag -f, -fr...
     * @Return: 0 success, -1 fail
     */
    public int TvMiscDeleteDirFiles(String path_str, int flag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(DELETE_DIR_FILES);
        cmd.writeString(path_str);
        cmd.writeInt(flag);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: TvMiscSetPowerLedIndicator
     * @Description: Set power led indicator, red or green.
     * @Param: onoff: 1 on, 0 off
     * @Return: 0 success, -1 fail
     */
    public int TvMiscSetPowerLedIndicator(int onoff) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_SET_POWER_LED_INDICATOR);
        cmd.writeInt(onoff);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: TvMiscMutePanel
     * @Description: Mute panel or unmute panel, power on or off panel and backlight.
     * @Param: par_str dir path string, flag -f, -fr...
     * @Return: 0 success, -1 fail
     */
    public int TvMiscMutePanel(int onoff) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(MISC_SET_PANEL_MUTE);
        cmd.writeInt(onoff);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }
    //MISC END



    public int DtvAutoScan() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_SCAN_AUTO);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public int DtvAutoScanAtsc(int attenna, atv_video_std_e videoStd,atv_audio_std_e audioStd) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_SCAN_AUTO_ATSC);
        cmd.writeInt(attenna);
        cmd.writeInt(videoStd.toInt());
        cmd.writeInt(audioStd.toInt());
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public int DtvManualScan(int beginFreq, int endFreq, int modulation) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_SCAN_MANUAL_BETWEEN_FREQ);
        cmd.writeInt(beginFreq);
        cmd.writeInt(endFreq);
        cmd.writeInt(modulation);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public int DtvManualScan(int freq) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_SCAN_MANUAL);
        cmd.writeInt(freq);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public int AtvAutoScan(atv_video_std_e videoStd, atv_audio_std_e audioStd) {
        return AtvAutoScan(videoStd, audioStd, 0);
    }

    public int AtvAutoScan(atv_video_std_e videoStd, atv_audio_std_e audioStd, int storeType) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(ATV_SCAN_AUTO);
        cmd.writeInt(videoStd.toInt());
        cmd.writeInt(audioStd.toInt());
        cmd.writeInt(storeType);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: AtvManualScan
     * @Description: atv manual scan
     * @Param: currentNum:current Channel Number
     * @Param: starFreq:start frequency
     * @Param: endFreq:end frequency
     * @Param: videoStd:scan video standard
     * @Param: audioStd:scan audio standard
     * @Return: 0 ok or -1 error
     */
    public int AtvManualScan(int startFreq, int endFreq, atv_video_std_e videoStd,
            atv_audio_std_e audioStd, int storeType, int currentNum) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(ATV_SCAN_MANUAL_BY_NUMBER);
        cmd.writeInt(startFreq);
        cmd.writeInt(endFreq);
        cmd.writeInt(videoStd.toInt());
        cmd.writeInt(audioStd.toInt());
        cmd.writeInt(storeType);
        cmd.writeInt(currentNum);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: AtvManualScan
     * @Description: atv manual scan
     * @Param: starFreq:start frequency
     * @Param: endFreq:end frequency
     * @Param: videoStd:scan video standard
     * @Param: audioStd:scan audio standard
     * @Return: 0 ok or -1 error
     */
    public int AtvManualScan(int startFreq, int endFreq, atv_video_std_e videoStd,
            atv_audio_std_e audioStd) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(ATV_SCAN_MANUAL);
        cmd.writeInt(startFreq);
        cmd.writeInt(endFreq);
        cmd.writeInt(videoStd.toInt());
        cmd.writeInt(audioStd.toInt());
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: clearAllProgram
     * @Description: clearAllProgram
     * @Param: arg0, not used currently
     * @Return: 0 ok or -1 error
     */
    public int clearAllProgram(int arg0){
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(TV_CLEAR_ALL_PROGRAM);
        cmd.writeInt(arg0);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }
    //enable: 0  is disable , 1  is enable.      when enable it , can black video for switching program
    public int setBlackoutEnable(int enable){
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(SET_BLACKOUT_ENABLE);
        cmd.writeInt(enable);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public void startAutoBacklight() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(START_AUTO_BACKLIGHT);
        sendCmdToTv(cmd, r);
    }

    public void stopAutoBacklight() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(STOP_AUTO_BACKLIGHT);
        sendCmdToTv(cmd, r);
    }

    /**
     * @return 1:on,0:off
     */
    public int isAutoBackLighting() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(IS_AUTO_BACKLIGHTING);
        sendCmdToTv(cmd, r);
        return r.readInt();
    }

    public int getAverageLut() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AVERAGE_LUMA);
        sendCmdToTv(cmd, r);
        return r.readInt();
    }

    public int getAutoBacklightData(int data[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_AUTO_BACKLIGHT_DATA);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();
        for (i = 0; i < tmp_buf_size; i++) {
        data[i] = r.readInt();
        }

        return tmp_buf_size;
    }

    public int setAutoBacklightData(HashMap<String,Integer> map) {
        libtv_log_open();
        int ret =0;
        String data =null;
        data ="opcSwitch:"+map.get("opcSwitch")+",MinBacklight:"+map.get("MinBacklight")+",Offset:"+map.get("Offset")+
                ",MaxStep:"+map.get("MaxStep")+",MinStep:"+map.get("MinStep");
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_AUTO_BACKLIGHT_DATA);
        cmd.writeString(data);
        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return 0;
    }

    //ref to setBlackoutEnable fun
    public int SSMReadBlackoutEnalbe() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_READ_BLACKOUT_ENABLE);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMEEPROMWriteOneByte_N310_N311
     * @Description: Write one byte to eerpom
     * @Param: offset pos in eeprom for this byte, val one byte value
     * @Return: 0 success, -1 fail
     */
    public int SSMEEPROMWriteOneByte_N310_N311(int offset, int val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_EEPROM_SAVE_ONE_BYTE_N310_N311);
        cmd.writeInt(offset);
        cmd.writeInt(val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMEEPROMReadOneByte_N310_N311
     * @Description: Read one byte from eeprom
     * @Param: offset pos in eeprom for this byte to read
     * @Return: one byte read value
     */
    public int SSMEEPROMReadOneByte_N310_N311(int offset) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_EEPROM_READ_ONE_BYTE_N310_N311);
        cmd.writeInt(offset);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMEEPROMWriteNBytes
     * @Description: Write n bytes to eeprom
     * @Param: offset pos in eeprom for the bytes, data_len how many bytes, data_buf n bytes write buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMEEPROMWriteNBytes_N310_N311(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_EEPROM_SAVE_N_BYTES_N310_N311);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);
        for (i = 0; i < data_len; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMEEPROMReadNBytes_N310_N311
     * @Description: Read one byte from eeprom
     * @Param: offset pos in eeprom for the bytes, data_len how many bytes, data_buf n bytes read buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMEEPROMReadNBytes_N310_N311(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_data_len = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_EEPROM_READ_N_BYTES_N310_N311);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);

        sendCmdToTv(cmd, r);

        data_len = r.readInt();
        for (i = 0; i < data_len; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMFlashWriteOneByte
     * @Description: Write one byte to flash
     * @Param: offset pos in flash for this byte, val one byte value
     * @Return: 0 success, -1 fail
     */
    public int SSMFlashWriteOneByte_N310_N311(int offset, int val) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_FLASH_SAVE_ONE_BYTE_N310_N311);
        cmd.writeInt(offset);
        cmd.writeInt(val);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMFlashReadOneByte
     * @Description: Read one byte from flash
     * @Param: offset pos in flash for this byte to read
     * @Return: one byte read value
     */
    public int SSMFlashReadOneByte_N310_N311(int offset) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(SSM_FLASH_READ_ONE_BYTE_N310_N311);
        cmd.writeInt(offset);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: SSMFlashWriteNBytes
     * @Description: Write n bytes to flash
     * @Param: offset pos in flash for the bytes, data_len how many bytes, data_buf n bytes write buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMFlashWriteNBytes_N310_N311(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_FLASH_SAVE_N_BYTES_N310_N311);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);
        for (i = 0; i < data_len; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SSMFlashReadNBytes
     * @Description: Read one byte from flash
     * @Param: offset pos in flash for the bytes, data_len how many bytes, data_buf n bytes read buffer
     * @Return: 0 success, -1 fail
     */
    public int SSMFlashReadNBytes_N310_N311(int offset, int data_len, int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_data_len = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(SSM_FLASH_READ_N_BYTES_N310_N311);
        cmd.writeInt(offset);
        cmd.writeInt(data_len);

        sendCmdToTv(cmd, r);

        data_len = r.readInt();
        for (i = 0; i < data_len; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: ATVGetChanInfo
     * @Description: Get atv current channel info
     * @Param: dbID,program's in the srv_table of DB
     * @out: dataBuf[0]:freq
     * @out: dataBuf[1]  finefreq
     * @out: dataBuf[2]:video standard
     * @out: dataBuf[3]:audeo standard
     * @out: dataBuf[4]:is auto color std? 1, is auto,   0  is not auto
     * @Return: 0 ok or -1 error
     */
    public int ATVGetChanInfo(int dbID, int dataBuf[]) {
        libtv_log_open();
        int tmpRet = -1,tmp_buf_size = 0, i = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(ATV_GET_CHANNEL_INFO);
        cmd.writeInt(dbID);
        sendCmdToTv(cmd, r);

        dataBuf[0] = r.readInt();
        dataBuf[1] = r.readInt();
        dataBuf[2] = r.readInt();
        dataBuf[3] = r.readInt();
        dataBuf[4] = r.readInt();

        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: ATVGetVideoCenterFreq
     * @Description: Get atv current channel video center freq
     * @Param: dbID,program's in the srv_table of DB
     * @Return: 0 ok or -1 error
     */
    public int ATVGetVideoCenterFreq(int dbID)
    {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(ATV_GET_VIDEO_CENTER_FREQ);
        cmd.writeInt(dbID);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: ATVGetLastProgramID
     * @Description: ATV Get Last Program's ID
     * @Return: ATV Last Program's ID
     */
    public int ATVGetLastProgramID()
    {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(ATV_GET_CURRENT_PROGRAM_ID);
        sendCmdToTv(cmd, r);
        int atvLastId = r.readInt();
        return atvLastId;
    }

    /**
     * @Function: DTVGetLastProgramID
     * @Description: DTV Get Last Program's ID
     * @Return: DTV Last Program's ID
     */
    public int DTVGetLastProgramID()
    {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_CURRENT_PROGRAM_ID);
        sendCmdToTv(cmd, r);
        int dtvLastId = r.readInt();
        return dtvLastId;
    }

    /**
     * @Function: ATVGetMinMaxFreq
     * @Description: ATV Get Min Max Freq
     * @Param:dataBuf[0]:min freq
     * @Param:dataBuf[1]:max freq
     * @Return: 0 or -1
     */
    public int ATVGetMinMaxFreq(int dataBuf[]) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(ATV_GET_MIN_MAX_FREQ);
        sendCmdToTv(cmd, r);
        dataBuf[0] = r.readInt();
        dataBuf[1] = r.readInt();
        int tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: DTVGetScanFreqList
     * @Description: DTVGetScanFreqList
     * @Param:
     * @Return: FreqList
     */
    public ArrayList<FreqList> DTVGetScanFreqList() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_SCAN_FREQUENCY_LIST);
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        int base = 1 ;
        ArrayList<FreqList> FList = new ArrayList<FreqList>();
        FreqList bpl = new FreqList();
        base = r.readInt() - 1;
        bpl.ID = 1 ;
        bpl.freq= r.readInt();
        FList.add(bpl);
        for (int i = 1; i < size; i++) {
            FreqList pl = new FreqList();
            pl.ID = r.readInt() - base;
            pl.freq= r.readInt();
            FList.add(pl);
        }
        return FList;

    }

    /**
     * @Function: DTVGetChanInfo
     * @Description: Get dtv current channel info
     * @Param: dbID:program's in the srv_table of DB
     * @Param: dataBuf[0]:freq
     * @Param: dataBuf[1]:strength
     * @Param: dataBuf[2]:snr
     * @Param: dataBuf[2]:ber
     * @Return: 0 ok or -1 error
     */
    public int DTVGetChanInfo(int dbID, int dataBuf[]) {
        libtv_log_open();
        int tmpRet = -1,tmp_buf_size = 0, i = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(DTV_GET_CHANNEL_INFO);
        cmd.writeInt(dbID);
        sendCmdToTv(cmd, r);

        dataBuf[0] = r.readInt();
        dataBuf[1] = r.readInt();
        dataBuf[2] = r.readInt();
        dataBuf[3] = r.readInt();

        tmpRet = r.readInt();
        return tmpRet;
    }
    public int TvSubtitleDrawUnlock() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(TV_SUBTITLE_DRAW_END);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }
    //public class Subtitle
    //{
    private Bitmap mSubtitleBMP = null;
    public void CreateSubtitleBitmap() {
        libtv_log_open();
        mSubtitleBMP = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
        native_create_subtitle_bitmap(mSubtitleBMP);
    }
    private native final void native_create_subtitle_bitmap(Object bmp);

    public Bitmap getSubtitleBitmap() {
        libtv_log_open();
        return mSubtitleBMP;
    }
    public void setSubtitleUpdateListener(SubtitleUpdateListener l) {
        libtv_log_open();
        mSubtitleListener = l;
    }
    //scanner
    public void setScannerListener(ScannerEventListener l) {
        libtv_log_open();
        mScannerListener = l;
    }
    //public onUpdate();
    //}

    private Bitmap mVideoFrameBMP = null;
    public void CreateVideoFrameBitmap(int inputSourceMode) {
        libtv_log_open();
        mVideoFrameBMP = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        native_create_video_frame_bitmap(mVideoFrameBMP);
    }

    private native final void native_create_video_frame_bitmap(Object bmp);

    public Bitmap getVideoFrameBitmap() {
        libtv_log_open();
        return mVideoFrameBMP;
    }

    public final static int EVENT_SCAN_PROGRESS = 0;
    public final static int EVENT_STORE_BEGIN   = 1;
    public final static int EVENT_STORE_END     = 2;
    public final static int EVENT_SCAN_END     = 3;
    public final static int EVENT_BLINDSCAN_PROGRESS = 4;
    public final static int EVENT_BLINDSCAN_NEWCHANNEL = 5;
    public final static int EVENT_BLINDSCAN_END    = 6;
    public final static int EVENT_ATV_PROG_DATA = 7;
    public final static int EVENT_DTV_PROG_DATA = 8;
    public final static int EVENT_SCAN_EXIT     = 9;

    public class ScannerEvent
    {
        public int type;
        public int precent;
        public int totalcount;
        public int lock;
        public int cnum;
        public int freq;
        public String programName;
        public int srvType;
        public String msg;
        public int strength;
        public int quality;

        //for ATV
        public int  videoStd;
        public int audioStd;
        public int isAutoStd;
        public int fineTune;

        //for DTV
        public int mode;
        public int sr;
        public int mod;
        public int bandwidth;
        public int ofdm_mode;
        public int ts_id;
        public int orig_net_id;

        public int serviceID;
        public int vid;
        public int vfmt;
        public int[] aids;
        public int[] afmts;
        public String[] alangs;
        public int[] atypes;
        public int pcr;

        public int[] stypes;
        public int[] sids;
        public int[] sstypes;
        public int[] sid1s;
        public int[] sid2s;
        public String[] slangs;

    }
    public interface ScannerEventListener {
        void onEvent(ScannerEvent ev);
    }

    //epg
    public void setEpgListener(EpgEventListener l) {
        libtv_log_open();
        mEpgListener = l;
    }

    public class EpgEvent
    {
        public int type;
        public int channelID;
        public int programID;
        public int dvbOrigNetID;
        public int dvbTSID;
        public int dvbServiceID;
        public long time;
        public int dvbVersion;
    }
    public interface EpgEventListener {
        void onEvent(EpgEvent ev);
    }

    public class VFrameEvent{
        public int FrameNum;
        public int FrameSize;
        public int FrameWidth;
        public int FrameHeight;
    }

    public interface VframBMPEventListener{
        void onEvent(VFrameEvent ev);
    }

    public void setGetVframBMPListener(VframBMPEventListener l) {
        libtv_log_open();
        mVframBMPListener = l;
    }

    public interface SubtitleUpdateListener {
        void onUpdate();
    }

    public int DtvStopScan() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_STOP_SCAN);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public int DtvGetSignalSNR() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_GET_SNR);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public int DtvGetSignalBER() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_GET_BER);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public int DtvGetSignalStrength() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_GET_STRENGTH);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    /**
     * @Function: DtvGetAudioTrackNum
     * @Description: Get number audio track of program
     * @Param: [in] prog_id is in db srv table
     * @Return: number audio track
     */
    public int DtvGetAudioTrackNum(int prog_id) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(DTV_GET_AUDIO_TRACK_NUM);
        cmd.writeInt(prog_id);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    /**
     * @Function: DtvGetCurrAudioTrackIndex
     * @Description: Get number audio track of program
     * @Param: [in] prog_id is in db srv table
     * @Return: current audio track index
     */

    public int DtvGetCurrAudioTrackIndex(int prog_id) {
        libtv_log_open();
        int currAudioTrackIdx;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(DTV_GET_CURR_AUDIO_TRACK_INDEX);
        cmd.writeInt(prog_id);
        sendCmdToTv(cmd, r);
        currAudioTrackIdx = r.readInt();

        return currAudioTrackIdx;
    }


    public class DtvAudioTrackInfo
    {
        public String language;
        public int audio_fmt;
        public int aPid;
    }
    public DtvAudioTrackInfo DtvGetAudioTrackInfo(int prog_id, int audio_ind) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_AUDIO_TRACK_INFO);
        cmd.writeInt(prog_id);
        cmd.writeInt(audio_ind);
        sendCmdToTv(cmd, r);

        DtvAudioTrackInfo tmpRet = new DtvAudioTrackInfo();
        tmpRet.audio_fmt = r.readInt();
        tmpRet.language = r.readString();

        return tmpRet;
    }

    /**
     * @Function: DtvSetAudioChannleMod
     * @Description: set audio channel mod
     * @Param: [in] audioChannelMod is [0 Stereo] [1 left] [2 right ] [3 swap left right]
     * @Return:
     */
    public int DtvSetAudioChannleMod(int audioChannelMod) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_AUDIO_CHANNEL_MOD);
        cmd.writeInt(audioChannelMod);
        sendCmdToTv(cmd, r);
        return 0;
    }

    /**
     * @Function: DtvGetAudioChannleMod
     * @Description: set audio channel mod
     * @Param:
     * @Return: [OUT] audioChannelMod is [0 Stereo] [1 left] [2 right ] [3 swap left right]
     */
    public int DtvGetAudioChannleMod() {
        libtv_log_open();
        int channelMod;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_AUDIO_CHANNEL_MOD);
        sendCmdToTv(cmd, r);
        channelMod = r.readInt();
        return channelMod;
    }


    public int DtvGetFreqByProgId(int progId) {
        libtv_log_open();
        int freq = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_FREQ_BY_PROG_ID);
        cmd.writeInt(progId);
        sendCmdToTv(cmd, r);
        freq = r.readInt();
        return freq;
    }

    public class EpgInfoEvent
    {
        public String programName;
        public String programDescription;
        public String programExtDescription;
        public long startTime;
        public long endTime;
        public int subFlag;
        public int evtId;
    }

    public EpgInfoEvent DtvEpgInfoPointInTime(int progId, long iUtcTime) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        EpgInfoEvent epgInfoEvent = new EpgInfoEvent();

        cmd.writeInt(DTV_GET_EPG_INFO_POINT_IN_TIME);
        cmd.writeInt(progId);
        cmd.writeInt((int)iUtcTime);
        sendCmdToTv(cmd, r);
        epgInfoEvent.programName = r.readString();
        epgInfoEvent.programDescription = r.readString();
        epgInfoEvent.programExtDescription = r.readString();
        epgInfoEvent.startTime = r.readInt();
        epgInfoEvent.endTime = r.readInt();
        epgInfoEvent.subFlag = r.readInt();
        epgInfoEvent.evtId =  r.readInt();
        return epgInfoEvent;
    }

    public ArrayList<EpgInfoEvent> GetEpgInfoEventDuration(int progId,long iStartTime,long iDuration) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_EPG_INFO_DURATION);
        cmd.writeInt(progId);
        cmd.writeInt((int)iStartTime);
        cmd.writeInt((int)iDuration);
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        ArrayList<EpgInfoEvent> pEpgInfoList = new ArrayList<EpgInfoEvent>();
        for (int i = 0; i < size; i++) {
            EpgInfoEvent pl = new EpgInfoEvent();
            pl.programName = r.readString();
            pl.programDescription = r.readString();
            pl.programExtDescription = r.readString();
            pl.startTime = r.readInt();
            pl.endTime = r.readInt();
            pl.subFlag = r.readInt();
            pl.evtId =  r.readInt();
            pEpgInfoList.add(pl);
        }
        return pEpgInfoList;
    }

    public int DtvSwitchAudioTrack(int audio_pid, int audio_format, int audio_param) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(DTV_SWITCH_AUDIO_TRACK);
        cmd.writeInt(audio_pid);
        cmd.writeInt(audio_format);
        cmd.writeInt(audio_param);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public int DtvSwitchAudioTrack(int prog_id, int audio_track_id) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(DTV_SWITCH_AUDIO_TRACK);
        cmd.writeInt(prog_id);
        cmd.writeInt(audio_track_id);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public long DtvGetEpgUtcTime() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        long utcTimeMs = 0;

        cmd.writeInt(DTV_GET_EPG_UTC_TIME);
        sendCmdToTv(cmd, r);
        utcTimeMs = r.readInt();

        return utcTimeMs;
    }

    public class VideoFormatInfo
    {
        public int width;
        public int height;
        public int fps;
        public int interlace;
    }

    public VideoFormatInfo DtvGetVideoFormatInfo() {
        libtv_log_open();
        VideoFormatInfo pVideoFormatInfo = new VideoFormatInfo();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(DTV_GET_VIDEO_FMT_INFO);
        sendCmdToTv(cmd, r);
        pVideoFormatInfo.width = r.readInt();
        pVideoFormatInfo.height= r.readInt();
        pVideoFormatInfo.fps= r.readInt();
        pVideoFormatInfo.interlace= r.readInt();
        pVideoFormatInfo.width = r.readInt();
        return pVideoFormatInfo;
    }


    public class BookEventInfo
    {
        public String programName;
        public String envName;
        public long startTime;
        public long durationTime;
        public int bookId;
        public int progId;
        public int evtId;
    }


    public ArrayList<BookEventInfo> getBookedEvent() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_BOOKED_EVENT);

        int size = r.readInt();
        ArrayList<BookEventInfo> pBookEventInfoList = new ArrayList<BookEventInfo>();
        for (int i = 0; i < size; i++) {
            BookEventInfo pl = new BookEventInfo();
            pl.programName = r.readString();
            pl.envName = r.readString();
            pl.startTime = r.readInt();
            pl.durationTime = r.readInt();
            pl.bookId = r.readInt();
            pl.progId = r.readInt();
            pl.evtId = r.readInt();
            pBookEventInfoList.add(pl);
        }
        return pBookEventInfoList;
    }

    public int setEventBookFlag(int id, int bookFlag) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_BOOKING_FLAG);
        cmd.writeInt(id);
        cmd.writeInt(bookFlag);
        sendCmdToTv(cmd, r);
        return 0;
    }

    public int setProgramName(int id, String name) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_PROGRAM_NAME);
        cmd.writeInt(id);
        cmd.writeString(name);
        sendCmdToTv(cmd, r);
        return 0;
    }

    public int setProgramSkipped(int id, int skipped) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_PROGRAM_SKIPPED);
        cmd.writeInt(id);
        cmd.writeInt(skipped);
        sendCmdToTv(cmd, r);
        return 0;
    }

    public int setProgramFavorite(int id, int favorite) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_PROGRAM_FAVORITE);
        cmd.writeInt(id);
        cmd.writeInt(favorite);
        sendCmdToTv(cmd, r);
        return 0;
    }

    public int deleteProgram(int id) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_DETELE_PROGRAM);
        cmd.writeInt(id);
        sendCmdToTv(cmd, r);
        return 0;
    }
    public int swapProgram(int first_id, int second_id) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SWAP_PROGRAM);
        cmd.writeInt(first_id);
        cmd.writeInt(second_id);
        sendCmdToTv(cmd, r);
        return 0;
    }

    public int setProgramLocked (int id, int locked) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_PROGRAM_LOCKED);
        cmd.writeInt(id);
        cmd.writeInt(locked);
        sendCmdToTv(cmd, r);
        return 0;
    }

    /*public int PlayProgram(int progid) {
      libtv_log_open();
      Parcel cmd = Parcel.obtain();
      Parcel r = Parcel.obtain();
      int tmpRet ;
      cmd.writeInt(PLAY_PROGRAM);
      cmd.writeInt(progid);
      sendCmdToTv(cmd, r);
      tmpRet = r.readInt();

      return tmpRet;
      }*/

    public int PlayATVProgram(int freq, int videoStd, int audioStd, int fineTune, int audioCompetation) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(PLAY_PROGRAM);
        cmd.writeInt(4);
        cmd.writeInt(freq);
        cmd.writeInt(videoStd);
        cmd.writeInt(audioStd);
        cmd.writeInt(fineTune);
        cmd.writeInt(audioCompetation);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public int PlayDTVProgram(int mode, int freq, int para1, int para2, int vid, int vfmt, int aid, int afmt, int pcr, int audioCompetation) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(PLAY_PROGRAM);
        cmd.writeInt(mode);
        cmd.writeInt(freq);
        cmd.writeInt(para1);
        cmd.writeInt(para2);
        cmd.writeInt(vid);
        cmd.writeInt(vfmt);
        cmd.writeInt(aid);
        cmd.writeInt(afmt);
        cmd.writeInt(pcr);
        cmd.writeInt(audioCompetation);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public int StopPlayProgram() {
        libtv_log_open();
        int tmpRet = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(STOP_PROGRAM_PLAY);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public enum tv_fe_type_e {
        TV_FE_QPSK(0),
            TV_FE_QAM(1),
            TV_FE_OFDM(2),
            TV_FE_ATSC(3),
            TV_FE_ANALOG(4),
            TV_FE_DTMB(5);

        private int val;

        tv_fe_type_e(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function:SetFrontendParms
     * @Description:set frontend parameters
     * @Param:dataBuf[0]:feType, tv_fe_type_e
     * @Param:dataBuf[1]:freq, set freq to tuner
     * @Param:dataBuf[2]:videoStd, video std
     * @Param:dataBuf[3]:audioStd, audio std
     * @Param:dataBuf[4]:parm1
     * @Param:dataBuf[5]:parm2
     * @Return: 0 ok or -1 error
     */
    public int SetFrontendParms(tv_fe_type_e feType, int freq, int vStd, int aStd, int p1, int p2) {
        libtv_log_open();
        int tmpRet  = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_FRONTEND_PARA);
        cmd.writeInt(feType.toInt());
        cmd.writeInt(freq);
        cmd.writeInt(vStd);
        cmd.writeInt(aStd);
        cmd.writeInt(p1);
        cmd.writeInt(p2);

        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }


    public enum CC_PARAM_COUNTRY {
        CC_PARAM_COUNTRY_USA(0),
            CC_PARAM_COUNTRY_KOREA(1);

        private int val;

        CC_PARAM_COUNTRY(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_PARAM_SOURCE_TYPE {
        CC_PARAM_SOURCE_VBIDATA(0),
            CC_PARAM_SOURCE_USERDATA(1);

        private int val;

        CC_PARAM_SOURCE_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum CC_PARAM_CAPTION_TYPE {
        CC_PARAM_ANALOG_CAPTION_TYPE_CC1(0),
            CC_PARAM_ANALOG_CAPTION_TYPE_CC2(1),
            CC_PARAM_ANALOG_CAPTION_TYPE_CC3(2),
            CC_PARAM_ANALOG_CAPTION_TYPE_CC4(3),
            CC_PARAM_ANALOG_CAPTION_TYPE_TEXT1(4),
            CC_PARAM_ANALOG_CAPTION_TYPE_TEXT2(5),
            CC_PARAM_ANALOG_CAPTION_TYPE_TEXT3(6),
            CC_PARAM_ANALOG_CAPTION_TYPE_TEXT4(7),
            //
            CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE1(8),
            CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE2(9),
            CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE3(10),
            CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE4(11),
            CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE5(12),
            CC_PARAM_DIGITAL_CAPTION_TYPE_SERVICE6(13);

        private int val;

        CC_PARAM_CAPTION_TYPE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }


    /*
     * 1, Set the country first and parameters should be either USA or KOREA
#define CMD_SET_COUNTRY_USA                 0x5001
#define CMD_SET_COUNTRY_KOREA            0x5002

2, Set the source type which including
a)VBI data(for analog program only)
b)USER data(for AIR or Cable service)
CMD_CC_SET_VBIDATA   = 0x7001,
CMD_CC_SET_USERDATA = 0x7002,
2.1 If the frontend type is Analog we must set the channel Index
with command 'CMD_CC_SET_CHAN_NUM' and the parameter is like 57M
we set 0x20000, this should according to USA standard frequency
table.

3, Next is to set the CC service type

#define CMD_CC_1                        0x3001
#define CMD_CC_2                        0x3002
#define CMD_CC_3                        0x3003
#define CMD_CC_4                        0x3004

    //this doesn't support currently
#define CMD_TT_1                        0x3005
#define CMD_TT_2                        0x3006
#define CMD_TT_3                        0x3007
#define CMD_TT_4                        0x3008

#define CMD_SERVICE_1                 0x4001
#define CMD_SERVICE_2                 0x4002
#define CMD_SERVICE_3                 0x4003
#define CMD_SERVICE_4                 0x4004
#define CMD_SERVICE_5                 0x4005
#define CMD_SERVICE_6                 0x4006

4, Then set CMD_CC_START to start the CC service, and you needn't to stop

CC service while switching services

5, CMD_CC_STOP should be called in some cases like switch source, change

program, no signal, blocked...*/

    //channel_num == 0 ,if frontend is dtv
    //else != 0
    public int StartCC(CC_PARAM_COUNTRY country, CC_PARAM_SOURCE_TYPE src_type, int channel_num, CC_PARAM_CAPTION_TYPE caption_type) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_START_CC);
        cmd.writeInt(country.toInt());
        cmd.writeInt(src_type.toInt());
        cmd.writeInt(channel_num);
        cmd.writeInt(caption_type.toInt());
        sendCmdToTv(cmd, r);
        return r.readInt();
    }

    public int StopCC() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_STOP_CC);
        sendCmdToTv(cmd, r);
        return r.readInt();
    }
    public String Test1(int progid) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_TEST_1);
        cmd.writeInt(progid);
        sendCmdToTv(cmd, r);
        return r.readString();
    }

    public String Test2(int c) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(DTV_TEST_2);
        cmd.writeInt(c);
        sendCmdToTv(cmd, r);
        return r.readString();
    }

    public int tvAutoScan() {
        libtv_log_open();
        int tmpRet = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(TV_AUTO_SCAN);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    /* public int tvSetScanSource(scan_source_t source) {
       Parcel cmd = Parcel.obtain();
       Parcel r = Parcel.obtain();
       int tmpRet ;
       cmd.writeInt(TV_SET_SCAN_SOURCE);
       cmd.writeInt(source.toInt());
       sendCmdToTv(cmd, r);
       tmpRet = r.readInt();

       return tmpRet;
       }*/



    public int tvSetAttennaType(atsc_attenna_type_t source) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(TV_SET_ATSC_ATTENNA_TYPE);
        cmd.writeInt(source.toInt());
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();

        return tmpRet;
    }

    public atsc_attenna_type_t tvGetAttennaType( ) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(TV_GET_ATSC_ATTENNA_TYPE);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        for (atsc_attenna_type_t t:atsc_attenna_type_t.values()) {
            if (t.toInt() == tmpRet)
                return t;
        }
        return null;
    }

    public int HistogramGet_AVE() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_HISTGRAM_AVE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int GetHistGram(int hist_gram_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();

        cmd.writeInt(GET_HISTGRAM);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();
        for (i = 0; i < tmp_buf_size; i++) {
            hist_gram_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    // atsc vchip
    public enum BLOCK_TYPE {
        BLOCK_BY_LOCK,
            BLOCK_BY_PARENTAL_CONTROL,
            BLOCK_BY_VCHIP;
    }
    public enum BLOCK_STATUS {
        TYPE_PROGRAM_UNBLOCK,
            TYPE_PROGRAM_BLOCK;
    }

    public int atscSetVchipLockstatus(String dimensioName,int ratingRegionId,int arr_length,int[] arr) {
        libtv_log_open();
        int tmpRet  = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_VCHIP_LOCKSTATUS);
        cmd.writeString(dimensioName);
        cmd.writeInt(ratingRegionId);
        cmd.writeInt(arr_length);
        for (int i=0; i<arr_length; i++) {
            cmd.writeInt(arr[i]);
        }
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }



    public int[] atscGetVchipLockstatus(String dimensioName,int ratingRegionId) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_VCHIP_LOCKSTATUS);
        cmd.writeString(dimensioName);
        cmd.writeInt(ratingRegionId);
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        int[] arr =new int[size];
        for (int i=0; i<size; i++) {
            arr[i]=r.readInt();
        }
        return arr;
    }

    public int atscSetVchipUbblock() {
        libtv_log_open();
        int tmpRet  = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_VCHIP_UNBLOCK);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public VchipLockStatus atscGetCurrentVchipBlock() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_GET_CURRENT_VCHIP_BLOCK);
        sendCmdToTv(cmd, r);
        VchipLockStatus lockStatus =new VchipLockStatus();
        lockStatus.blockstatus=r.readInt();
        lockStatus.vchipDimension =r.readString();
        lockStatus.vchipAbbrev =r.readString();
        return lockStatus;
    }

    private void libtv_log_open(){
        if (tvLogFlg) {
            StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
            Log.i(TAG,traceElement.getMethodName());
        }
    }

    public  int dtvSetVchipBlockOnOff(int onOff ,int regin_id) {
        libtv_log_open();
        int tmpRet  = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(DTV_SET_VCHIP_BLOCK_ON_OFF);
        cmd.writeInt(onOff);
        cmd.writeInt(regin_id);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    //Vchip Lock Status
    public class VchipLockStatus
    {
        public int blockType;
        public int blockstatus;
        public String vchipDimension;
        public String vchipText;
        public String vchipAbbrev;
    }

    public void setVchipLockStatusListener(VchipLockStatusListener l) {
        libtv_log_open();
        mLockStatusListener = l;
    }

    public interface VchipLockStatusListener {
        void onLock(VchipLockStatus lockStatus);
    }


    public enum SOUND_TRACK_MODE {
        SOUND_TRACK_MODE_MONO(0),
            SOUND_TRACK_MODE_STEREO(1),
            SOUND_TRACK_MODE_SAP(2);
        private int val;

        SOUND_TRACK_MODE(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }


    public int Tv_SetSoundTrackMode(SOUND_TRACK_MODE mode) {
        libtv_log_open();
        int tmpRet  = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(TV_SET_SOUND_TRACK_MODE);
        cmd.writeInt(mode.toInt());
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }
    public int Tv_GetSoundTrackMode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(TV_GET_SOUND_TRACK_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public enum LEFT_RIGHT_SOUND_CHANNEL {
        LEFT_RIGHT_SOUND_CHANNEL_STEREO(0),
            LEFT_RIGHT_SOUND_CHANNEL_LEFT(1),
            LEFT_RIGHT_SOUND_CHANNEL_RIGHT(2),
            LEFT_RIGHT_SOUND_CHANNEL_SWAP(3);
        private int val;

        LEFT_RIGHT_SOUND_CHANNEL(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public int setLeftRightSondChannel(LEFT_RIGHT_SOUND_CHANNEL mode)
    {
        libtv_log_open();
        int tmpRet  = -1;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_LEFT_RIGHT_SOUND_CHANNEL);
        cmd.writeInt(mode.toInt());
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    public int getLeftRightSondChannel() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(GET_LEFT_RIGHT_SOUND_CHANNEL);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public class ProgList
    {
        public String name;
        public int Id;
        public int chanOrderNum;
        public int major;
        public int minor;
        public int type;//service_type
        public int skipFlag;
        public int favoriteFlag;
        public int videoFmt;
        public int tsID;
        public int serviceID;
        public int pcrID;
        public int vPid;
        public ArrayList<DtvAudioTrackInfo> audioInfoList;
        public int chFreq;
    }

    public class FreqList
    {
        public int ID;
        public int freq;
    }

    /**
     * @Function:GetProgramList
     * @Description,get program list
     * @Param:serType,get diff program list by diff service type
     * @Param:skip,default 0(it shows no skip)
     * @Return:ProgList
     */
    public ArrayList<ProgList> GetProgramList(tv_program_type serType, program_skip_type_e skip) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(GET_PROGRAM_LIST);
        cmd.writeInt(serType.toInt());
        cmd.writeInt(skip.toInt());
        sendCmdToTv(cmd, r);
        int size = r.readInt();
        ArrayList<ProgList> pList = new ArrayList<ProgList>();
        for (int i = 0; i < size; i++) {
            ProgList pl = new ProgList();
            pl.Id = r.readInt();
            pl.chanOrderNum = r.readInt();
            pl.major = r.readInt();
            pl.minor = r.readInt();
            pl.type = r.readInt();
            pl.name = r.readString();
            pl.skipFlag = r.readInt();
            pl.favoriteFlag = r.readInt();
            pl.videoFmt = r.readInt();
            pl.tsID = r.readInt();
            pl.serviceID = r.readInt();
            pl.pcrID = r.readInt();
            pl.vPid = r.readInt();
            int trackSize = r.readInt();
            pl.audioInfoList = new ArrayList<DtvAudioTrackInfo>();
            for (int j = 0; j < trackSize; j++) {
                DtvAudioTrackInfo info = new DtvAudioTrackInfo();
                info.language =r.readString();
                info.audio_fmt =r.readInt();
                info.aPid = r.readInt();
                pl.audioInfoList.add(info);
            }
            pl.chFreq = r.readInt();
            pList.add(pl);
        }
        Log.i(TAG,"get prog list size = "+pList.size());
        return pList;
    }

    public enum vpp_display_resolution_t {
        VPP_DISPLAY_RESOLUTION_1366X768(0),
            VPP_DISPLAY_RESOLUTION_1920X1080(1),
            VPP_DISPLAY_RESOLUTION_3840X2160(2),
            VPP_DISPLAY_RESOLUTION_MAX(3);
        private int val;

        vpp_display_resolution_t(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function:GetDisplayResolutionConfig
     * @Description, get the display resolution config
     * @Param: none
     * @Return: value refer to enum vpp_display_resolution_t
     */
    public int GetDisplayResolutionConfig() {
        libtv_log_open();
        int temp = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(GET_DISPLAY_RESOLUTION_CONFIG);
        sendCmdToTv(cmd, r);
        temp = r.readInt();
        return temp;
    }

    /**
     * @Function:GetDisplayResolutionInfo
     * @Description, get the display resolution info
     * @Param: none
     * @Return: high 16 bits is width, low 16 bits is height
     */
    public int GetDisplayResolutionInfo() {
        libtv_log_open();
        int temp = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(GET_DISPLAY_RESOLUTION_INFO);
        sendCmdToTv(cmd, r);
        temp = r.readInt();
        return temp;
    }

    /**
     * @Function: SendHDMIRxCECCustomMessage
     * @Description: send hdmi rx cec custom message
     * @Param: data_buf, value buffer of message.
     * @Return: 0 success, -1 fail
     */
    public int SendHDMIRxCECCustomMessage(int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HDMIRX_CEC_SEND_CUSTOM_MESSAGE);

        tmp_buf_size = data_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(data_buf[i]);
        }

        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SendHDMIRxCECCustomMessageAndWaitReply
     * @Description: send hdmi rx cec custom message and wait reply message
     * @Param: data_buf, value buffer of message.
     *         reply_buf, value buffer of reply message.
     *         WaitCmd, wait command.
     *         timeout, wait timeout (ms).
     * @Return: >= 0 reply data size, -1 command send fail
     */
    public int SendHDMIRxCECCustomMessageAndWaitReply(int data_buf[], int reply_buf[], int WaitCmd, int timeout) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HDMIRX_CEC_SEND_CUSTOM_WAIT_REPLY_MESSAGE);

        tmp_buf_size = data_buf.length;
        cmd.writeInt(tmp_buf_size);
        for (i = 0; i < tmp_buf_size; i++) {
            cmd.writeInt(data_buf[i]);
        }

        cmd.writeInt(WaitCmd);
        cmd.writeInt(timeout);
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();
        for (i = 0; i < tmp_buf_size; i++) {
            reply_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function:SendHDMIRxCECBoradcastStandbyMessage
     * @Description, send hdmi rx cec broadcase standby message.
     * @Param: none
     * @Return: 0 success, -1 fail
     */
    public int SendHDMIRxCECBoradcastStandbyMessage() {
        libtv_log_open();
        int temp = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet ;
        cmd.writeInt(HDMIRX_CEC_SEND_BROADCAST_STANDBY_MESSAGE);
        sendCmdToTv(cmd, r);
        temp = r.readInt();
        return temp;
    }

    /**
     * @Function: SendHDMIRxCECGiveCECVersionMessage
     * @Description: send hdmi rx cec give cec version message
     * @Param: data_buf, value buffer of reply message.
     * @Return: >= 0 reply data size, -1 command send fail
     */
    public int SendHDMIRxCECGiveCECVersionMessage(SourceInput source_input, int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HDMIRX_CEC_SEND_GIVE_CEC_VERSION_MESSAGE);
        cmd.writeInt(source_input.toInt());
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();
        for (i = 0; i < tmp_buf_size; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SendHDMIRxCECGiveDeviceVendorIDMessage
     * @Description: send hdmi rx cec give device vendor id message
     * @Param: data_buf, value buffer of reply message.
     * @Return: >= 0 reply data size, -1 command send fail
     */
    public int SendHDMIRxCECGiveDeviceVendorIDMessage(SourceInput source_input, int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HDMIRX_CEC_SEND_GIVE_DEV_VENDOR_ID_MESSAGE);
        cmd.writeInt(source_input.toInt());
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();
        for (i = 0; i < tmp_buf_size; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    /**
     * @Function: SendHDMIRxCECGiveOSDNameMessage
     * @Description: send hdmi rx cec give osd name message
     * @Param: data_buf, value buffer of reply message.
     * @Return: >= 0 reply data size, -1 command send fail
     */
    public int SendHDMIRxCECGiveOSDNameMessage(SourceInput source_input, int data_buf[]) {
        libtv_log_open();
        int i = 0, tmp_buf_size = 0, ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HDMIRX_CEC_SEND_GIVE_OSD_NAME_MESSAGE);
        cmd.writeInt(source_input.toInt());
        sendCmdToTv(cmd, r);

        tmp_buf_size = r.readInt();
        for (i = 0; i < tmp_buf_size; i++) {
            data_buf[i] = r.readInt();
        }

        ret = r.readInt();
        return ret;
    }

    public int GetHdmiHdcpKeyKsvInfo(int data_buf[]) {
        int ret = 0;
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(HDMIRX_GET_KSV_INFO);
        sendCmdToTv(cmd, r);

        ret = r.readInt();
        data_buf[0] = r.readInt();
        data_buf[1] = r.readInt();
        return ret;
    }


    public enum FBCUpgradeState {
        STATE_STOPED(0),
            STATE_RUNNING(1),
            STATE_FINISHED(2),
            STATE_ABORT(3);

        private int val;

        FBCUpgradeState(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public enum FBCUpgradeErrorCode {
        ERR_SERIAL_CONNECT(-1),
            ERR_OPEN_BIN_FILE(-2),
            ERR_BIN_FILE_SIZE(-3);

        private int val;

        FBCUpgradeErrorCode(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    /**
     * @Function: StartUpgradeFBC
     * @Description: start upgrade fbc
     * @Param: file_name: upgrade bin file name
     *         mode: value refer to enum FBCUpgradeState
     * @Return: 0 success, -1 fail
     */
    public int StartUpgradeFBC(String file_name, int mode) {
        return StartUpgradeFBC(file_name, mode, 0x10000);
    }

    /**
     * @Function: StartUpgradeFBC
     * @Description: start upgrade fbc
     * @Param: file_name: upgrade bin file name
     *         mode: value refer to enum FBCUpgradeState
     *         upgrade_blk_size: upgrade block size (min is 4KB)
     * @Return: 0 success, -1 fail
     */
    public int StartUpgradeFBC(String file_name, int mode, int upgrade_blk_size) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int tmpRet;
        cmd.writeInt(FACTORY_FBC_UPGRADE);
        cmd.writeString(file_name);
        cmd.writeInt(mode);
        cmd.writeInt(upgrade_blk_size);
        sendCmdToTv(cmd, r);
        tmpRet = r.readInt();
        return tmpRet;
    }

    /**
     * @Function: FactorySet_FBC_Brightness
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Brightness(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_BRIGHTNESS);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Brightness
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Brightness() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_BRIGHTNESS);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Contrast
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Contrast(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_CONTRAST);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Contrast
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Contrast() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_CONTRAST);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Saturation
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Saturation(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_SATURATION);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Saturation
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Saturation() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_SATURATION);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_HueColorTint
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_HueColorTint(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_HUE);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_HueColorTint
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_HueColorTint() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_HUE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Backlight
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Backlight(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_BACKLIGHT);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Backlight
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Backlight() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_BACKLIGHT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_backlight_onoff
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_backlight_onoff(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_BACKLIGHT_EN);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Backlight
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_backlight_onoff() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_BACKLIGHT_EN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_SET_LVDS_SSG
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_SET_LVDS_SSG(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_LVDS_SSG);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_AUTO_ELEC_MODE
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_AUTO_ELEC_MODE(int mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_ELEC_MODE);
        cmd.writeInt(mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }


    /**
     * @Function: FactoryGet_AUTO_ELEC_MODE
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_AUTO_ELEC_MODE() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_ELEC_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Backlight_N360
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Backlight_N360(int mode) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_BACKLIFHT_N360);
        cmd.writeInt(mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Backlight_N360
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Backlight_N360() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_BACKLIFHT_N360);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }


    /**
     * @Function: FactorySet_FBC_Picture_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Picture_Mode(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_PIC_MODE);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Picture_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Picture_Mode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_PIC_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Test_Pattern
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Test_Pattern(int value) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_TEST_PATTERN);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Test_Pattern
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Test_Pattern() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_TEST_PATTERN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Gain_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Gain_Red(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_GAIN_RED);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Gain_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Gain_Red() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_GAIN_RED);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Gain_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Gain_Green(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_GAIN_GREEN);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Gain_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Gain_Green() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_GAIN_GREEN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Gain_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Gain_Blue(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_GAIN_BLUE);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Gain_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Gain_Blue() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_GAIN_BLUE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Offset_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Offset_Red(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_OFFSET_RED);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Offset_Red
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Offset_Red() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_OFFSET_RED);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Offset_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Offset_Green(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_OFFSET_GREEN);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Offset_Green
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Offset_Green() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_OFFSET_GREEN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_Offset_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_Offset_Blue(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_OFFSET_BLUE);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_Offset_Blue
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_Offset_Blue() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_OFFSET_BLUE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_ColorTemp_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_ColorTemp_Mode(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_COLORTEMP_MODE);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_ColorTemp_Mode
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_ColorTemp_Mode() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_COLORTEMP_MODE);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_WB_Initial
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactorySet_FBC_WB_Initial(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SET_WB_INIT);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactoryGet_FBC_WB_Initial
     * @Description:
     * @Param:
     * @Return:
     */
    public int FactoryGet_FBC_WB_Initial() {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_WB_INIT);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public class FBC_MAINCODE_INFO
    {
        public String Version;
        public String LastBuild;
        public String GitVersion;
        public String GitBranch;
        public String BuildName;
    }

    public FBC_MAINCODE_INFO FactoryGet_FBC_Get_MainCode_Version() {
        FBC_MAINCODE_INFO  info = new FBC_MAINCODE_INFO();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_GET_MAINCODE_VERSION);
        sendCmdToTv(cmd, r);
        info.Version = r.readString();
        info.LastBuild = r.readString();
        info.GitVersion = r.readString();
        info.GitBranch = r.readString();
        info.BuildName = r.readString();
        return info;
    }

    /**
     * @Function: FactorySet_FBC_Panel_Power_Switch
     * @Description: set fbc panel power switch
     * @Param: value, 0 is fbc panel power off, 1 is panel power on.
     * @Return:
     */
    public int FactorySet_FBC_Panel_Power_Switch(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_PANEL_POWER_SWITCH);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: FactorySet_FBC_SN_Info
     * @Description: set SN info to FBC save
     * @Param: strFactorySN is string to set. len is SN length
     * @Return 0 is success,else is fail:
     */
    public int FactorySet_FBC_SN_Info(String strFactorySN,int len) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_SET_SN);
        cmd.writeString(strFactorySN);
        sendCmdToTv(cmd, r);
        return r.readInt();
    }

    public class Factory_SN_INFO {
        public String STR_SN_INFO;
    }

    public Factory_SN_INFO FactoryGet_FBC_SN_Info() {
        Factory_SN_INFO info = new Factory_SN_INFO();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_GET_SN);
        sendCmdToTv(cmd, r);
        info.STR_SN_INFO = r.readString();
        return info;
    }

    public class FBC_PANEL_INFO
    {
        public String PANEL_MODEL;
    }

    public FBC_PANEL_INFO FactorySet_FBC_Panel_Get_Info() {
        FBC_PANEL_INFO  info = new FBC_PANEL_INFO();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_PANEL_GET_INFO);
        sendCmdToTv(cmd, r);
        info.PANEL_MODEL = r.readString();
        return info;
    }
    //@:value ,default 0
    public int FactorySet_FBC_Panel_Suspend(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_PANEL_SUSPEND);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    public int SetKalaokIOLevel(int level) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(SET_KALAOK_IO_LEVEL);
        cmd.writeInt(level);
        sendCmdToTv(cmd, r);
        Log.d(TAG,"SetKalaokIOLevel aaaaa");
        int ret = r.readInt();
        return ret;
    }
    //@:value ,default 0
    public int FactorySet_FBC_Panel_User_Setting_Default(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_PANEL_USER_SETTING_DEFAULT);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: Read the red gain with specified souce and color temperature
     * @Param:
     * @ Return value: the red gain value
     * */
    public int FactoryWhiteBalanceSetRedGain(int sourceType, int colorTemp_mode, int value) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_GAIN_RED);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceSetGreenGain(int sourceType, int colorTemp_mode, int value) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_GAIN_GREEN);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceSetBlueGain(int sourceType, int colorTemp_mode, int value) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_GAIN_BLUE);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetRedGain(int sourceType, int colorTemp_mode) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_GAIN_RED);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetGreenGain(int sourceType, int colorTemp_mode) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_GAIN_GREEN);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetBlueGain(int sourceType, int colorTemp_mode) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_GAIN_BLUE);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceSetRedOffset(int sourceType, int colorTemp_mode, int value) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_OFFSET_RED);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceSetGreenOffset(int sourceType, int colorTemp_mode, int value) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_OFFSET_GREEN);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceSetBlueOffset(int sourceType, int colorTemp_mode, int value) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_OFFSET_BLUE);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetRedOffset(int sourceType, int colorTemp_mode) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_OFFSET_RED);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetGreenOffset(int sourceType, int colorTemp_mode) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_OFFSET_GREEN);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetBlueOffset(int sourceType, int colorTemp_mode) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_OFFSET_BLUE);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceSetColorTemperature(int sourceType, int colorTemp_mode, int is_save) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_COLOR_TMP);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(is_save);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetColorTemperature(int sourceType) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_COLOR_TMP);
        cmd.writeInt(sourceType);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    /**
     * @Function: Save the white balance data to fbc or g9
     * @Param:
     * @Return value: save OK: 0 , else -1
     *
     * */
    public int FactoryWhiteBalanceSaveParameters(int sourceType, int colorTemp_mode, int r_gain, int g_gain, int b_gain, int r_offset, int g_offset, int b_offset) {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SAVE_PRAMAS);
        cmd.writeInt(sourceType);
        cmd.writeInt(colorTemp_mode);
        cmd.writeInt(r_gain);
        cmd.writeInt(g_gain);
        cmd.writeInt(b_gain);
        cmd.writeInt(r_offset);
        cmd.writeInt(g_offset);
        cmd.writeInt(b_offset);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public class WhiteBalanceParams {
        public int r_gain;        // u1.10, range 0~2047, default is 1024 (1.0x)
        public int g_gain;        // u1.10, range 0~2047, default is 1024 (1.0x)
        public int b_gain;        // u1.10, range 0~2047, default is 1024 (1.0x)
        public int r_offset;      // s11.0, range -1024~+1023, default is 0
        public int g_offset;      // s11.0, range -1024~+1023, default is 0
        public int b_offset;      // s11.0, range -1024~+1023, default is 0
    }


    public  WhiteBalanceParams FactoryWhiteBalanceGetAllParams(int colorTemp_mode)
    {
        WhiteBalanceParams params = new WhiteBalanceParams();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_ALL_PRAMAS);
        cmd.writeInt(colorTemp_mode);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        if (ret == 0)
        {
            params.r_gain = r.readInt();
            params.g_gain = r.readInt();
            params.b_gain = r.readInt();
            params.r_offset = r.readInt();
            params.g_offset = r.readInt();
            params.b_offset = r.readInt();
        }

        return params;
    }

    public int FactoryWhiteBalanceOpenGrayPattern() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_OPEN_GRAY_PATTERN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceCloseGrayPattern() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_CLOSE_GRAY_PATTERN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceSetGrayPattern(int value)
    {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_SET_GRAY_PATTERN);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int FactoryWhiteBalanceGetGrayPattern(int value)
    {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_WHITE_BALANCE_GET_GRAY_PATTERN);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Factory_FBC_Get_LightSensor_Status_N310() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_GET_LIGHT_SENSOR_STATUS_N310);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Factory_FBC_Set_LightSensor_Status_N310(int value)
    {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_SET_LIGHT_SENSOR_STATUS_N310);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    public int Factory_FBC_Get_DreamPanel_Status_N310() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_GET_DREAM_PANEL_STATUS_N310);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Factory_FBC_Set_DreamPanel_Status_N310(int value)
    {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_SET_DREAM_PANEL_STATUS_N310);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    public int Factory_FBC_Get_MULT_PQ_Status_N310() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_GET_MULT_PQ_STATUS_N310);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Factory_FBC_Set_MULT_PQ_Status_N310(int value)
    {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_SET_MULT_PQ_STATUS_N310);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    public int Factory_FBC_Get_MEMC_Status_N310() {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_GET_MEMC_STATUS_N310);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    public int Factory_FBC_Set_MEMC_Status_N310(int value)
    {
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTROY_FBC_SET_MEMC_STATUS_N310);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    //value:
    /*#define REBOOT_FLAG_NORMAL              0x00000000
#define REBOOT_FLAG_UPGRADE             0x80808080
#define REBOOT_FLAG_UPGRADE2    0x88888888      // reserved
#define REBOOT_FLAG_SUSPEND             0x12345678*/
    public int FactorySet_FBC_Power_Reboot(int value) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_POWER_REBOOT);
        cmd.writeInt(value);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }

    //ref to FBC include/key_const.h
    public static final int AML_FBC_KEY_NOP  =  0;

    public static final int AML_FBC_KEY_NUM_PLUS10  =  1;

    public static final int AML_FBC_KEY_NUM_0  =  2;

    public static final int AML_FBC_KEY_NUM_1  =  3;

    public static final int AML_FBC_KEY_NUM_2  =  4;

    public static final int AML_FBC_KEY_NUM_3  =  5;

    public static final int AML_FBC_KEY_NUM_4  =  6;

    public static final int AML_FBC_KEY_NUM_5  =  7;

    public static final int AML_FBC_KEY_NUM_6  =  8;

    public static final int AML_FBC_KEY_NUM_7   = 9;

    public static final int AML_FBC_KEY_NUM_8  =  10;

    public static final int AML_FBC_KEY_NUM_9  =  11;

    public static final int AML_FBC_KEY_UP   = 12;

    public static final int AML_FBC_KEY_DOWN   = 13;

    public static final int AML_FBC_KEY_LEFT  =  14;

    public static final int AML_FBC_KEY_RIGHT  =  15;

    public static final int AML_FBC_KEY_ENTER  =  16;

    public static final int AML_FBC_KEY_EXIT   = 17;

    public static final int AML_FBC_KEY_PAGE_UP  =  18;

    public static final int AML_FBC_KEY_PAGE_DOWN  =  19;

    public static final int AML_FBC_KEY_POWER  =  20;

    public static final int AML_FBC_KEY_SLEEP  =  21;

    public static final int AML_FBC_KEY_HOME   = 22;

    public static final int AML_FBC_KEY_SETUP  =  23;

    public static final int AML_FBC_KEY_OSD  =  24;

    public static final int AML_FBC_KEY_MENU  =  25;

    public static final int AML_FBC_KEY_DISPLAY   = 26;

    public static final int AML_FBC_KEY_MARK   = 27;

    public static final int AML_FBC_KEY_CLEAR  =  28;

    public static final int AML_FBC_KEY_PLAY_PAUSE  =  29;

    public static final int AML_FBC_KEY_STOP  =  30;

    public static final int AML_FBC_KEY_PAUSE  =  31;

    public static final int AML_FBC_KEY_NEXT_CHAP  =  32;

    public static final int AML_FBC_KEY_PREVIOUS_CHAP  =  33;

    public static final int AML_FBC_KEY_FAST_FORWARD  =  34;

    public static final int AML_FBC_KEY_FAST_BACKWARD  =  35;

    public static final int AML_FBC_KEY_REPEAT  =  36;

    public static final int AML_FBC_KEY_PLAY_MODE  =  37;

    public static final int AML_FBC_KEY_SLIDE_SHOW  =  38;

    public static final int AML_FBC_KEY_MUTE  =  39;

    public static final int AML_FBC_KEY_VOL_MINUS  =  40;

    public static final int AML_FBC_KEY_VOL_PLUS   = 41;

    public static final int AML_FBC_KEY_ZOOM  =  42;

    public static final int AML_FBC_KEY_ROTATE  =  43;

    public static final int AML_FBC_KEY_MOUSE_L_DOWN   = 44;

    public static final int AML_FBC_KEY_MOUSE_L_UP  =  45;

    public static final int AML_FBC_KEY_MOUSE_R_DOWN  =  46;

    public static final int AML_FBC_KEY_MOUSE_R_UP  =  47;

    public static final int AML_FBC_KEY_MOUSE_M_DOWN  =  48;

    public static final int AML_FBC_KEY_MOUSE_M_UP   = 49;

    public static final int AML_FBC_KEY_MOUSE_ROLL_DOWN  =  50;

    public static final int AML_FBC_KEY_MOUSE_ROLL_UP  =  51;

    public static final int AML_FBC_KEY_MOUSE_MOVE  =  52;

    public static final int AML_FBC_KEY_LONG_EXIT   = 53;

    public static final int AML_FBC_KEY_LONG_RIGHT  =  54;

    public static final int AML_FBC_KEY_LONG_LEFT   = 55;

    public static final int AML_FBC_KEY_LONG_DOWN  =  56;

    public static final int AML_FBC_KEY_LONG_UP  =  57;

    public static final int AML_FBC_KEY_LONG_ENTER  =  58;

    public static final int AML_FBC_KEY_LONG_MENU   = 59;

    public static final int AML_FBC_KEY_OPEN_CLOSE  =  60;

    public static final int AML_FBC_KEY_NTSC_PAL  =  61;

    public static final int AML_FBC_KEY_PROGRESSIVE  =  62;

    public static final int AML_FBC_KEY_TITLE_CALL  =  63;

    public static final int AML_FBC_KEY_AUDIO  =  64;

    public static final int AML_FBC_KEY_SUBPICTURE  =  65;

    public static final int AML_FBC_KEY_ANGLE   = 66;

    public static final int AML_FBC_KEY_AB_PLAY  =  67;

    public static final int AML_FBC_KEY_RECODE  =  68;

    public static final int AML_FBC_KEY_SHORTCUT   = 69;

    public static final int AML_FBC_KEY_ORIGINAL  =  70;

    public static final int AML_FBC_KEY_BOOKING   = 71;

    public static final int AML_FBC_KEY_ORDER_SYSTEM =   72;

    public static final int AML_FBC_KEY_SOUND_CTRL  =  73;

    public static final int AML_FBC_KEY_FUNCTION  =  74;

    public static final int AML_FBC_KEY_SCHEDULE  =  75;

    public static final int AML_FBC_KEY_FAVOR   = 76;

    public static final int AML_FBC_KEY_RELATION  =  77;

    public static final int AML_FBC_KEY_FIRST   = 78;

    public static final int AML_FBC_KEY_DELETE  =  79;

    public static final int AML_FBC_KEY_SLIDE_RELEASE   = 80;

    public static final int AML_FBC_KEY_SLIDE_TOUCH  =  81;

    public static final int AML_FBC_KEY_SLIDE_LEFT  =  82;

    public static final int AML_FBC_KEY_SLIDE_RIGHT =   83;

    public static final int AML_FBC_KEY_SLIDE_UP   = 84;

    public static final int AML_FBC_KEY_SLIDE_DOWN =   85;

    public static final int AML_FBC_KEY_SLIDE_CLOCKWISE   = 86;

    public static final int AML_FBC_KEY_SLIDE_ANTI_CLOCKWISE  =  87;

    public static final int AML_FBC_KEY_SLIDE_UP_LEFT  =  88;

    public static final int AML_FBC_KEY_SLIDE_UP_RIGHT  =  89;

    public static final int AML_FBC_KEY_SLIDE_DOWN_LEFT  =  90;

    public static final int AML_FBC_KEY_SLIDE_DOWN_RIGHT =   91;

    public static final int AML_FBC_KEY_SLIDE_NULL  =  92;

    public static final int AML_FBC_KEY_MENU_CALL   = 93;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_A  =  94;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_B  =  95;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_C  =  96;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_D  =  97;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_E   = 98;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_F   = 99;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_G   = 100;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_H   = 101;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_I   = 102;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_J   = 103;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_K   = 104;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_L  = 105;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_M   = 106;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_N   = 107;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_O  =  108;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_P  =  109;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_Q  =  110;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_R   = 111;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_S  =  112;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_T  =  113;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_U  =  114;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_V   = 115;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_W  =  116;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_X  =  117;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_Y  =  118;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_Z  =  119;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_0  =  120;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_1   = 121;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_2  =  122;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_3  =  123;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_4  =  124;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_5  =  125;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_6  =  126;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_7  =  127;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_8   = 128;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_9  =  129;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_ENTER  =  130;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_SPACE  =  131;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_BACKSPACE  =  132;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_ESC   = 133;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_CAESURA_SIGN  =  134;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_SUBTRACTION_SIGN   = 135;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_EQUALS_SIGN  =  136;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_LEFT_BRACKET  =  137;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_RIGHT_BRACKET   = 138;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_BACKSLASH   = 139;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_SEMICOLON  =  140;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_QUOTATION_MARK   = 141;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_COMMA   = 142;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_POINT  =  143;

    public static final int AML_FBC_KEY_AML_FBC_KEYBOARD_SLASH  =  144;

    public static final int AML_FBC_KEY_ALARM_SET  =  145;

    public static final int AML_FBC_KEY_ALARM_OFF  =  146;

    public static final int AML_FBC_KEY_IPOD_PLAY_PAUSE  =  147;

    public static final int AML_FBC_KEY_B_TIME_SET_MINUS  =  148;

    public static final int AML_FBC_KEY_B_TIME_SET_PLUS  =  149;

    public static final int AML_FBC_KEY_ALARM_B_ONOFF  =  150;

    public static final int AML_FBC_KEY_SNOOZE_BRIGHTNESS   = 151;

    public static final int AML_FBC_KEY_ALARM_A_ONOFF   = 152;

    public static final int AML_FBC_KEY_A_TIME_SET_MINUS  =  153;

    public static final int AML_FBC_KEY_A_TIME_SET_PLUS  =  154;

    public static final int AML_FBC_KEY_BACK   = 155;

    public static final int AML_FBC_KEY_RADIO_BAND   = 156;

    public static final int AML_FBC_KEY_OPTION  =  157;

    public static final int AML_FBC_KEY_LONG_B_TIME_SET_MINUS  =  158;

    public static final int AML_FBC_KEY_LONG_B_TIME_SET_PLUS   = 159;

    public static final int AML_FBC_KEY_LONG_A_TIME_SET_MINUS  =  160;

    public static final int AML_FBC_KEY_LONG_A_TIME_SET_PLUS  =  161;

    public static final int AML_FBC_KEY_LONG2_B_TIME_SET_MINUS  =  162;

    public static final int AML_FBC_KEY_LONG2_B_TIME_SET_PLUS   = 163;

    public static final int AML_FBC_KEY_LONG2_A_TIME_SET_MINUS  =  164;

    public static final int AML_FBC_KEY_LONG2_A_TIME_SET_PLUS  =  165;

    public static final int AML_FBC_KEY_LONG2_LEFT   = 166;

    public static final int AML_FBC_KEY_LONG2_RIGHT   = 167;

    public static final int AML_FBC_KEY_LONGRLS_LEFT   = 168;

    public static final int AML_FBC_KEY_LONGRLS_RIGHT   = 169;

    public static final int AML_FBC_KEY_LONGRLS_B_TIME_SET_MINUS  =  170;

    public static final int AML_FBC_KEY_LONGRLS_B_TIME_SET_PLUS   = 171;

    public static final int AML_FBC_KEY_LONGRLS_A_TIME_SET_MINUS  =  172;

    public static final int AML_FBC_KEY_LONGRLS_A_TIME_SET_PLUS   = 173;

    public static final int AML_FBC_KEY_LONG2RLS_LEFT   = 174;

    public static final int AML_FBC_KEY_LONG2RLS_RIGHT   = 175;

    public static final int AML_FBC_KEY_LONG2RLS_B_TIME_SET_MINUS  =  176;

    public static final int AML_FBC_KEY_LONG2RLS_B_TIME_SET_PLUS   = 177;

    public static final int AML_FBC_KEY_LONG2RLS_A_TIME_SET_MINUS  =  178;

    public static final int AML_FBC_KEY_LONG2RLS_A_TIME_SET_PLUS   = 179;

    //@param:keyCode  AML_FBC_KEY_XXX   param:default 0
    public int FactorySet_FBC_SEND_KEY_TO_FBC(int keyCode, int param) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_FBC_SEND_KEY_TO_FBC);
        cmd.writeInt(keyCode);
        cmd.writeInt(param);
        sendCmdToTv(cmd, r);
        int ret = r.readInt();
        return ret;
    }
    /**
     * @Description:copy srcPath to desPath
     * @Return:0 success,-1 fail
     */
    public int CopyFile (String srcPath,String desPath)
    {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        cmd.writeInt(FACTORY_COPY_FILE);
        cmd.writeString(srcPath);
        cmd.writeString(desPath);
        sendCmdToTv(cmd, r);
        int tmpRet = r.readInt();
        return tmpRet;
    }


    /**
     * @Function: TvMiscChannelExport
     * @Description: export the /param/dtv.db file to the udisk
     * @Param: none
     * @Return: 0 success , -1 copy fail , -2 other
     */
    public int TvMiscChannelExport(String destPath) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int ret;
        cmd.writeInt(MISC_CHANNEL_EXPORT);
        cmd.writeString(destPath);
        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }
    /**
     * @Function: TvMiscChannelImport
     * @Description: import the dtv.db file from the udisk to the /param directory
     * @Param: none
     * @Return: 0 success , -1 copy fail , -2 other
     */
    public int TvMiscChannelImport(String srcPath) {
        libtv_log_open();
        Parcel cmd = Parcel.obtain();
        Parcel r = Parcel.obtain();
        int ret;
        cmd.writeInt(MISC_CHANNEL_IMPORT);
        cmd.writeString(srcPath);
        sendCmdToTv(cmd, r);
        ret = r.readInt();
        return ret;
    }

    // set listener when not need to listen set null

    public final static int EVENT_AV_PLAYBACK_NODATA            = 1;
    public final static int EVENT_AV_PLAYBACK_RESUME           = 2;
    public final static int EVENT_AV_SCRAMBLED               = 3;
    public final static int EVENT_AV_UNSUPPORT               = 4;

    public class AVPlaybackEvent
    {
        public int mMsgType;
        public int programID;
    }
    public interface AVPlaybackListener {
        void onEvent(AVPlaybackEvent ev);
    };

    public void SetAVPlaybackListener(AVPlaybackListener l) {
        libtv_log_open();
        mAVPlaybackListener = l;
    }

    private class EventHandler extends Handler
    {
        private TvControlManager mTv;
        int mCC_data_array[];
        int mCC_cmd_array[];
        int msg_pdu[];

        public EventHandler(TvControlManager c, Looper looper) {
            super(looper);
            mTv = c;
            mCC_data_array = new int[512];//max data buf
            mCC_cmd_array = new int[128];
            msg_pdu = new int[1200];
        }

        @Override
            public void handleMessage(Message msg) {
                int i = 0, loop_count = 0, tmp_val = 0;
                Parcel p;

                switch (msg.what) {
                    case SUBTITLE_UPDATE_CALLBACK:
                        if (mSubtitleListener != null) {
                            mSubtitleListener.onUpdate();
                        }
                        break;
                    case VFRAME_BMP_EVENT_CALLBACK:
                        p = ((Parcel) (msg.obj));
                        if (mVframBMPListener != null) {
                            VFrameEvent ev = new VFrameEvent();
                            mVframBMPListener.onEvent(ev);
                            ev.FrameNum = p.readInt();
                            ev.FrameSize= p.readInt();
                            ev.FrameWidth= p.readInt();
                            ev.FrameHeight= p.readInt();
                        }
                        break;
                    case SCAN_EVENT_CALLBACK:
                        p = ((Parcel) (msg.obj));
                        if (mScannerListener != null) {
                            ScannerEvent ev = new ScannerEvent();
                            ev.type = p.readInt();
                            ev.precent = p.readInt();
                            ev.totalcount = p.readInt();
                            ev.lock = p.readInt();
                            ev.cnum = p.readInt();
                            ev.freq = p.readInt();
                            ev.programName = p.readString();
                            ev.srvType = p.readInt();
                            ev.msg = p.readString();
                            ev.strength = p.readInt();
                            ev.quality = p.readInt();
                            ev.videoStd = p.readInt();
                            ev.audioStd = p.readInt();
                            ev.isAutoStd = p.readInt();

                            ev.mode = p.readInt();
                            ev.sr = p.readInt();
                            ev.mod = p.readInt();
                            ev.bandwidth = p.readInt();
                            ev.ofdm_mode = p.readInt();
                            ev.ts_id = p.readInt();
                            ev.orig_net_id = p.readInt();
                            ev.serviceID = p.readInt();
                            ev.vid = p.readInt();
                            ev.vfmt = p.readInt();
                            int acnt = p.readInt();
                            if (acnt != 0) {
                                ev.aids = new int[acnt];
                                for (i=0;i<acnt;i++)
                                    ev.aids[i] = p.readInt();
                                ev.afmts = new int[acnt];
                                for (i=0;i<acnt;i++)
                                    ev.afmts[i] = p.readInt();
                                ev.alangs = new String[acnt];
                                for (i=0;i<acnt;i++)
                                    ev.alangs[i] = p.readString();
                                ev.atypes = new int[acnt];
                                for (i=0;i<acnt;i++)
                                    ev.atypes[i] = p.readInt();
                            }
                            ev.pcr = p.readInt();
                            int scnt = p.readInt();
                            if (scnt != 0) {
                                ev.stypes = new int[scnt];
                                for (i=0;i<scnt;i++)
                                    ev.stypes[i] = p.readInt();
                                ev.sids = new int[scnt];
                                for (i=0;i<scnt;i++)
                                    ev.sids[i] = p.readInt();
                                ev.sstypes = new int[scnt];
                                for (i=0;i<scnt;i++)
                                    ev.sstypes[i] = p.readInt();
                                ev.sid1s = new int[scnt];
                                for (i=0;i<scnt;i++)
                                    ev.sid1s[i] = p.readInt();
                                ev.sid2s = new int[scnt];
                                for (i=0;i<scnt;i++)
                                    ev.sid2s[i] = p.readInt();
                                ev.slangs = new String[scnt];
                                for (i=0;i<scnt;i++)
                                    ev.slangs[i] = p.readString();
                            }

                            mScannerListener.onEvent(ev);
                        }
                        break;
                    case VCHIP_CALLBACK:
                        Log.i(TAG,"atsc ---VCHIP_CALLBACK-----------------");
                        p = ((Parcel) (msg.obj));
                        if (mLockStatusListener != null) {
                            VchipLockStatus lockStatus = new VchipLockStatus();
                            lockStatus.blockstatus = p.readInt();
                            lockStatus.blockType = p.readInt();
                            lockStatus.vchipDimension = p.readString();
                            lockStatus.vchipAbbrev = p.readString();
                            lockStatus.vchipText = p.readString();
                            mLockStatusListener.onLock(lockStatus);
                        }
                        break;
                    case EPG_EVENT_CALLBACK:
                        p = ((Parcel) (msg.obj));
                        if (mEpgListener != null) {
                            EpgEvent ev = new EpgEvent();
                            ev.type = p.readInt();
                            ev.time = p.readInt();
                            ev.programID = p.readInt();
                            ev.channelID = p.readInt();
                            mEpgListener.onEvent(ev);
                        }
                        break;
                    case DTV_AV_PLAYBACK_CALLBACK:
                        p = ((Parcel) (msg.obj));
                        if (mAVPlaybackListener != null) {
                            AVPlaybackEvent ev = new AVPlaybackEvent();
                            ev.mMsgType= p.readInt();
                            ev.programID= p.readInt();
                            mAVPlaybackListener.onEvent(ev);
                        }
                        break ;
                    case SEARCH_CALLBACK:
                        if (mSigChanSearchListener != null) {
                            if (msg_pdu != null) {
                                loop_count = ((Parcel) (msg.obj)).readInt();
                                for (i = 0; i < loop_count; i++) {
                                    msg_pdu[i] = ((Parcel) (msg.obj)).readInt();
                                }
                                mSigChanSearchListener.onChannelSearchChange(msg_pdu);
                            }
                        }
                        break;
                    case SIGLE_DETECT_CALLBACK:
                        if (mSigInfoChangeLister != null) {
                            tvin_info_t sig_info = new tvin_info_t();
                            sig_info.trans_fmt = tvin_trans_fmt.values()[(((Parcel) (msg.obj)).readInt())];
                            sig_info.fmt = tvin_sig_fmt_e.valueOf(((Parcel) (msg.obj)).readInt());
                            sig_info.status = tvin_sig_status_t.values()[(((Parcel) (msg.obj)).readInt())];
                            sig_info.reserved = ((Parcel) (msg.obj)).readInt();
                            mSigInfoChangeLister.onSigChange(sig_info);
                        }
                        break;
                    case VGA_CALLBACK:
                        if (mVGAChangeListener != null) {
                            mVGAChangeListener.onVGAAdjustChange(((Parcel) (msg.obj)).readInt());
                        }
                        break;
                    case STATUS_3D_CALLBACK:
                        if (mStatus3DChangeListener != null) {
                            mStatus3DChangeListener.onStatus3DChange(((Parcel) (msg.obj)).readInt());
                        }
                        break;
                    case SOURCE_CONNECT_CALLBACK:
                        if (mSourceConnectChangeListener != null) {
                            mSourceConnectChangeListener.onSourceConnectChange( SourceInput.values()[((Parcel) (msg.obj)).readInt()], ((Parcel) (msg.obj)).readInt());
                        }
                        break;
                    case HDMIRX_CEC_CALLBACK:
                        if (mHDMIRxCECListener != null) {
                            if (msg_pdu != null) {
                                loop_count = ((Parcel) (msg.obj)).readInt();
                                for (i = 0; i < loop_count; i++) {
                                    msg_pdu[i] = ((Parcel) (msg.obj)).readInt();
                                }
                                mHDMIRxCECListener.onHDMIRxCECMessage(loop_count, msg_pdu);
                            }
                        }
                        break;
                    case UPGRADE_FBC_CALLBACK:
                        if (mUpgradeFBCListener != null) {
                            loop_count = ((Parcel) (msg.obj)).readInt();
                            tmp_val = ((Parcel) (msg.obj)).readInt();
                            Log.d(TAG, "state = " + loop_count + "    param = " + tmp_val);
                            mUpgradeFBCListener.onUpgradeStatus(loop_count, tmp_val);
                        }
                        break;
                    case DREAM_PANEL_CALLBACK:
                        break;
                    case ADC_CALIBRATION_CALLBACK:
                        if (mAdcCalibrationListener != null) {
                            mAdcCalibrationListener.onAdcCalibrationChange(((Parcel) (msg.obj)).readInt());
                        }
                        break;
                    case SOURCE_SWITCH_CALLBACK:
                        if (mSourceSwitchListener != null) {
                            mSourceSwitchListener.onSourceSwitchStatusChange(
                                    SourceInput.values()[(((Parcel) (msg.obj)).readInt())], ((Parcel) (msg.obj)).readInt());
                        }
                        break;
                    case CHANNEL_SELECT_CALLBACK:
                        if (mChannelSelectListener != null) {
                            if (msg_pdu != null) {
                                loop_count = ((Parcel) (msg.obj)).readInt();
                                for (i = 0; i < loop_count; i++) {
                                    msg_pdu[i] = ((Parcel) (msg.obj)).readInt();
                                }
                                mChannelSelectListener.onChannelSelect(msg_pdu);
                            }
                        }
                        break;
                    case SERIAL_COMMUNICATION_CALLBACK:
                        if (mSerialCommunicationListener != null) {
                            if (msg_pdu != null) {
                                int dev_id = ((Parcel) (msg.obj)).readInt();
                                loop_count = ((Parcel) (msg.obj)).readInt();
                                for (i = 0; i < loop_count; i++) {
                                    msg_pdu[i] = ((Parcel) (msg.obj)).readInt();
                                }
                                mSerialCommunicationListener.onSerialCommunication(dev_id, loop_count, msg_pdu);
                            }
                        }
                        break;
                    case CLOSE_CAPTION_CALLBACK:
                        if (mCloseCaptionListener != null) {
                            loop_count = ((Parcel) (msg.obj)).readInt();
                            Log.d(TAG, "cc listenner data count =" + loop_count);
                            for (i = 0; i < loop_count; i++) {
                                mCC_data_array[i] = ((Parcel) (msg.obj)).readInt();
                            }
                            //data len write to end
                            mCC_data_array[mCC_data_array.length - 1] =  loop_count;
                            loop_count = ((Parcel) (msg.obj)).readInt();
                            for (i = 0; i < loop_count; i++) {
                                mCC_cmd_array[i] = ((Parcel) (msg.obj)).readInt();
                            }
                            mCC_cmd_array[mCC_cmd_array.length - 1] =  loop_count;
                            mCloseCaptionListener.onCloseCaptionProcess(mCC_data_array, mCC_cmd_array);
                        }
                        break;
                    default:
                        Log.e(TAG, "Unknown message type " + msg.what);
                        break;
                }
                return;
            }
    }

    private static void postEventFromNative(Object tv_ref, int what, Parcel ext) {
        ext.setDataPosition(0);

        TvControlManager c = (TvControlManager)((WeakReference) tv_ref).get();
        if (c == null)
            return;
        if (c.mEventHandler != null) {
            Message m = c.mEventHandler.obtainMessage(what, 0, 0, ext);
            c.mEventHandler.sendMessage(m);
        }
    }

    public static final int TV_ERROR_UNKNOWN = 1;
    public static final int TV_ERROR_SERVER_DIED = 100;

    public interface ErrorCallback {
        void onError(int error, TvControlManager tv);
    };

    // set listener when not need to listen set null
    public interface SigInfoChangeListener {
        void onSigChange(tvin_info_t sig_info);
    };

    public void SetSigInfoChangeListener(SigInfoChangeListener l) {
        libtv_log_open();
        mSigInfoChangeLister = l;
    }

    public enum SIG_LINE_STATUS {
        SIG_LINE_PLUG_IN(0),
            SIG_LINE_PULL_OUT(1),
            SIG_LINE_UNKNOW(2);
        private int val;

        SIG_LINE_STATUS(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    // set listener when not need to listen set null
    public interface SigLineChangeListener {
        void onSigLineStatusChange(SourceInput inputline,  SIG_LINE_STATUS status);
    };

    public void SetSigLineChangeListener(SigLineChangeListener l) {
        //
    }

    public interface SigChannelSearchListener {
        void onChannelSearchChange(int msg_pdu[]);
    };

    public void SetSigChannelSearchListener(SigChannelSearchListener l) {
        libtv_log_open();
        mSigChanSearchListener = l;
    }

    public enum CC_VGA_AUTO_ADJUST_STATUS {
        CC_VGA_AUTO_ADJUST_START(0),
            CC_VGA_AUTO_ADJUST_SUCCESS(1),
            CC_VGA_AUTO_ADJUST_FAILED(-1),
            CC_VGA_AUTO_ADJUST_CURTIMMING_FAILED(-2),
            CC_VGA_AUTO_ADJUST_PARA_FAILED(-3),
            CC_VGA_AUTO_ADJUST_TERMINATED(-4),
            CC_VGA_AUTO_ADJUST_IDLE(-5);

        private int val;

        CC_VGA_AUTO_ADJUST_STATUS(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public interface VGAAdjustChangeListener {
        void onVGAAdjustChange(int state);
    };

    public void SetVGAChangeListener(VGAAdjustChangeListener l) {
        libtv_log_open();
        mVGAChangeListener = l;
    }

    public interface Status3DChangeListener {
        void onStatus3DChange(int state);
    }

    public interface StatusTVChangeListener {
        void onStatusTVChange(int type,int state,int mode,int freq,int para1,int para2);
    }

    public interface StatusSourceConnectListener {
        void onSourceConnectChange(SourceInput source, int msg);
    }

    public void SetSourceConnectListener(StatusSourceConnectListener l) {
        libtv_log_open();
        mSourceConnectChangeListener = l;
    }

    public interface HDMIRxCECListener {
        void onHDMIRxCECMessage(int msg_len, int msg_buf[]);
    }

    public void SetHDMIRxCECListener(HDMIRxCECListener l) {
        libtv_log_open();
        mHDMIRxCECListener = l;
    }

    public interface UpgradeFBCListener {
        void onUpgradeStatus(int state, int param);
    }

    public void SetUpgradeFBCListener(UpgradeFBCListener l) {
        libtv_log_open();
        mUpgradeFBCListener = l;
    }

    public void SetStatus3DChangeListener(Status3DChangeListener l) {
        libtv_log_open();
        mStatus3DChangeListener = l;
    }

    public void SetStatusTVChangeListener(StatusTVChangeListener l) {
        libtv_log_open();
        mStatusTVChangeListener = l;
    }

    public interface DreamPanelChangeListener {
        void onDreamPanelChange(int msg_pdu[]);
    };

    public void SetDreamPanelChangeListener(DreamPanelChangeListener l) {
        libtv_log_open();
        mDreamPanelChangeListener = l;
    }

    public interface AdcCalibrationListener {
        void onAdcCalibrationChange(int state);
    }

    public void SetAdcCalibrationListener(AdcCalibrationListener l) {
        libtv_log_open();
        mAdcCalibrationListener = l;
    }

    public interface SourceSwitchListener {
        void onSourceSwitchStatusChange(SourceInput input, int state);
    }

    public void SetSourceSwitchListener(SourceSwitchListener l) {
        libtv_log_open();
        mSourceSwitchListener = l;
    }

    public interface ChannelSelectListener {
        void onChannelSelect(int msg_pdu[]);
    }

    public void SetChannelSelectListener(ChannelSelectListener l) {
        libtv_log_open();
        mChannelSelectListener = l;
    }

    public final void setErrorCallback(ErrorCallback cb) {
        libtv_log_open();
        mErrorCallback = cb;
    }

    public interface SerialCommunicationListener {
        //dev_id, refer to enum SerialDeviceID
        void onSerialCommunication(int dev_id, int msg_len, int msg_pdu[]);
    };

    public void SetSerialCommunicationListener(SerialCommunicationListener l) {
        libtv_log_open();
        mSerialCommunicationListener = l;
    }

    public interface CloseCaptionListener {
        void onCloseCaptionProcess(int data_buf[], int cmd_buf[]);
    };

    public void SetCloseCaptionListener(CloseCaptionListener l) {
        libtv_log_open();
        mCloseCaptionListener = l;
    }

    private native final void native_setup(Object tv_this);

    private native final void native_release();

    public native void addCallbackBuffer(byte cb[]);

    public native final void unlock();

    public native final void lock();

    public native final void reconnect() throws IOException;

    private native int processCmd(Parcel p, Parcel r);

    private int sendCmdToTv(Parcel p, Parcel r) {
        p.setDataPosition(0);
        int ret = processCmd(p, r);
        r.setDataPosition(0);
        return ret;
    }

    static {
        System.loadLibrary("tv_jni");
    }
}
