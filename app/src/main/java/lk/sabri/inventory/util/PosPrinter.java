package lk.sabri.inventory.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.util.Log;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;

import java.lang.ref.WeakReference;
import java.util.Date;

public class PosPrinter {

    public interface OnPrinterListener{
        void onComplete();
        void onError();
    }

    private final WeakReference<Context> context;

    public PosPrinter(WeakReference<Context> context) {
        this.context = context;
    }


    public void print(DeviceConnection printerConnection,String text,OnPrinterListener listener){
        new AsyncBluetoothEscPosPrint(context.get(), new AsyncEscPosPrint.OnPrintFinished() {
            @Override
            public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                if(listener != null)
                    listener.onError();
            }

            @Override
            public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                if(listener != null)
                    listener.onComplete();
            }
        }).execute(getAsyncEscPosPrinter(printerConnection, text));
    }

    private AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection,String printText) {
    //    SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 384, 78f, 48);
        // AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 300, 104, 60);
        return printer.addTextToPrint(
                printText
        );

//      return   printer.addTextToPrint(
//                "test "
//        );
    }
}


 class AsyncBluetoothEscPosPrint extends AsyncEscPosPrint {
    public AsyncBluetoothEscPosPrint(Context context) {
        super(context);
    }

    public AsyncBluetoothEscPosPrint(Context context, OnPrintFinished onPrintFinished) {
        super(context, onPrintFinished);
    }

    protected PrinterStatus doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return new PrinterStatus(null, AsyncEscPosPrint.FINISH_NO_PRINTER);
        }

  AsyncEscPosPrinter printerData = printersData[0];
        DeviceConnection deviceConnection = printerData.getPrinterConnection();

        this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);

        if (deviceConnection == null) {
            printersData[0] = new AsyncEscPosPrinter(
                    BluetoothPrintersConnections.selectFirstPaired(),
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine()
            );
            printersData[0].setTextsToPrint(printerData.getTextsToPrint());
        } else {
            try {
                deviceConnection.connect();
            } catch (EscPosConnectionException e) {
                e.printStackTrace();
            }
        }

        return super.doInBackground(printersData);
    }
}

class AsyncEscPosPrinter extends EscPosPrinterSize {
    private DeviceConnection printerConnection;
    private String[] textsToPrint = new String[0];

    public AsyncEscPosPrinter(DeviceConnection printerConnection, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine) {
        super(printerDpi, printerWidthMM, printerNbrCharactersPerLine);
        this.printerConnection = printerConnection;
    }

    public DeviceConnection getPrinterConnection() {
        return this.printerConnection;
    }

    public AsyncEscPosPrinter setTextsToPrint(String[] textsToPrint) {
        this.textsToPrint = textsToPrint;
        return this;
    }

    public AsyncEscPosPrinter addTextToPrint(String textToPrint) {
        String[] tmp = new String[this.textsToPrint.length + 1];
        System.arraycopy(this.textsToPrint, 0, tmp, 0, this.textsToPrint.length);
        tmp[this.textsToPrint.length] = textToPrint;
        this.textsToPrint = tmp;
        return this;
    }

    public String[] getTextsToPrint() {
        return this.textsToPrint;
    }
}


abstract class AsyncEscPosPrint extends AsyncTask<AsyncEscPosPrinter, Integer,AsyncEscPosPrint.PrinterStatus> {
    public final static int FINISH_SUCCESS = 1;
    public final static int FINISH_NO_PRINTER = 2;
    public final static int FINISH_PRINTER_DISCONNECTED = 3;
    public final static int FINISH_PARSER_ERROR = 4;
    public final static int FINISH_ENCODING_ERROR = 5;
    public final static int FINISH_BARCODE_ERROR = 6;

    protected final static int PROGRESS_CONNECTING = 1;
    protected final static int PROGRESS_CONNECTED = 2;
    protected final static int PROGRESS_PRINTING = 3;
    protected final static int PROGRESS_PRINTED = 4;

    protected ProgressDialog dialog;
    protected WeakReference<Context> weakContext;
    protected AsyncEscPosPrint.OnPrintFinished onPrintFinished;


    public AsyncEscPosPrint(Context context) {
        this(context, null);
    }

    public AsyncEscPosPrint(Context context, AsyncEscPosPrint.OnPrintFinished onPrintFinished) {
        this.weakContext = new WeakReference<>(context);
        this.onPrintFinished = onPrintFinished;
    }

    protected AsyncEscPosPrint.PrinterStatus doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return new AsyncEscPosPrint.PrinterStatus(null, AsyncEscPosPrint.FINISH_NO_PRINTER);
        }

        this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);

        AsyncEscPosPrinter printerData = printersData[0];

        try {
            DeviceConnection deviceConnection = printerData.getPrinterConnection();

            if(deviceConnection == null) {
                return new AsyncEscPosPrint.PrinterStatus(null, AsyncEscPosPrint.FINISH_NO_PRINTER);
            }

            EscPosPrinter printer = new EscPosPrinter(
                    deviceConnection,
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine(),
                    new EscPosCharsetEncoding("windows-1252", 16)
            );

            // printer.useEscAsteriskCommand(true);

            this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTING);

            String[] textsToPrint = printerData.getTextsToPrint();

            for(String textToPrint : textsToPrint) {
                printer.printFormattedTextAndCut(textToPrint);
                Thread.sleep(500);
            }

            this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTED);

        } catch (EscPosConnectionException e) {
            e.printStackTrace();
            return new AsyncEscPosPrint.PrinterStatus(printerData, AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED);
        } catch (EscPosParserException e) {
            e.printStackTrace();
            return new AsyncEscPosPrint.PrinterStatus(printerData, AsyncEscPosPrint.FINISH_PARSER_ERROR);
        } catch (EscPosEncodingException e) {
            e.printStackTrace();
            return new AsyncEscPosPrint.PrinterStatus(printerData, AsyncEscPosPrint.FINISH_ENCODING_ERROR);
        } catch (EscPosBarcodeException e) {
            e.printStackTrace();
            return new AsyncEscPosPrint.PrinterStatus(printerData, AsyncEscPosPrint.FINISH_BARCODE_ERROR);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new AsyncEscPosPrint.PrinterStatus(printerData, AsyncEscPosPrint.FINISH_SUCCESS);
    }

    protected void onPreExecute() {
        if (this.dialog == null) {
            Context context = weakContext.get();

            if (context == null) {
                return;
            }

            this.dialog = new ProgressDialog(context);
            this.dialog.setTitle("Printing in progress...");
            this.dialog.setMessage("...");
            this.dialog.setProgressNumberFormat("%1d / %2d");
            this.dialog.setCancelable(false);
            this.dialog.setIndeterminate(false);
            this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.dialog.show();
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        switch (progress[0]) {
            case AsyncEscPosPrint.PROGRESS_CONNECTING:
                this.dialog.setMessage("Connecting printer...");
                break;
            case AsyncEscPosPrint.PROGRESS_CONNECTED:
                this.dialog.setMessage("Printer is connected...");
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTING:
                this.dialog.setMessage("Printer is printing...");
                break;
            case AsyncEscPosPrint.PROGRESS_PRINTED:
                this.dialog.setMessage("Printer has finished...");
                break;
        }
        this.dialog.setProgress(progress[0]);
        this.dialog.setMax(4);
    }

    protected void onPostExecute(AsyncEscPosPrint.PrinterStatus result) {
        this.dialog.dismiss();
        this.dialog = null;

        Context context = weakContext.get();

        if (context == null) {
            return;
        }

        switch (result.getPrinterStatus()) {
            case AsyncEscPosPrint.FINISH_SUCCESS:
                new AlertDialog.Builder(context)
                        .setTitle("Print Success!")
                        .setMessage("")
                        .setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss())
                        .show();
                break;
            case AsyncEscPosPrint.FINISH_NO_PRINTER:
                new AlertDialog.Builder(context)
                        .setTitle("No printer")
                        .setMessage("The application can't find any printer connected.")
                        .show();
                break;
            case AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED:
                new AlertDialog.Builder(context)
                        .setTitle("Broken connection")
                        .setMessage("Unable to connect the printer.")
                        .show();
                break;
            case AsyncEscPosPrint.FINISH_PARSER_ERROR:
                new AlertDialog.Builder(context)
                        .setTitle("Invalid formatted text")
                        .setMessage("It seems to be an invalid syntax problem.")
                        .show();
                break;
            case AsyncEscPosPrint.FINISH_ENCODING_ERROR:
                new AlertDialog.Builder(context)
                        .setTitle("Bad selected encoding")
                        .setMessage("The selected encoding character returning an error.")
                        .show();
                break;
            case AsyncEscPosPrint.FINISH_BARCODE_ERROR:
                new AlertDialog.Builder(context)
                        .setTitle("Invalid barcode")
                        .setMessage("Data send to be converted to barcode or QR code seems to be invalid.")
                        .show();
                break;
        }
        if(this.onPrintFinished != null) {
            if (result.getPrinterStatus() == AsyncEscPosPrint.FINISH_SUCCESS) {
                this.onPrintFinished.onSuccess(result.getAsyncEscPosPrinter());
            } else {
                this.onPrintFinished.onError(result.getAsyncEscPosPrinter(), result.getPrinterStatus());
            }
        }
    }

    public  class PrinterStatus {
        private AsyncEscPosPrinter asyncEscPosPrinter;
        private int printerStatus;

        public PrinterStatus (AsyncEscPosPrinter asyncEscPosPrinter, int printerStatus) {
            this.asyncEscPosPrinter = asyncEscPosPrinter;
            this.printerStatus = printerStatus;
        }

        public AsyncEscPosPrinter getAsyncEscPosPrinter() {
            return asyncEscPosPrinter;
        }

        public int getPrinterStatus() {
            return printerStatus;
        }
    }

    public  abstract static class OnPrintFinished {
        public abstract void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException);
        public abstract void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter);
    }
}
