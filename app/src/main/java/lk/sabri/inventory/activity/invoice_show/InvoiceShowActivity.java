package lk.sabri.inventory.activity.invoice_show;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import lk.sabri.inventory.R;
import lk.sabri.inventory.activity.InvoiceBaseActivity;
import lk.sabri.inventory.activity.PrinterConnectActivity;
import lk.sabri.inventory.adapter.OnItemClickListener;
import lk.sabri.inventory.adapter.item.InvoiceItemAdapter;
import lk.sabri.inventory.data.InventoryDatabase;
import lk.sabri.inventory.data.Invoice;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.Item;
import lk.sabri.inventory.data.Payment;
import lk.sabri.inventory.data.PaymentDAO;
import lk.sabri.inventory.data.PaymentMethodAnnotation;
import lk.sabri.inventory.util.AppUtil;
import lk.sabri.inventory.util.BixolonPrinter;
import lk.sabri.inventory.util.Constants;
import lk.sabri.inventory.util.TimeFormatter;

public class InvoiceShowActivity extends InvoiceBaseActivity {

    //instances
    private InvoiceShowViewModel invoiceViewModel;
    private InvoiceItemAdapter adapter;

    //views
    private RecyclerView recyclerInvoiceItems;
    private View paymentBottomSheet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        invoiceViewModel = new ViewModelProvider(this).get(InvoiceShowViewModel.class);

        setContentView(R.layout.activity_invoice_view);

        paymentBottomSheet = findViewById(R.id.bottonsheet_payment);

        readExtras();
        setView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return onCustomCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PRINTER_OPEN && resultCode == Activity.RESULT_OK) {
            final Invoice invoice = invoiceViewModel.getInvoice().getValue();

            if (invoice != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PaymentDAO paymentDAO = InventoryDatabase.getInstance(InvoiceShowActivity.this).paymentDAO();
                        final List<Payment> payments = paymentDAO.getPaymentsForInvoice(String.valueOf(invoice.getInvoiceNo()));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                proceedToPrint(invoice.getInvoiceNo(), invoice.getDate(), invoice.getCustomer(),
                                        invoice.getItems(), payments != null ? payments : new ArrayList<Payment>(), invoice.getTotal());
                            }
                        });
                    }
                }).start();
            }
        }
    }

    @Override
    protected void callCreatePDF() {
        Invoice invoice = invoiceViewModel.getInvoice().getValue();
        if (invoice != null)
            createPDFFile(invoice.getInvoiceNo(), invoice.getDate(), invoice.getCustomer(), invoice.getItems(), invoice.getTotal());
    }

    protected boolean saveData(boolean close) {
        return true;
    }

    private void readExtras() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle extra = getIntent().getExtras();
            Invoice invoice = (Invoice) extra.get(Constants.EXTRA_INVOICE_INFO);

            invoiceViewModel.setInvoice(invoice);
            if (invoice != null)
                this.invoiceId = invoice.getInvoiceNo().substring(1);
        }
    }

    private void setView() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final TextView txtInvoiceId = findViewById(R.id.text_invoice_no);
        final TextView txtInvoiceDate = findViewById(R.id.text_invoice_date);
        final TextView txtInvoiceTo = findViewById(R.id.text_invoice_to);
        recyclerInvoiceItems = findViewById(R.id.recycler_invoice_items);

        Invoice invoice = invoiceViewModel.getInvoice().getValue();
        if (invoice != null) {
            loadData(invoice);
        }

        invoiceViewModel.getInvoice().observe(this, new Observer<Invoice>() {
            @Override
            public void onChanged(Invoice invoice) {
                txtInvoiceId.setText(invoice.getInvoiceNo());
                if (actionBar != null)
                    actionBar.setTitle(String.format(Locale.getDefault(), "Invoice %s", invoice.getInvoiceNo()));
                txtInvoiceDate.setText(TimeFormatter.getFormattedDate("yyyy-MM-dd", invoice.getDate()));
                if (invoice.getCustomer() != null)
                    txtInvoiceTo.setText(invoice.getCustomer().getCustName());
                else
                    txtInvoiceTo.setText(R.string.unknown);
                if (adapter != null)
                    adapter.notifyDataSetChanged();
            }
        });

        findViewById(R.id.btn_invoice_print).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bixolonPrinter = new BixolonPrinter(InvoiceShowActivity.this);

                Intent intent = new Intent(getApplicationContext(), PrinterConnectActivity.class);
                startActivityForResult(intent, REQUEST_PRINTER_OPEN);
            }
        });
    }

    private void loadData(final Invoice invoice) {
        final InventoryDatabase database = InventoryDatabase.getInstance(this);

        new Thread(new Runnable() {
            @Override
            public void run() {

                final List<InvoiceItem> items = database.invoiceItemDAO().loadAllByIds(invoice.getId());
                for (InvoiceItem invoiceItem : items) {
                    Item item = database.itemDAO().loadAllById(invoiceItem.getItemId());
                    invoiceItem.setItem(item);
                }
//                items.add(null);

                invoice.setItems(items);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invoiceViewModel.getInvoice().setValue(invoice);
                        setAdapter(items);
                    }
                });
            }
        }).start();
    }

    private void showPaymentBottomSheet() {
        final Spinner spnPaymentType = paymentBottomSheet.findViewById(R.id.spn_payment_type);
        final EditText edtChequeNo = paymentBottomSheet.findViewById(R.id.edt_payment_cheque_no);
        final TextView txtChequeNo = paymentBottomSheet.findViewById(R.id.txt_payment_cheque_no);
        final EditText edtAmount = paymentBottomSheet.findViewById(R.id.edt_payment_amount);
        Button btnPay = paymentBottomSheet.findViewById(R.id.btn_payment_proceed);

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(paymentBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setSkipCollapsed(true);

        paymentBottomSheet.post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) paymentBottomSheet.getLayoutParams();
                params.height = (int) (AppUtil.getDeviceHeight(InvoiceShowActivity.this) * Constants.BOTTOM_SHEET_HEIGHT);
                paymentBottomSheet.setLayoutParams(params);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_row, Constants.PAYMENT_TYPES);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnPaymentType.setAdapter(arrayAdapter);
        spnPaymentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (Constants.PAYMENT_TYPES[i].equals(PaymentMethodAnnotation.CHEQUE)) {
                    edtChequeNo.setVisibility(View.VISIBLE);
                    txtChequeNo.setVisibility(View.VISIBLE);
                } else {
                    edtChequeNo.setVisibility(View.GONE);
                    txtChequeNo.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String amount = edtAmount.getText().toString();
                try {
                    if (!amount.equals("")) {
                        showSaveDataDialog(Constants.PAYMENT_TYPES[spnPaymentType.getSelectedItemPosition()], edtChequeNo.getText().toString(), Double.parseDouble(amount));
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    } else
                        Toast.makeText(InvoiceShowActivity.this, "Please enter valid amount", Toast.LENGTH_LONG).show();
                } catch (NumberFormatException ex) {
                    Toast.makeText(InvoiceShowActivity.this, "Please enter valid amount", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void addToPaymentDB(@PaymentMethodAnnotation.Method String method, String details, double amount) {
        final Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setDetails(details);
        payment.setDate(Calendar.getInstance().getTime());
        payment.setInvoiceId(invoiceId);

        new Thread(new Runnable() {
            @Override
            public void run() {
                PaymentDAO paymentDAO = InventoryDatabase.getInstance(InvoiceShowActivity.this).paymentDAO();
                paymentDAO.insertAll(payment);
            }
        }).start();
    }

    private void showSaveDataDialog(final String paymentType, final String chequeNo, final double amount) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.payment);
        builder.setMessage(R.string.save_message_payment);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                addToPaymentDB(paymentType, chequeNo, amount);
                showPaymentInfo(paymentType, chequeNo, amount);
            }
        });
        builder.setNegativeButton(R.string.dont_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.show();
    }

    private void showPaymentInfo(String paymentType, String chequeNo, double amount) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoiceId(InvoiceItemAdapter.VIEW_TYPE_PAYMENT_AMOUNT);
        if (paymentType.equals(PaymentMethodAnnotation.CHEQUE))
            paymentType = paymentType.concat(" " + chequeNo);
        item.setItemName(paymentType);
        item.setPrice(Calendar.getInstance().getTimeInMillis());
        item.setQuantity(1);
        item.setUnitPrice(amount);

        if (invoiceViewModel.getInvoice().getValue() != null && invoiceViewModel.getInvoice().getValue().getItems() != null) {
            int pos = getItemPosition(InvoiceItemAdapter.VIEW_TYPE_PAYMENT_AMOUNT);
            if (pos < 0) {
                pos = getItemPosition(InvoiceItemAdapter.VIEW_TYPE_PAYMENT);
                invoiceViewModel.getInvoice().getValue().getItems().add(pos, item);
                adapter.notifyItemInserted(pos);

                InvoiceItem balanceItem = new InvoiceItem(); //balance row
                balanceItem.setInvoiceId(InvoiceItemAdapter.VIEW_TYPE_BALANCE);
                invoiceViewModel.getInvoice().getValue().getItems().add(invoiceViewModel.getInvoice().getValue().getItems().size() - 1, balanceItem);
                adapter.notifyItemInserted(invoiceViewModel.getInvoice().getValue().getItems().size() - 1);
            } else {
                invoiceViewModel.getInvoice().getValue().getItems().add(pos + 1, item);
                adapter.notifyItemInserted(pos + 1);
                adapter.notifyItemChanged(invoiceViewModel.getInvoice().getValue().getItems().size() - 2); //balance item value reset
            }
        }
    }

    private int getItemPosition(int type) {
        int i = 0;

        if (invoiceViewModel.getInvoice().getValue() != null && invoiceViewModel.getInvoice().getValue().getItems() != null)
            for (InvoiceItem invoiceItem : invoiceViewModel.getInvoice().getValue().getItems()) {
                if (invoiceItem.getInvoiceId() == type) {
                    return i;
                }
                i++;
            }
        return -1;
    }

    private void setAdapter(List<InvoiceItem> items) {
        adapter = new InvoiceItemAdapter(this, items);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerInvoiceItems.setLayoutManager(layoutManager);
        recyclerInvoiceItems.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(Object object) {
                if (object instanceof Double)
                    showPaymentBottomSheet();
            }
        });
//        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerInvoiceItems.getContext(),
//                layoutManager.getOrientation());
//        recyclerInvoiceItems.addItemDecoration(dividerItemDecoration);
    }
}
