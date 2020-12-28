package lk.sabri.inventory.util;

import android.content.Context;

import com.bxl.config.editor.BXLConfigLoader;

import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.config.JposEntry;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.DirectIOEvent;
import jpos.events.DirectIOListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class BixolonPrinter implements ErrorListener, OutputCompleteListener, StatusUpdateListener, DirectIOListener, DataListener {

    //Constants
    // ------------------- alignment ------------------- //
    public static int ALIGNMENT_LEFT = 1;
    public static int ALIGNMENT_CENTER = 2;
    public static int ALIGNMENT_RIGHT = 4;
    // ------------------- Text attribute ------------------- //
    public static int ATTRIBUTE_NORMAL = 0;
    public static int ATTRIBUTE_FONT_A = 1;
    public static int ATTRIBUTE_FONT_B = 2;
    public static int ATTRIBUTE_FONT_C = 4;
    public static int ATTRIBUTE_BOLD = 8;
    public static int ATTRIBUTE_UNDERLINE = 16;
    public static int ATTRIBUTE_REVERSE = 32;
    public static int ATTRIBUTE_FONT_D = 64;
//    // ------------------- Barcode Symbology ------------------- //
//    public static int BARCODE_TYPE_UPCA = POSPrinterConst.PTR_BCS_UPCA;
//    public static int BARCODE_TYPE_UPCE = POSPrinterConst.PTR_BCS_UPCE;
//    public static int BARCODE_TYPE_EAN8 = POSPrinterConst.PTR_BCS_EAN8;
//    public static int BARCODE_TYPE_EAN13 = POSPrinterConst.PTR_BCS_EAN13;
//    public static int BARCODE_TYPE_ITF = POSPrinterConst.PTR_BCS_ITF;
//    public static int BARCODE_TYPE_Codabar = POSPrinterConst.PTR_BCS_Codabar;
//    public static int BARCODE_TYPE_Code39 = POSPrinterConst.PTR_BCS_Code39;
//    public static int BARCODE_TYPE_Code93 = POSPrinterConst.PTR_BCS_Code93;
//    public static int BARCODE_TYPE_Code128 = POSPrinterConst.PTR_BCS_Code128;
//    public static int BARCODE_TYPE_PDF417 = POSPrinterConst.PTR_BCS_PDF417;
//    public static int BARCODE_TYPE_MAXICODE = POSPrinterConst.PTR_BCS_MAXICODE;
//    public static int BARCODE_TYPE_DATAMATRIX = POSPrinterConst.PTR_BCS_DATAMATRIX;
//    public static int BARCODE_TYPE_QRCODE = POSPrinterConst.PTR_BCS_QRCODE;
//    public static int BARCODE_TYPE_EAN128 = POSPrinterConst.PTR_BCS_EAN128;
//    // ------------------- Barcode HRI ------------------- //
//    public static int BARCODE_HRI_NONE = POSPrinterConst.PTR_BC_TEXT_NONE;
//    public static int BARCODE_HRI_ABOVE = POSPrinterConst.PTR_BC_TEXT_ABOVE;
//    public static int BARCODE_HRI_BELOW = POSPrinterConst.PTR_BC_TEXT_BELOW;

    //instance
    private Context context;
    private POSPrinter posPrinter;
    private BXLConfigLoader bxlConfigLoader;

    //primary data
    private int mPortType;
    private String mAddress;

    public BixolonPrinter(Context context) {
        this.context = context;

        posPrinter = new POSPrinter(this.context);
        posPrinter.addStatusUpdateListener(this);
        posPrinter.addErrorListener(this);
        posPrinter.addOutputCompleteListener(this);
        posPrinter.addDirectIOListener(this);


        bxlConfigLoader = new BXLConfigLoader(this.context);
        try {
            bxlConfigLoader.openFile();
        } catch (Exception e) {
            bxlConfigLoader.newFile();
        }

    }

    public boolean printerOpen(int portType, String logicalName, String address, boolean isAsyncMode) {
        if (setTargetDevice(portType, logicalName, BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER, address)) {
            try {
                posPrinter.open(logicalName);
                posPrinter.claim(10000);
                posPrinter.setDeviceEnabled(true);
                posPrinter.setAsyncMode(isAsyncMode);

                mPortType = portType;
                mAddress = address;
            } catch (JposException e) {
                e.printStackTrace();
                try {
                    posPrinter.close();
                } catch (JposException e1) {
                    e1.printStackTrace();
                }

                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean printerClose() {
        try {
            if (posPrinter.getClaimed()) {
                posPrinter.setDeviceEnabled(false);
                posPrinter.close();
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean setTargetDevice(int portType, String logicalName, int deviceCategory, String address) {
        try {
            for (Object entry : bxlConfigLoader.getEntries()) {
                JposEntry jposEntry = (JposEntry) entry;
                if (jposEntry.getLogicalName().equals(logicalName)) {
                    bxlConfigLoader.removeEntry(jposEntry.getLogicalName());
                }
            }

            bxlConfigLoader.addEntry(logicalName, deviceCategory, getProductName(logicalName), portType, address, true);

            bxlConfigLoader.saveFile();
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean beginTransactionPrint() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_TRANSACTION);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean endTransactionPrint() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_NORMAL);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean printText(String data, int alignment, int attribute, int textSize) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            String strOption = EscapeSequence.getString(0);

            if ((alignment & ALIGNMENT_LEFT) == ALIGNMENT_LEFT) {
                strOption += EscapeSequence.getString(4);
            }

            if ((alignment & ALIGNMENT_CENTER) == ALIGNMENT_CENTER) {
                strOption += EscapeSequence.getString(5);
            }

            if ((alignment & ALIGNMENT_RIGHT) == ALIGNMENT_RIGHT) {
                strOption += EscapeSequence.getString(6);
            }

            if ((attribute & ATTRIBUTE_FONT_A) == ATTRIBUTE_FONT_A) {
                strOption += EscapeSequence.getString(1);
            }

            if ((attribute & ATTRIBUTE_FONT_B) == ATTRIBUTE_FONT_B) {
                strOption += EscapeSequence.getString(2);
            }

            if ((attribute & ATTRIBUTE_FONT_C) == ATTRIBUTE_FONT_C) {
                strOption += EscapeSequence.getString(3);
            }

            if ((attribute & ATTRIBUTE_FONT_D) == ATTRIBUTE_FONT_D) {
                strOption += EscapeSequence.getString(33);
            }

            if ((attribute & ATTRIBUTE_BOLD) == ATTRIBUTE_BOLD) {
                strOption += EscapeSequence.getString(7);
            }

            if ((attribute & ATTRIBUTE_UNDERLINE) == ATTRIBUTE_UNDERLINE) {
                strOption += EscapeSequence.getString(9);
            }

            if ((attribute & ATTRIBUTE_REVERSE) == ATTRIBUTE_REVERSE) {
                strOption += EscapeSequence.getString(11);
            }

            switch (textSize) {
                case 1:
                    strOption += EscapeSequence.getString(17);
                    strOption += EscapeSequence.getString(26);
//                    strOption += EscapeSequence.getString(25);
                    break;
                case 2:
                    strOption += EscapeSequence.getString(18);
//                    strOption += EscapeSequence.getString(26);
                    strOption += EscapeSequence.getString(25);
                    break;
                case 3:
                    strOption += EscapeSequence.getString(19);
                    strOption += EscapeSequence.getString(27);
                    break;
                case 4:
                    strOption += EscapeSequence.getString(20);
                    strOption += EscapeSequence.getString(28);
                    break;
                case 5:
                    strOption += EscapeSequence.getString(21);
                    strOption += EscapeSequence.getString(29);
                    break;
                case 6:
                    strOption += EscapeSequence.getString(22);
                    strOption += EscapeSequence.getString(30);
                    break;
                case 7:
                    strOption += EscapeSequence.getString(23);
                    strOption += EscapeSequence.getString(31);
                    break;
                case 8:
                    strOption += EscapeSequence.getString(24);
                    strOption += EscapeSequence.getString(32);
                    break;
                default:
                    strOption += EscapeSequence.getString(17);
                    strOption += EscapeSequence.getString(25);
                    break;
            }

            posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, strOption + data);
        } catch (JposException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    public boolean printBarcode(String data, int symbology, int width, int height, int alignment, int hri) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BC_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BC_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BC_RIGHT;
            }

            posPrinter.printBarCode(POSPrinterConst.PTR_S_RECEIPT, data, symbology, height, width, alignment, hri);
        } catch (JposException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    @Override
    public void dataOccurred(DataEvent dataEvent) {

    }

    @Override
    public void directIOOccurred(DirectIOEvent directIOEvent) {

    }

    @Override
    public void errorOccurred(ErrorEvent errorEvent) {

    }

    @Override
    public void outputCompleteOccurred(OutputCompleteEvent outputCompleteEvent) {

    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {

    }

    private String getProductName(String name) {
        String productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200II;

        switch (name) {
            case "SPP-R200III":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200III;
                break;
            case "SPP-R210":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R210;
                break;
            case "SPP-R215":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R215;
                break;
            case "SPP-R220":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R220;
                break;
            case "SPP-R300":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R300;
                break;
            case "SPP-R310":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R310;
                break;
            case "SPP-R318":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R318;
                break;
            case "SPP-R400":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R400;
                break;
            case "SPP-R410":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R410;
                break;
            case "SPP-R418":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_R418;
                break;
            case "SPP-100II":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_100II;
                break;
            case "SRP-350III":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_350III;
                break;
            case "SRP-352III":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_352III;
                break;
            case "SRP-350plusIII":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_350PLUSIII;
                break;
            case "SRP-352plusIII":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_352PLUSIII;
                break;
            case "SRP-380":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_380;
                break;
            case "SRP-382":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_382;
                break;
            case "SRP-383":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_383;
                break;
            case "SRP-340II":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_340II;
                break;
            case "SRP-342II":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_342II;
                break;
            case "SRP-Q200":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q200;
                break;
            case "SRP-Q300":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q300;
                break;
            case "SRP-Q302":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q302;
                break;
            case "SRP-QE300":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE300;
                break;
            case "SRP-QE302":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE302;
                break;
            case "SRP-E300":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_E300;
                break;
            case "SRP-E302":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_E302;
                break;
            case "SRP-330II":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_330II;
                break;
            case "SRP-332II":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_332II;
                break;
            case "SRP-S300":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_S300;
                break;
            case "SRP-F310":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_F310;
                break;
            case "SRP-F312":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_F312;
                break;
            case "SRP-F310II":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_F310II;
                break;
            case "SRP-F312II":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_F312II;
                break;
            case "SRP-F313II":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_F313II;
                break;
            case "SRP-275III":
                productName = BXLConfigLoader.PRODUCT_NAME_SRP_275III;
//        } else if ((name.equals("BK3-2"))) {
//            productName = BXLConfigLoader.PRODUCT_NAME_BK3_2;
                break;
            case "BK3-3":
                productName = BXLConfigLoader.PRODUCT_NAME_BK3_3;
                break;
            case "SLP X-Series":
                productName = BXLConfigLoader.PRODUCT_NAME_SLP_X_SERIES;
                break;
            case "SLP-DX420":
                productName = BXLConfigLoader.PRODUCT_NAME_SLP_DX420;
                break;
            case "SPP-L410II":
                productName = BXLConfigLoader.PRODUCT_NAME_SPP_L410II;
                break;
            case "MSR":
                productName = BXLConfigLoader.PRODUCT_NAME_MSR;
                break;
            case "CashDrawer":
                productName = BXLConfigLoader.PRODUCT_NAME_CASH_DRAWER;
                break;
            case "LocalSmartCardRW":
                productName = BXLConfigLoader.PRODUCT_NAME_LOCAL_SMART_CARD_RW;
                break;
            case "SmartCardRW":
                productName = BXLConfigLoader.PRODUCT_NAME_SMART_CARD_RW;
                break;
        }

        return productName;
    }
}
