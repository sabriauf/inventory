package lk.sabri.inventory.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.print.PrintAttributes;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.uttampanchasara.pdfgenerator.CreatePdf;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.sabri.inventory.R;
import lk.sabri.inventory.data.Customer;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.Payment;
import lk.sabri.inventory.data.PaymentMethodAnnotation;
import lk.sabri.inventory.util.AppUtil;
import lk.sabri.inventory.util.BixolonPrinter;
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_item_share) {

            File file = new File(Environment.getExternalStorageDirectory(), FOLDER_MAIN + "/" + invoiceId + ".pdf");
            final boolean fileExits = file.exists();

            if (!fileExits && invoiceId != null) {
                checkPermissionToContinue("");
            } else if (invoiceId == null) {
                if (saveData(false))
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkPermissionToContinue("");
                        }
                    }, 2000);
            } else {
                checkPermissionToContinue(file.getAbsolutePath());
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
        if (items != null) {
            for (Payment item : paymentList) {
                if (item != null) {
                    payments += item.getAmount();
                    getPrinterInstance().printText(getPaymentRowString(item), BixolonPrinter.ALIGNMENT_LEFT,
                            BixolonPrinter.ATTRIBUTE_NORMAL, 1);
                }
            }
        }

        //printing balance
        if(payments > 0) {
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
        if(payments > 0) {
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

    private String createHtmlCode(String invoiceNo, Date saleDate, Customer customerObj, List<InvoiceItem> items, double totalValue) {

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
        builder.append(AppUtil.PDF_HTML_FOOTER);

        return builder.toString();
    }

    protected void createPDFFile(String invoiceNo, Date saleDate, Customer customerObj, List<InvoiceItem> items, double totalValue) {

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
                .setContent(createHtmlCode(invoiceNo, saleDate, customerObj, items, totalValue))
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
        builderTitle.append(item.getItemName());

        double totalPrice = item.getUnitPrice() * item.getQuantity();
        String price = String.format(Locale.getDefault(), "%,.2f\n", totalPrice);

        String quantity = String.format(Locale.getDefault(), "%d", item.getQuantity());
        String unitPrice = String.format(Locale.getDefault(), "%.2f", item.getUnitPrice());

        int sectionSize = PRINTER_CHAR_LENGTH_SIZE_1 / 11;
        int balance = sectionSize * 5 - item.getItemName().length();
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
}
