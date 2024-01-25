package lk.sabri.inventory.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.print.PrintAttributes;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uttampanchasara.pdfgenerator.CreatePdf;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lk.sabri.inventory.R;
import lk.sabri.inventory.data.Customer;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.Item;
import lk.sabri.inventory.data.Payment;
import lk.sabri.inventory.data.PaymentMethodAnnotation;
import lk.sabri.inventory.util.AppUtil;
import lk.sabri.inventory.util.BixolonPrinter;
import lk.sabri.inventory.util.PosPrinter;
import lk.sabri.inventory.util.TimeFormatter;

public abstract class InvoiceBaseActivity extends AppCompatActivity {

    //Constants
    protected static final int REQUEST_PRINTER_OPEN = 600;
    protected static final int REQUEST_PERMISSION_WRITE = 601;
    protected static final int PRINTER_CHAR_LENGTH_SIZE_1 = 69;
    protected static final int PRINTER_CHAR_LENGTH_SIZE_2 = 34;
    protected static final String FOLDER_MAIN = "EVERGREEN_LANKA";

    //instances
    protected static BixolonPrinter bixolonPrinter = null;
    private Intent sharingIntent;

    //primary data
    protected String invoiceId;

    public boolean onCustomCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share_menu, menu);
        return true;
    }

    protected abstract void onPrintingInvoices();

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_item_share) {

//            File file = new File(Environment.getExternalStorageDirectory(), FOLDER_MAIN + "/" + invoiceId + ".pdf");
//            final boolean fileExits = file.exists();

            if (invoiceId != null) {
                checkPermissionToContinue("");
            } else {
                if (saveData(false))
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkPermissionToContinue("");
                        }
                    }, 2000);
//            } else {
//                checkPermissionToContinue(file.getAbsolutePath());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermissionToContinue(String filePath) {
        if (ContextCompat.checkSelfPermission(InvoiceBaseActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                InvoiceBaseActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            if (!filePath.equals("")) {
                getDefaultShareIntent(filePath);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            } else
                callCreatePDF();
        } else {
            Toast.makeText(InvoiceBaseActivity.this, "Requesting permission to create PDF", Toast.LENGTH_LONG).show();
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_WRITE) {
            boolean isGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                }
            }
            if (isGranted) {
                callCreatePDF();
            } else
                Toast.makeText(InvoiceBaseActivity.this, "Permission denied to continue", Toast.LENGTH_LONG).show();
        }
    }

    private Intent getDefaultShareIntent(String filePath) {
        sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Invoice Share");
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            File pdf = new File(filePath);
            Uri fileUri = FileProvider.getUriForFile(InvoiceBaseActivity.this,
                    "lk.sabri.inventory.fileprovider", pdf);
            sharingIntent.setDataAndType(fileUri, getContentResolver().getType(fileUri));
            sharingIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        sharingIntent.setType("*/*");

        return sharingIntent;
    }

    protected abstract void callCreatePDF();

    protected abstract boolean saveData(boolean close);

    protected void printInvoiceReceipt(String invoiceNo, Date saleDate, Customer customerObj, List<InvoiceItem> items, List<Payment> paymentList, double totalValue) {
        getPrinterInstance().beginTransactionPrint();

        StringBuilder dottedLine = new StringBuilder();
        for (int i = 0; i < PRINTER_CHAR_LENGTH_SIZE_1; i++) {
            dottedLine.append(".");
        }

        getPrinterInstance().printText(dottedLine.toString() + "\n\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText("Evergreen Lanka\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_BOLD, 3);
        getPrinterInstance().printText("Kumbukulawa,Polpithigama\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("evergreenlanka@yahoo.com\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("www.evergreenlanka.lk\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("Tel: 0703 914 219 / 0372 273 107\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(":::::: SALES INVOICE ::::::\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 2);

        getPrinterInstance().printText("Invoice No " + invoiceNo + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(getBasicDetails(customerObj, saleDate) + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(dottedLine.toString(), BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(getTitleString() + "\n\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_BOLD, 1);

        if (items != null) {
            for (InvoiceItem item : items) {
                if (item != null) {
                    getPrinterInstance().printText(getItemRowString(item), BixolonPrinter.ALIGNMENT_LEFT,
                            BixolonPrinter.ATTRIBUTE_NORMAL, 1);
                }
            }
        }

        //separator
        getPrinterInstance().printText(dottedLine.toString() + "\n\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        //printing sub total
        StringBuilder builderTotal = new StringBuilder();
        String total = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue);
        builderTotal.append("Sub Total");
        int spaces = PRINTER_CHAR_LENGTH_SIZE_2 - (builderTotal.length() + total.length());
        for (int i = 0; i < spaces; i++)
            builderTotal.append(" ");
        builderTotal.append(total);
        getPrinterInstance().printText(builderTotal.toString() + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_BOLD, 2);

        double payments = 0;
        if (paymentList != null) {
            for (Payment item : paymentList) {
                if (item != null) {
                    payments += item.getAmount();
                    getPrinterInstance().printText(getPaymentRowString(item), BixolonPrinter.ALIGNMENT_LEFT,
                            BixolonPrinter.ATTRIBUTE_NORMAL, 1);
                }
            }
        }

        //printing balance
        if (payments > 0) {
            StringBuilder builderBalance = new StringBuilder();
            String balance = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue - payments);
            builderBalance.append("Balance");
            spaces = PRINTER_CHAR_LENGTH_SIZE_2 - (builderBalance.length() + total.length());
            for (int i = 0; i < spaces; i++)
                builderBalance.append(" ");
            builderBalance.append(balance);
            getPrinterInstance().printText(builderBalance.toString() + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_BOLD, 2);
        }

        //separator
        getPrinterInstance().printText(dottedLine.toString() + "\n\n\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        //footer
        getPrinterInstance().printText("*** Thank you for your purchase! ***" + "\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_BOLD, 1);
        getPrinterInstance().printText("Feel free to visit www.evergreenlanka.lk" + "\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(dottedLine.toString(), BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("\n\n\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_BOLD, 1);

        getPrinterInstance().endTransactionPrint();
    }

    private void printInvoice(String invoiceNo, Date saleDate, Customer customerObj, List<InvoiceItem> items, List<Payment> paymentList, double totalValue) {
        String prints = "";
        for (int i = 0; i < 48; i++) {
            prints += ".";
        }

        prints += "\n\n";

        prints += "[C]Evergreen Lanka\n";
        prints += "[C]Kumbukulawa,Polpithigama\n";
        prints += "[C]evergreenlanka@yahoo.com\n";
        prints += "[C]www.evergreenlanka.lk\n";
        prints += "[C]Tel: 0703 914 219 / 0372 273 107\n\n";
        prints += "[C]:::::: SALES INVOICE ::::::\n\n";

        prints += "[L]Invoice No " + invoiceNo + "\n";

        prints += getCustomerDetails(customerObj, saleDate);

        for (int i = 0; i < 48; i++) {
            prints += ".";
        }

        prints += "\n";
        prints += "[L]Item[R]Qty[R]Price\n\n";

        for (InvoiceItem item : items) {
            if (item.getItemName() == null || item.getItemId() == 0)
                continue;
            int max = Math.min(item.getItemName().length(), 18);
            double total = item.getQuantity() * item.getUnitPrice();
//            prints += "[L]" +item.getItemName().substring(0,max) +"[R]"+String.format(Locale.getDefault(),"%.2f",item.getUnitPrice())
//                    + "x"+String.format(Locale.getDefault(),"%d",item.getQuantity())+"[R]" + String.format(Locale.getDefault(),"%.2f",total)+"\n";
            prints += getfromatedItemRow(item);
        }

        for (int i = 0; i < 48; i++) {
            prints += ".";
        }

        prints += "\n\n";

        prints += "[L]Sub total[R]" + String.format(Locale.getDefault(), "%.2f", totalValue) + "\n";

        double payments = 0;
        if (paymentList != null) {
            for (Payment item : paymentList) {
                if (item != null) {
                    payments += item.getAmount();

                    String itemName = item.getMethod().equals(PaymentMethodAnnotation.CHEQUE) ?
                            String.format(Locale.getDefault(), PaymentMethodAnnotation.CHEQUE + " %s", item.getDetails()) : item.getMethod();

                    String price = String.format(Locale.getDefault(), "%,.2f", item.getAmount());
                    Calendar cal = Calendar.getInstance();

                    cal.setTime(item.getDate());
                    String date = DateFormat.format("dd-MM-yyyy", cal).toString();

                    prints += "[L]" + itemName + "[C]" + date + "[R]" + price + "\n";
                }
            }
        }

        //printing balance
        if (payments > 0) {

            String balance = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue - payments);
            prints += "[L]Balance[R]" + balance + "\n\n";

        }

        //separator
        for (int i = 0; i < 48; i++) {
            prints += ".";
        }

        prints += "\n\n";

        //footer
        prints += "[C]*** Thank you for your purchase! ***\n";
        prints += "[C]Feel free to visit www.evergreenlanka.lk\n";

        for (int i = 0; i < 48; i++) {
            prints += ".";
        }


        PosPrinter posPrinter = new PosPrinter(new WeakReference<>(this));
        posPrinter.print(selectedDevice, prints, new PosPrinter.OnPrinterListener() {
            @Override
            public void onComplete() {
                onPrintingInvoices();
            }

            @Override
            public void onError() {

            }
        });

    }

    private String getfromatedItemRow(InvoiceItem item) {
        final int max = 18;
        double total = item.getQuantity() * item.getUnitPrice();
        if (item.getItemName().length() > max) {
            int remain = item.getItemName().length() - 18;
            return "[L]" + item.getItemName().substring(0, max) + "[R]" + String.format(Locale.getDefault(), "%.2f", item.getUnitPrice())
                    + "x" + String.format(Locale.getDefault(), "%d", item.getQuantity()) + "[R]" + String.format(Locale.getDefault(), "%.2f", total) + "\n"
                    + "[L]" + item.getItemName().substring(max, Math.min((remain + max), item.getItemName().length() - 1)).trim() + "\n";
        } else {
            return "[L]" + item.getItemName() + "[R]" + String.format(Locale.getDefault(), "%.2f", item.getUnitPrice())
                    + "x" + String.format(Locale.getDefault(), "%d", item.getQuantity()) + "[R]" + String.format(Locale.getDefault(), "%.2f", total) + "\n";
        }
    }

    private String getCustomerDetails(Customer customerObj, Date saleDate) {
        String date = TimeFormatter.getFormattedDate("yyyy-MM-dd", saleDate);
        String data = "[L]Date : " + date + "\n";
        data += "[L]Customer : " + customerObj.getCustName() + "\n";
//        StringBuilder builder = new StringBuilder();
//        builder.append("Date : ");
//        builder.append(date);
//        if (customerObj != null) {
//            String customer = "Customer : " + customerObj.getCustName();
//            int spaces = 40 - (builder.length() + customer.length());
//            for (int i = 0; i < spaces; i++)
//                builder.append(" ");
//            builder.append(customer);
//        }
        return data;
    }

    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice(String invoiceNo, Date saleDate, Customer customerObj, List<InvoiceItem> items, List<Payment> paymentList, double totalValue) {

        final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

        if (bluetoothDevicesList != null) {
            final String[] dev = new String[bluetoothDevicesList.length + 1];
            dev[0] = "Default printer";
            int i = 0;
            for (BluetoothConnection device : bluetoothDevicesList) {
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
                dev[++i] = device.getDevice().getName();
                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setTitle("Bluetooth printer selection");
                alertDialog.setItems(
                        dev,
                        (dialogInterface, i1) -> {
                            int index = i1 - 1;
                            if (index == -1) {
                                selectedDevice = null;
                            } else {
                                selectedDevice = bluetoothDevicesList[index];
                                printInvoice(invoiceNo, saleDate, customerObj, items, paymentList, totalValue);
                            }
                            //  Button button = (Button) findViewById(R.id.button_bluetooth_browse);
                            //  button.setText(items[i1]);
                        }
                );

                AlertDialog alert = alertDialog.create();
                alert.setCanceledOnTouchOutside(false);
                alert.show();
            }

    }

    protected void printReceipts(String invoiceNo, Date saleDate, Customer customerObj, List<Payment> items, double totalValue){
        String prints = "";
        for (int i = 0; i < 48; i++) {
            prints += ".";
        }

        prints += "\n\n";

        prints += "[C]Evergreen Lanka\n";
        prints += "[C]Kumbukulawa,Polpithigama\n";
        prints += "[C]evergreenlanka@yahoo.com\n";
        prints += "[C]www.evergreenlanka.lk\n";
        prints += "[C]Tel: 0703 914 219 / 0372 273 107\n\n";
        prints += "[C]:::::: SALES INVOICE ::::::\n\n";

        prints += "[L]Invoice No " + invoiceNo + "\n";

        prints += getCustomerDetails(customerObj, saleDate);

        for (int i = 0; i < 48; i++) {
            prints += ".";
        }

        prints += "\n\n";
        String total = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue);

        prints+="[L]Sub total[R]"+total+"\n";


        double payments = 0;
        if (items != null) {
            for (Payment item : items) {
                if (item != null) {
                    payments += item.getAmount();

                    String itemName = item.getMethod().equals(PaymentMethodAnnotation.CHEQUE) ?
                            String.format(Locale.getDefault(), PaymentMethodAnnotation.CHEQUE + " %s", item.getDetails()) : item.getMethod();

                    String price = String.format(Locale.getDefault(), "%,.2f", item.getAmount());
                    Calendar cal = Calendar.getInstance();

                    cal.setTime(item.getDate());
                    String date = DateFormat.format("dd-MM-yyyy", cal).toString();

                    prints += "[L]"+itemName + "[C]"+date+"[R]"+price+"\n";
                }
            }
        }

        //printing balance
        if (payments > 0) {
            String balance = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue - payments);
            prints += "[L]Balance[R]"+balance+"\n\n";

        }

        //separator
        for (int i = 0; i < 48; i++) {
            prints += ".";
        }

        prints += "\n\n";

        //footer
        prints += "[C]*** Thank you for your purchase! ***\n";
        prints += "[C]Feel free to visit www.evergreenlanka.lk\n";

        for (int i = 0; i < 48; i++) {
            prints += ".";
        }


        PosPrinter posPrinter = new PosPrinter(new WeakReference<>(this));
        posPrinter.print(selectedDevice, prints, new PosPrinter.OnPrinterListener() {
            @Override
            public void onComplete() {
                onPrintingInvoices();
            }

            @Override
            public void onError() {
               // onPrintingInvoices();
            }
        });
    }

    protected void printPaymentReceipt(String invoiceNo, Date saleDate, Customer customerObj, List<Payment> items, double totalValue) {
        getPrinterInstance().beginTransactionPrint();

        StringBuilder dottedLine = new StringBuilder();
        for (int i = 0; i < PRINTER_CHAR_LENGTH_SIZE_1; i++) {
            dottedLine.append(".");
        }

        getPrinterInstance().printText(dottedLine.toString() + "\n\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText("Evergreen Lanka\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_BOLD, 3);
        getPrinterInstance().printText("Kumbukulawa,Polpithigama\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("evergreenlanka@yahoo.com\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("www.evergreenlanka.lk\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("Tel: 0703 914 219 / 0372 273 107  \n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(":::::: SALES INVOICE ::::::\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 2);

        getPrinterInstance().printText("Invoice No " + invoiceNo + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(getBasicDetails(customerObj, saleDate) + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        //separator
        getPrinterInstance().printText(dottedLine.toString() + "\n\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        //printing sub total
        StringBuilder builderTotal = new StringBuilder();
        String total = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue);
        builderTotal.append("Sub Total");
        int spaces = PRINTER_CHAR_LENGTH_SIZE_2 - (builderTotal.length() + total.length());
        for (int i = 0; i < spaces; i++)
            builderTotal.append(" ");
        builderTotal.append(total);
        getPrinterInstance().printText(builderTotal.toString() + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_BOLD, 2);

        double payments = 0;
        if (items != null) {
            for (Payment item : items) {
                if (item != null) {
                    payments += item.getAmount();
                    getPrinterInstance().printText(getPaymentRowString(item), BixolonPrinter.ALIGNMENT_LEFT,
                            BixolonPrinter.ATTRIBUTE_NORMAL, 1);
                }
            }
        }

        //printing balance
        if (payments > 0) {
            StringBuilder builderBalance = new StringBuilder();
            String balance = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue - payments);
            builderBalance.append("Balance");
            spaces = PRINTER_CHAR_LENGTH_SIZE_2 - (builderBalance.length() + total.length());
            for (int i = 0; i < spaces; i++)
                builderBalance.append(" ");
            builderBalance.append(balance);
            getPrinterInstance().printText(builderBalance.toString() + "\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_BOLD, 2);
        }

        //separator
        getPrinterInstance().printText(dottedLine.toString() + "\n\n\n", BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        //footer
        getPrinterInstance().printText("*** Thank you for your purchase! ***" + "\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_BOLD, 1);
        getPrinterInstance().printText("Feel free to visit www.evergreenlanka.lk" + "\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_NORMAL, 1);

        getPrinterInstance().printText(dottedLine.toString(), BixolonPrinter.ALIGNMENT_LEFT, BixolonPrinter.ATTRIBUTE_NORMAL, 1);
        getPrinterInstance().printText("\n\n\n\n", BixolonPrinter.ALIGNMENT_CENTER, BixolonPrinter.ATTRIBUTE_BOLD, 1);

        getPrinterInstance().endTransactionPrint();
    }

    private String createHtmlCode(String invoiceNo, Date saleDate, Customer customerObj, List<InvoiceItem> items, List<Payment> payments, double totalValue) {

        StringBuilder builder = new StringBuilder();
        builder.append(AppUtil.PDF_HTML_HEAD);

        String date = TimeFormatter.getFormattedDate(TimeFormatter.DATE_FORMAT_DEFAULT, saleDate);
        builder.append(String.format(Locale.getDefault(), AppUtil.PDF_HTML_TOPIC, invoiceNo, date, customerObj.getCustName()));

        builder.append(AppUtil.PDF_HTML_BODY_TITLE);

        for (InvoiceItem item : items) {
            if (item != null) {
                double itemPrice = item.getQuantity() * item.getUnitPrice();

                String quantity = String.format(Locale.getDefault(), "%d", item.getQuantity());
                String unitPrice = String.format(Locale.getDefault(), "%,.2f", item.getUnitPrice());
                String totalTxt = String.format(Locale.getDefault(), "%,.2f", itemPrice);

                builder.append(String.format(Locale.getDefault(), AppUtil.PDF_HTML_BODY_ITEM, item.getItem().getItemName(), quantity, unitPrice, totalTxt));
            }
        }

        String formattedTotal = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue);
        builder.append(String.format(Locale.getDefault(), AppUtil.PDF_HTML_TOTAL, "", formattedTotal));

        double totalPayments = 0;
        if (payments.size() > 0)
            builder.append(AppUtil.PDF_HTML_TABLE_START);
        for (Payment payment : payments) {
            if (payment != null) {
                totalPayments += payment.getAmount();
                String dateString = TimeFormatter.getFormattedDate(TimeFormatter.DATE_FORMAT_DEFAULT, payment.getDate());
                String amount = String.format(Locale.getDefault(), "%,.2f", payment.getAmount());
                builder.append(String.format(Locale.getDefault(), AppUtil.PDF_HTML_PAY_ITEM, payment.getMethod(),
                        dateString, amount));
            }
        }
        if (totalPayments > 0) {
            builder.append(AppUtil.PDF_HTML_TABLE_END);
            String formattedPayment = String.format(Locale.getDefault(), "Rs. %,.2f", totalValue - totalPayments);
            builder.append(String.format(Locale.getDefault(), AppUtil.PDF_HTML_PAY_FOOTER, formattedPayment));
        }

        builder.append(AppUtil.PDF_HTML_FOOTER);

        return builder.toString();
    }

    protected void createPDFFile(String invoiceNo, Date saleDate, Customer customerObj, List<InvoiceItem> items, List<Payment> payments, double totalValue) {

        //Create folder if not exits
        File f = new File(Environment.getExternalStorageDirectory(), FOLDER_MAIN);
        if (!f.exists()) {
            if (f.mkdirs())
                Toast.makeText(InvoiceBaseActivity.this, "New Folder created", Toast.LENGTH_LONG).show();
        }

        new CreatePdf(this)
                .setPdfName(invoiceNo.substring(1))
                .openPrintDialog(false)
                .setContentBaseUrl(null)
                .setPageSize(PrintAttributes.MediaSize.ISO_A4)
                .setContent(createHtmlCode(invoiceNo, saleDate, customerObj, items, payments, totalValue))
                .setFilePath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + FOLDER_MAIN)
                .setCallbackListener(new CreatePdf.PdfCallbackListener() {
                    @Override
                    public void onFailure(@NotNull String s) {
                        Toast.makeText(InvoiceBaseActivity.this, "Failed to create the PDF file", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(@NotNull String s) {
                        Toast.makeText(InvoiceBaseActivity.this, "Successfully created the PDF file.", Toast.LENGTH_LONG).show();
                        File file = new File(Environment.getExternalStorageDirectory(), FOLDER_MAIN + "/" + invoiceId + ".pdf");
                        getDefaultShareIntent(file.getAbsolutePath());
                        startActivity(Intent.createChooser(sharingIntent, "Share via"));
                    }
                })
                .create();
    }

    private String getTitleString() {
        StringBuilder builderTitle = new StringBuilder();
        int sections = PRINTER_CHAR_LENGTH_SIZE_1 / 11;

        printWithAlignment(builderTitle, "Items", sections * 5, Alignment.LEFT);

        printWithAlignment(builderTitle, "Qty", sections * 2, Alignment.CENTER);

        printWithAlignment(builderTitle, "Price", sections * 2, Alignment.CENTER);

        printWithAlignment(builderTitle, "Total", PRINTER_CHAR_LENGTH_SIZE_1 - builderTitle.length(), Alignment.RIGHT);

        return builderTitle.toString();
    }

    private String getBasicDetails(Customer customerObj, Date saleDate) {
        String date = TimeFormatter.getFormattedDate("yyyy-MM-dd", saleDate);
        StringBuilder builder = new StringBuilder();
        builder.append("Date : ");
        builder.append(date);
        if (customerObj != null) {
            String customer = "Customer : " + customerObj.getCustName();
            int spaces = PRINTER_CHAR_LENGTH_SIZE_1 - (builder.length() + customer.length());
            for (int i = 0; i < spaces; i++)
                builder.append(" ");
            builder.append(customer);
        }
        return builder.toString();
    }

    private String getItemRowString(InvoiceItem item) {
        StringBuilder builderTitle = new StringBuilder();
        double totalPrice = item.getUnitPrice() * item.getQuantity();
        String price = String.format(Locale.getDefault(), "%,.2f\n", totalPrice);
        String quantity = String.format(Locale.getDefault(), "%d", item.getQuantity());
        String unitPrice = String.format(Locale.getDefault(), "%.2f", item.getUnitPrice());
        int sectionSize = PRINTER_CHAR_LENGTH_SIZE_1 / 11;

        if (item.getItemName() == null)
            item.setItemName("Unknown Item");

        int nameLength = item.getItemName().length();

        if (item.getItemName().length() > sectionSize * 5)
            nameLength = sectionSize * 5;

        String itemName = item.getItemName().substring(0, nameLength);
        builderTitle.append(itemName);
        int balance = sectionSize * 5 - itemName.length();
        for (int i = 0; i < balance; i++)
            builderTitle.append(" ");

        balance = sectionSize * 2 - quantity.length();
        for (int i = 0; i < balance / 2; i++)
            builderTitle.append(" ");
        builderTitle.append(quantity);
        for (int i = 0; i < (sectionSize * 2) - ((balance / 2) + quantity.length()); i++)
            builderTitle.append(" ");

        balance = sectionSize * 2 - unitPrice.length();
        for (int i = 0; i < (sectionSize * 2) - ((balance / 2) + unitPrice.length()); i++)
            builderTitle.append(" ");
        builderTitle.append(unitPrice);
        for (int i = 0; i < balance / 2; i++)
            builderTitle.append(" ");

        //if have any remainder
        int remainder = PRINTER_CHAR_LENGTH_SIZE_1 - (builderTitle.length() + price.length());
        if (remainder > 0)
            for (int i = 0; i < remainder; i++)
                builderTitle.append(" ");

        builderTitle.append(price);

        return builderTitle.toString();
    }

    private String getPaymentRowString(Payment item) {
        StringBuilder builderTitle = new StringBuilder();
        String itemName = item.getMethod().equals(PaymentMethodAnnotation.CHEQUE) ?
                String.format(Locale.getDefault(), PaymentMethodAnnotation.CHEQUE + " %s", item.getDetails()) : item.getMethod();
        builderTitle.append(itemName);

        String price = String.format(Locale.getDefault(), "%,.2f\n", item.getAmount());

        int sectionSize = PRINTER_CHAR_LENGTH_SIZE_1 / 11;
        int balance = sectionSize * 5 - itemName.length();
        for (int i = 0; i < balance; i++)
            builderTitle.append(" ");

        Calendar cal = Calendar.getInstance();
        cal.setTime(item.getDate());
        String date = DateFormat.format("dd-MM-yyyy", cal).toString();

        balance = sectionSize * 2 - date.length();
        for (int i = 0; i < balance / 2; i++)
            builderTitle.append(" ");
        builderTitle.append(date);
        for (int i = 0; i < (sectionSize * 2) - ((balance / 2) + date.length()); i++)
            builderTitle.append(" ");

        //if have any remainder
        int remainder = PRINTER_CHAR_LENGTH_SIZE_1 - (builderTitle.length() + price.length());
        if (remainder > 0)
            for (int i = 0; i < remainder; i++)
                builderTitle.append(" ");

        builderTitle.append(price);

        return builderTitle.toString();
    }

    private void printWithAlignment(StringBuilder builder, String value, int sectionSize, Alignment alignment) {
        int balance = sectionSize - value.length();
        int sideSpace;

        if (alignment == Alignment.CENTER)
            sideSpace = balance / 2;
        else if (alignment == Alignment.RIGHT)
            sideSpace = balance;
        else
            sideSpace = 0;

        for (int i = 0; i < sideSpace; i++)
            builder.append(" ");
        builder.append(value);
        for (int i = 0; i < sectionSize - (sideSpace + value.length()); i++)
            builder.append(" ");
    }

    public static BixolonPrinter getPrinterInstance() {
        return bixolonPrinter;
    }

    enum Alignment {
        LEFT, CENTER, RIGHT
    }

    public void proceedToPrint(String invoiceNo, Date saleDate, Customer customerObj,
                               List<InvoiceItem> items, List<Payment> paymentList, double totalValue) {
        if (paymentList == null || paymentList.size() == 0) {
            browseBluetoothDevice(invoiceNo, saleDate, customerObj, items, paymentList, totalValue);
          //  printInvoiceReceipt(invoiceNo, saleDate, customerObj, items, paymentList, totalValue);
        } else
            showPrintDialog(invoiceNo, saleDate, customerObj, items, paymentList, totalValue);


    }

    private void showPrintDialog(final String invoiceNo, final Date saleDate, final Customer customerObj,
                                 final List<InvoiceItem> items, final List<Payment> paymentList, final double totalValue) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_print_alert);
        dialog.setCancelable(true);

        final Window window = dialog.getWindow();
        window.setLayout(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.BOTTOM;
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.5f);
        window.setAttributes(wlp);

        dialog.findViewById(R.id.btn_print_invoice_alert).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printInvoice(invoiceNo, saleDate, customerObj, items, paymentList, totalValue);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.btn_print_balance_alert).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printReceipts(invoiceNo, saleDate, customerObj, paymentList, totalValue);
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
