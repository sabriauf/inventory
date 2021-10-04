package lk.sabri.inventory.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.net.InetAddress;
import java.util.Locale;

import lk.sabri.inventory.data.LoginData;

public class AppUtil {

    public static int getDeviceHeight(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private static boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            return ipAddr != null && !ipAddr.toString().equals("");

        } catch (Exception e) {
            return false;
        }
    }

    public static void checkInternetConnection(Context context, final OnConnectionCheck listener) {
        final boolean hasNetwork = isNetworkConnected(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (listener != null)
                    listener.onHasConnection(hasNetwork && isInternetAvailable());
            }
        }).start();
    }

    public interface OnConnectionCheck {
        void onHasConnection(boolean hasConnection);
    }

    public static int getNextId(LoginData user) {
        return getNextId(user, 5);
    }

    public static int getNextId(LoginData user, int leadingZeros) {
        int count = Integer.parseInt(user.getLast_invoice_id());
        String format = "%0" + leadingZeros + "d";
        String id = String.format(Locale.getDefault(), format, (count + 1));
        String value = String.format(Locale.getDefault(), "%d%s", user != null ? user.getId() : 99, id);
        return Integer.parseInt(value);
    }

    public static final String PDF_HTML_HEAD = "<!DOCTYPE html><html><head><style>h1 {text-align: " +
            "center;}h3 {text-align: right;}h4 {text-align: center;}th {text-align: left;font-weight: " +
            "normal;}</style><title>Invoice</title></head><body><hr style=\"border-top: dotted 2px;\" " +
            "/><div style=\"text-align:center\"><h2>EVERGREEN LANKA</h2><p>Kumbukulawa,Polpithigama<br>" +
            "evergreenlanka@yahoo.com<br>www.evergreenlanka.lk<br>Tel: 0703 914 219 / 0372 273 107</p></div>" +
            "<div style=\"text-align:center;font-size:20px\">::::::::::::::::::::::::::::::::::: SALES INVOICE :::::::::::::::::::::::::::::::::::</div><br>";
    public static final String PDF_HTML_TOPIC = "<table style=\"width:100%%\"><tr><td style=\"font-size:" +
            "20px\">Invoice No %s</td><td></td></tr><tr><td>Date %s</td><td style=\"text-align:right\">Customer: %s</td></tr></table>";
    public static final String PDF_HTML_BODY_TITLE = "<hr style=\"border-top: dotted 1px;\" /><table " +
            "style=\"width:100%\"><tr ><th style=\"width:30%\"><b>Item</b></th><th style=\"text-align:" +
            "center;width:8%\"><b>Qty</b></th><th style=\"text-align:center;width:8%\"><b>Price</b></th>" +
            "<th style=\"text-align:right;width:12%\"><b>Total</b></th></tr><tr><td colspan=\"4\"></td></tr>";
    public static final String PDF_HTML_BODY_ITEM = "<tr><td>%s</td><td style=\"text-align:center;\">" +
            "%s</td><td style=\"text-align:center;\">%s</td><td style=\"text-align:right\">%s</td></tr>";
    public static final String PDF_HTML_TOTAL = "<tr><td colspan=\"4\"><hr style=\"border-top: " +
            "dotted 1px;\" /></td></tr><tr><td style=\"font-size:20px\">Sub Total</td><td style=\"" +
            "text-align:center;\">%s</td><td colspan=\"2\" style=\"text-align:right;font-size:20px\">%s" +
            "</td></tr></table><hr style=\"border-top: dotted 1px;\" />";
    public static final String PDF_HTML_TABLE_START = "<table style=\"width:100%\"><tr >";
    public static final String PDF_HTML_TABLE_END = "</table>";
    public static final String PDF_HTML_PAY_ITEM = "<th style='width:40%%'>%s</th><th style='text-align:center;width:40%%'>%s</th><th style='text-align:right;width:20%%'>%s</th></tr>";
    public static final String PDF_HTML_PAY_FOOTER = "<table style='width:100%%'><tr><th style='width:50%%;font-size:20px;'><b>Balance</b></th><th style='text-align:right;width:50%%;font-size:20px;'><b>%s</b></th></tr></table>";
    public static final String PDF_HTML_FOOTER = "<br><h4>*** Thank you for your purchase ***</h4>" +
            "<p style=\"text-align:center;font-size:12px\">Feel free to visit www.evergreenlanka.lk</p>" +
            "<hr style=\"border-top: dotted 2px;\" /></body></html>";
}
