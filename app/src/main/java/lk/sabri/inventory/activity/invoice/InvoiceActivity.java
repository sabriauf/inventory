package lk.sabri.inventory.activity.invoice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lk.sabri.inventory.R;
import lk.sabri.inventory.activity.InvoiceBaseActivity;
import lk.sabri.inventory.activity.PrinterConnectActivity;
import lk.sabri.inventory.adapter.OnItemClickListener;
import lk.sabri.inventory.adapter.customer.CustomerAdapter;
import lk.sabri.inventory.adapter.item.InvoiceItemAdapter;
import lk.sabri.inventory.adapter.item.ItemAdapter;
import lk.sabri.inventory.data.Customer;
import lk.sabri.inventory.data.InventoryDatabase;
import lk.sabri.inventory.data.Invoice;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.Item;
import lk.sabri.inventory.data.LoginData;
import lk.sabri.inventory.data.Payment;
import lk.sabri.inventory.data.PaymentDAO;
import lk.sabri.inventory.data.PaymentMethodAnnotation;
import lk.sabri.inventory.ui.CustomDatePickerDialog;
import lk.sabri.inventory.util.AppUtil;
import lk.sabri.inventory.util.BixolonPrinter;
import lk.sabri.inventory.util.Constants;
import lk.sabri.inventory.util.SwipeToDeleteCallback;
import lk.sabri.inventory.util.TimeFormatter;

public class InvoiceActivity extends InvoiceBaseActivity {

    //instances
    protected InvoiceViewModel invoiceViewModel;
    private OnAddItemListener addItemListener;
    private InvoiceItemAdapter adapter;
    private Payment payment;

    //primary data
    private boolean isQtyShown;
    private ProgressBar progressBar;

    //views
    private View qtySheet, paymentBottomSheet;
    private RecyclerView recyclerInvoiceItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        invoiceViewModel = new ViewModelProvider(this).get(InvoiceViewModel.class);

        setContentView(R.layout.activity_invoice);

        setData();
        setView();

        // browseBluetoothDevice();
    }

    @Override
    protected void onPrintingInvoices() {
        //save data to database
        saveData(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finishInvoice();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        finishInvoice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (getPrinterInstance() != null)
            getPrinterInstance().printerClose();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PRINTER_OPEN && resultCode == Activity.RESULT_OK) {

            List<Payment> payments = new ArrayList<>();
            if (payment != null)
                payments.add(payment);

            proceedToPrint(invoiceViewModel.getInvoiceNo().getValue(), invoiceViewModel.getDate().getValue(),
                    invoiceViewModel.getCustomer().getValue(), invoiceViewModel.getItems().getValue(), payments,
                    invoiceViewModel.getTotal().getValue());
            saveData(true);
        }
    }


    private void setData() {
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        invoiceViewModel.setItems(invoiceItems);

        final InventoryDatabase database = InventoryDatabase.getInstance(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Item> items = database.itemDAO().getAll();
                final List<Customer> customers = database.customerDAO().getAll();
                String username = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
                        .getString(Constants.PREFERENCE_LAST_USER, "");
                final LoginData user = database.loginDAO().loadAllByUsername(username);
                final int idCount = database.invoiceDAO().getCount();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invoiceViewModel.setAddItems(items);
                        invoiceViewModel.setCustomerList(customers);
                        invoiceViewModel.setInvoiceNo(String.format(Locale.getDefault(), "#%d",
                                AppUtil.getNextId(user, idCount)));

                        setCustomerBottomSheet();
                        setAddItemBottomSheet();
                    }
                });
            }
        }).start();
    }

    private void setView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        final TextView txtInvoiceId = findViewById(R.id.text_invoice_no);
        final TextView txtInvoiceDate = findViewById(R.id.text_invoice_date);
        recyclerInvoiceItems = findViewById(R.id.recycler_invoice_items);
        qtySheet = findViewById(R.id.bottonsheet_add_quantity);
        paymentBottomSheet = findViewById(R.id.bottonsheet_payment);
        progressBar = findViewById(R.id.progress_invoice);

        adapter = new InvoiceItemAdapter(this, invoiceViewModel.getItems().getValue());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerInvoiceItems.setLayoutManager(layoutManager);
        recyclerInvoiceItems.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(Object object) {
                if (addItemListener != null && object == null)
                    addItemListener.onAddItemClick();
                else if (object instanceof Double)
                    if (isValidData()) {
                        showPaymentBottomSheet();
                    }
            }
        });
//        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerInvoiceItems.getContext(),
//                layoutManager.getOrientation());
//        recyclerInvoiceItems.addItemDecoration(dividerItemDecoration);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerInvoiceItems);

        invoiceViewModel.getInvoiceNo().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String value) {
                txtInvoiceId.setText(value);
            }
        });

        invoiceViewModel.getDate().observe(this, new Observer<Date>() {
            @Override
            public void onChanged(Date date) {
                txtInvoiceDate.setText(TimeFormatter.getFormattedDate("yyyy-MM-dd", date));
            }
        });

        invoiceViewModel.getItems().observe(this, new Observer<List<InvoiceItem>>() {
            @Override
            public void onChanged(List<InvoiceItem> itemList) {
                adapter.notifyDataSetChanged();
            }
        });

        txtInvoiceDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomDatePickerDialog newFragment = new CustomDatePickerDialog(InvoiceActivity.this);
                newFragment.show(getSupportFragmentManager(), "Date Picker");
                newFragment.setOnDateSetListener(new CustomDatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(Date date) {
                        invoiceViewModel.setDate(date);
                    }
                });
            }
        });

        findViewById(R.id.btn_invoice_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showExitDialog();
            }
        });

        findViewById(R.id.btn_invoice_print).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (isValidData()) {
//                    bixolonPrinter = new BixolonPrinter(InvoiceActivity.this);
//                    Intent intent = new Intent(getApplicationContext(), PrinterConnectActivity.class);
//                    startActivityForResult(intent, REQUEST_PRINTER_OPEN);
//                }

                browseBluetoothDevice();
            }
        });
    }

    protected boolean saveData(final boolean close) {
        if (isValidData()) {
            final Invoice temp = invoiceViewModel.getInvoice().getValue();
            progressBar.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InventoryDatabase database = InventoryDatabase.getInstance(InvoiceActivity.this);
                    String username = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
                            .getString(Constants.PREFERENCE_LAST_USER, "");
                    LoginData data = database.loginDAO().loadAllByUsername(username);
                    int idCount = database.invoiceDAO().getCount();

                    if (temp != null) {
                        temp.setId(AppUtil.getNextId(data, idCount));

                        int count = 0;
                        for (InvoiceItem item : temp.getItems()) {
                            count += item.getQuantity();
                        }
                        temp.setItemCount(count);
                        database.invoiceDAO().insertAll(temp);

                        int itemId = database.invoiceItemDAO().getCount();
                        for (InvoiceItem invoiceItem : temp.getItems()) {
                            if (invoiceItem.getInvoiceId() == temp.getId() || invoiceItem.getInvoiceId() == 0) {
                                itemId = itemId + 1;
                                invoiceItem.setId(itemId);
                                invoiceItem.setInvoiceId(temp.getId());
                                database.invoiceItemDAO().insertAll(invoiceItem);
                            }
                        }

                        if (payment != null && payment.getAmount() > 0) {
                            payment.setInvoiceId(temp.getInvoiceNo().substring(1)); //Add payment info to db
                            database.paymentDAO().insertAll(payment);
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);

                            if (temp != null)
                                invoiceId = temp.getInvoiceNo().substring(1);

                            Intent data = new Intent();
                            data.putExtra(Constants.EXTRA_INVOICE_INFO, temp);
                            setResult(Activity.RESULT_OK, data);
                            if (close)
                                finish();
                        }
                    });
                }
            }).start();
            return true;
        }
        return false;
    }

    private boolean isValidData() {
        if (invoiceViewModel.getItems().getValue() != null && invoiceViewModel.getItems()
                .getValue().size() > 1 && invoiceViewModel.getCustomer().getValue() != null) {
            return true;
        } else if (invoiceViewModel.getCustomer().getValue() == null) {
            Toast.makeText(InvoiceActivity.this, "Please select a customer", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(InvoiceActivity.this, "No item selected", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void setCustomerBottomSheet() {
        final View customerSheet = findViewById(R.id.bottonsheet_invoice);
        RecyclerView recyclerCustomers = findViewById(R.id.recycler_customer);
        final EditText edtCustomerSearch = findViewById(R.id.edt_customer_search);
        final TextView txtInvoiceTo = findViewById(R.id.text_invoice_to);

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(customerSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setSkipCollapsed(true);

        final CustomerAdapter cusAdapter = new CustomerAdapter(this, invoiceViewModel
                .getCustomerList().getValue());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerCustomers.setLayoutManager(layoutManager);
        recyclerCustomers.setAdapter(cusAdapter);
        cusAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(Object object) {
                if (object != null) {
                    invoiceViewModel.setCustomer((Customer) object);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    final InventoryDatabase database = InventoryDatabase.getInstance(InvoiceActivity.this);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String username = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME,
                                    Context.MODE_PRIVATE).getString(Constants.PREFERENCE_LAST_USER, "");
                            LoginData login = database.loginDAO().loadAllByUsername(username);
                            int count = database.customerDAO().getCount();

                            final Customer customer = new Customer();
                            customer.setAddress("");
                            customer.setCustName(edtCustomerSearch.getText().toString());
                            customer.setPhone("");
                            customer.setCustId(AppUtil.getNextId(login, count));
                            customer.setInserted(true);
//                            customer.setDateInserted(Calendar.getInstance().getTime());

                            database.customerDAO().insertAll(customer);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    invoiceViewModel.setCustomer(customer);
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                    cusAdapter.onItemAdd(customer);
                                }
                            });
                        }
                    }).start();
                }
            }
        });
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerCustomers.getContext(),
                layoutManager.getOrientation());
        recyclerCustomers.addItemDecoration(dividerItemDecoration);

        invoiceViewModel.getCustomerList().observe(this, new Observer<List<Customer>>() {
            @Override
            public void onChanged(List<Customer> customers) {
                cusAdapter.notifyDataSetChanged();
            }
        });

        invoiceViewModel.getCustomer().observe(this, new Observer<Customer>() {
            @Override
            public void onChanged(Customer customer) {
                txtInvoiceTo.setText(customer.getCustName());
            }
        });

        txtInvoiceTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) customerSheet.getLayoutParams();
                params.height = (int) (AppUtil.getDeviceHeight(InvoiceActivity.this) * Constants.BOTTOM_SHEET_HEIGHT);
                customerSheet.setLayoutParams(params);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        edtCustomerSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                cusAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void setAddItemBottomSheet() {
        final View addItemSheet = findViewById(R.id.bottonsheet_add_item);
        RecyclerView recyclerAddItem = findViewById(R.id.recycler_add_item);
        final EditText edtAddItemSearch = findViewById(R.id.edt_add_item_search);

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(addItemSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setSkipCollapsed(true);

        final ItemAdapter itemAdapter = new ItemAdapter(this, invoiceViewModel.getAddItems().getValue());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerAddItem.setLayoutManager(layoutManager);
        recyclerAddItem.setAdapter(itemAdapter);
        itemAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(Object object) {
                setAddQtyBottomSheet((Item) object);
                edtAddItemSearch.setText("");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerAddItem.getContext(),
                layoutManager.getOrientation());
        recyclerAddItem.addItemDecoration(dividerItemDecoration);

        invoiceViewModel.getAddItems().observe(this, new Observer<List<Item>>() {
            @Override
            public void onChanged(List<Item> items) {
                itemAdapter.notifyDataSetChanged();
            }
        });

        addItemListener = new OnAddItemListener() {
            @Override
            public void onAddItemClick() {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) addItemSheet.getLayoutParams();
                params.height = (int) (AppUtil.getDeviceHeight(InvoiceActivity.this) * 0.9);
                addItemSheet.setLayoutParams(params);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        };

        edtAddItemSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                itemAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void setAddQtyBottomSheet(final Item item) {
        final EditText edtAddQuantity = qtySheet.findViewById(R.id.edt_add_quantity);
        final EditText edtUnitPrice = qtySheet.findViewById(R.id.edt_set_quantity_price);
        TextView txtTitle = qtySheet.findViewById(R.id.text_add_quantity_title);
        TextView txtSubTitle = qtySheet.findViewById(R.id.text_add_quantity_sub_title);
        final TextView txtPrice = qtySheet.findViewById(R.id.text_add_quantity_price);
        Button btnAdd = qtySheet.findViewById(R.id.btn_add_quantity);
        isQtyShown = false;

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(qtySheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setSkipCollapsed(true);

        qtySheet.post(new Runnable() {
            @Override
            public void run() {
                if (!isQtyShown) {
                    isQtyShown = true;
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) qtySheet.getLayoutParams();
                    params.height = (int) (AppUtil.getDeviceHeight(InvoiceActivity.this) * Constants.BOTTOM_SHEET_HEIGHT);
                    qtySheet.setLayoutParams(params);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        txtTitle.setText(String.format(Locale.getDefault(), "#%d-%s", item.getItemId(), item.getItemName()));
        txtSubTitle.setText(R.string.one_pcs_price);
        edtUnitPrice.setText(String.format(Locale.getDefault(), "%,.2f", item.getPrice()));
        txtPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", item.getPrice()));

        edtAddQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String qty = edtAddQuantity.getText().toString();
                String unitPriceTxt = edtUnitPrice.getText().toString();

                unitPriceTxt = unitPriceTxt.replaceAll(",", "");

                if (unitPriceTxt.equals("") || (Double.parseDouble(unitPriceTxt) <= 0)) {
                    txtPrice.setText("");
                    return;
                }

                if (!qty.equals("") && !qty.equals(".") && Float.parseFloat(qty) > 0) {
                    double unitPrice = Double.parseDouble(unitPriceTxt);
                    double total = unitPrice * Float.parseFloat(qty);
                    txtPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", total));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        edtUnitPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String qty = edtAddQuantity.getText().toString();
                String unitPriceTxt = edtUnitPrice.getText().toString();

                unitPriceTxt = unitPriceTxt.replaceAll(",", "");

                if (unitPriceTxt.equals("") || (Double.parseDouble(unitPriceTxt) <= 0)) {
                    txtPrice.setText("");
                    return;
                }

                if (qty.equals("") || qty.equals(".") || Float.parseFloat(qty) <= 0) {
                    qty = "1";
                }

                double unitPrice = Double.parseDouble(unitPriceTxt);
                double total = unitPrice * Float.parseFloat(qty);
                txtPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", total));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String qty = edtAddQuantity.getText().toString();
                String unitPriceTxt = edtUnitPrice.getText().toString().replaceAll(",", "");

                if (unitPriceTxt.equals("") || (Double.parseDouble(unitPriceTxt) <= 0)) {
                    Toast.makeText(InvoiceActivity.this, "Please enter valid unit price", Toast.LENGTH_LONG).show();
                    return;
                }

                if (qty.equals("") || Float.parseFloat(qty) <= 0)
                    qty = "1";
                edtAddQuantity.setText("");
                double unitPrice = Double.parseDouble(unitPriceTxt);

                InvoiceItem invoiceItem = new InvoiceItem();
                invoiceItem.setQuantity(Integer.parseInt(qty));
                invoiceItem.setItem(item);
                invoiceItem.setItemId(item.getItemId());
                invoiceItem.setItemName(item.getItemName());
                invoiceItem.setUnitPrice(unitPrice);
                double totalPrice = unitPrice * invoiceItem.getQuantity();
                invoiceItem.setPrice(totalPrice);

                if (invoiceViewModel.getItems().getValue() != null) {
                    int size = invoiceViewModel.getItems().getValue().size();
                    int newPos = getItemPosition(InvoiceItemAdapter.VIEW_TYPE_ADD);
                    newPos = Math.max(newPos, 0);
                    invoiceViewModel.getItems().getValue().add(newPos, invoiceItem);
                    adapter.notifyItemInserted(newPos);
                    adapter.notifyItemChanged(getItemPosition(InvoiceItemAdapter.VIEW_TYPE_TOTAL));
                    adapter.notifyItemChanged(getItemPosition(InvoiceItemAdapter.VIEW_TYPE_BALANCE));
                    if (size == 3)
                        recyclerInvoiceItems.smoothScrollToPosition(0);
                }
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppUtil.hideKeyboard(InvoiceActivity.this);
                    }
                }, 500);
            }
        });

    }

    private int getItemPosition(int type) {
        int i = 0;

        if (invoiceViewModel.getItems().getValue() != null)
            for (InvoiceItem invoiceItem : invoiceViewModel.getItems().getValue()) {
                if (invoiceItem.getInvoiceId() == type) {
                    return i;
                }
                i++;
            }
        return -1;
    }

    private void showPaymentBottomSheet() {
        final Spinner spnPaymentType = paymentBottomSheet.findViewById(R.id.spn_payment_type);
        final EditText edtChequeNo = paymentBottomSheet.findViewById(R.id.edt_payment_cheque_no);
        final TextView txtChequeNo = paymentBottomSheet.findViewById(R.id.txt_payment_cheque_no);
        final EditText edtAmount = paymentBottomSheet.findViewById(R.id.edt_payment_amount);
        Button btnPay = paymentBottomSheet.findViewById(R.id.btn_payment_proceed);
        isQtyShown = false;

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(paymentBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setSkipCollapsed(true);

        paymentBottomSheet.post(new Runnable() {
            @Override
            public void run() {
                if (!isQtyShown) {
                    isQtyShown = true;
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) paymentBottomSheet.getLayoutParams();
                    params.height = (int) (AppUtil.getDeviceHeight(InvoiceActivity.this) * Constants.BOTTOM_SHEET_HEIGHT);
                    paymentBottomSheet.setLayoutParams(params);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
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
                if (isValidData()) {
                    String amount = edtAmount.getText().toString();
                    try {
                        if (!amount.equals("")) {

                            String paymentType = Constants.PAYMENT_TYPES[spnPaymentType.getSelectedItemPosition()];
                            String chequeNo = edtChequeNo.getText().toString();

                            createPaymentObject(paymentType, chequeNo, Double.parseDouble(amount));
                            showPaymentInfo(paymentType, chequeNo, Double.parseDouble(amount));
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        } else
                            Toast.makeText(InvoiceActivity.this, "Please enter valid amount", Toast.LENGTH_LONG).show();
                    } catch (NumberFormatException ex) {
                        Toast.makeText(InvoiceActivity.this, "Please enter valid amount", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
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

        if (invoiceViewModel.getItems().getValue() != null) {
            int pos = getItemPosition(InvoiceItemAdapter.VIEW_TYPE_PAYMENT_AMOUNT);
            if (pos < 0) {
                pos = getItemPosition(InvoiceItemAdapter.VIEW_TYPE_PAYMENT);
                invoiceViewModel.getItems().getValue().add(pos, item);
                adapter.notifyItemInserted(pos);

                InvoiceItem balanceItem = new InvoiceItem(); //balance row
                balanceItem.setInvoiceId(InvoiceItemAdapter.VIEW_TYPE_BALANCE);
                invoiceViewModel.getItems().getValue().add(invoiceViewModel.getItems().getValue().size() - 1, balanceItem);
                adapter.notifyItemInserted(invoiceViewModel.getItems().getValue().size() - 1);
            } else {
                invoiceViewModel.getItems().getValue().set(pos, item);
                adapter.notifyItemChanged(pos);
                adapter.notifyItemChanged(invoiceViewModel.getItems().getValue().size() - 1); //balance item value reset
            }
        }
    }

    private void createPaymentObject(String method, String chequeNo, double amount) {
        payment = new Payment();
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setDetails(chequeNo);
        payment.setDate(Calendar.getInstance().getTime());
    }

    @Override
    protected void callCreatePDF() {
        if (isValidData()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PaymentDAO paymentDAO = InventoryDatabase.getInstance(InvoiceActivity.this).paymentDAO();
                    final List<Payment> payments = paymentDAO.getPaymentsForInvoice(String.valueOf(invoiceViewModel.getInvoice().getValue().getId()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            createPDFFile(invoiceViewModel.getInvoiceNo().getValue(), invoiceViewModel.getDate().getValue(),
                                    invoiceViewModel.getCustomer().getValue(), invoiceViewModel.getItems().getValue(), payments,
                                    invoiceViewModel.getTotal().getValue());
                        }
                    });
                }
            });
        }
    }

    private interface OnAddItemListener {
        void onAddItemClick();
    }

    private void finishInvoice() {
        Invoice temp = invoiceViewModel.getInvoice().getValue();
        List<InvoiceItem> items = invoiceViewModel.getItems().getValue();
        if ((temp == null || temp.getId() == 0) && (items != null && items.size() > 1))
            showSaveDataDialog();
        else
            finish();
    }

    private void showSaveDataDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.exit);
        builder.setMessage(R.string.exit_message_invoice);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveData(true);
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

    private void showExitDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.save);
        builder.setMessage(R.string.save_message_invoice);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveData(true);
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


    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_BLUETOOTH:
                case PERMISSION_BLUETOOTH_ADMIN:
                case PERMISSION_BLUETOOTH_CONNECT:
                case PERMISSION_BLUETOOTH_SCAN:
                    this.checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
                    break;
            }
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted onBluetoothPermissionsGranted) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_BLUETOOTH);
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_BLUETOOTH_SCAN);
        } else {
            this.onBluetoothPermissionsGranted.onPermissionsGranted();
        }
    }

    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice() {
        this.checkBluetoothPermissions(this::printBluetooth);

    }

    public void printBluetooth() {
        this.checkBluetoothPermissions(() -> {

            List<Payment> payments = new ArrayList<>();
            if (payment != null)
                payments.add(payment);

            proceedToPrint(invoiceViewModel.getInvoiceNo().getValue(), invoiceViewModel.getDate().getValue(),
                    invoiceViewModel.getCustomer().getValue(), invoiceViewModel.getItems().getValue(), payments,
                    invoiceViewModel.getTotal().getValue());
            // saveData(true);

        });
    }

}
