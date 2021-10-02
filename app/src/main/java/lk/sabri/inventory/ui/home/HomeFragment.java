package lk.sabri.inventory.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lk.sabri.inventory.R;
import lk.sabri.inventory.activity.invoice_show.InvoiceShowActivity;
import lk.sabri.inventory.activity.invoice.InvoiceActivity;
import lk.sabri.inventory.adapter.OnItemClickListener;
import lk.sabri.inventory.adapter.invoice.InvoiceAdapter;
import lk.sabri.inventory.data.Customer;
import lk.sabri.inventory.data.InventoryDatabase;
import lk.sabri.inventory.data.Invoice;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.LoginObject;
import lk.sabri.inventory.data.Payment;
import lk.sabri.inventory.data.SyncObject;
import lk.sabri.inventory.data.UploadData;
import lk.sabri.inventory.util.APIClient;
import lk.sabri.inventory.util.APIInterface;
import lk.sabri.inventory.util.AppUtil;
import lk.sabri.inventory.util.Constants;
import lk.sabri.inventory.util.TimeFormatter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    //constants
    private static final int REQUEST_CODE_ADD = 101;

    //instances
    private HomeViewModel homeViewModel;
    private SharedPreferences preferences;
    private InvoiceHandler invoiceHandler;
    private static InvoiceAdapter adapter;
    private Context context;

    //views
    private RecyclerView recyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        preferences = root.getContext().getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);

        invoiceHandler = new InvoiceHandler(homeViewModel);

        readData(root.getContext());
        setViews(root);

        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_ADD) {
            Invoice invoice = (Invoice) data.getSerializableExtra(Constants.EXTRA_INVOICE_INFO);
            List<Invoice> invoices = homeViewModel.getInvoiceList().getValue();
            if (invoices != null)
                homeViewModel.getInvoiceList().getValue().add(0, invoice);
            adapter.notifyItemInserted(0);

            if (homeViewModel.getCount().getValue() != null)
                homeViewModel.setCount(homeViewModel.getCount().getValue() + 1);

            if (homeViewModel.getTotal().getValue() != null && invoice != null)
                homeViewModel.getTotal().setValue(homeViewModel.getTotal().getValue() + invoice.getTotal());

            recyclerView.smoothScrollToPosition(0);
        }
    }

    private void readData(final Context context) {
        long time = preferences.getLong(Constants.PREFERENCE_LAST_SYNC, 0L);
        homeViewModel.setLastSync(new Date(time));

        homeViewModel.getInvoiceList().setValue(new ArrayList<Invoice>());

        final InventoryDatabase database = InventoryDatabase.getInstance(context);

        new Thread(new Runnable() {
            @Override
            public void run() {
//                Item item1 = new Item();
//                item1.setItemId(64647);
//                item1.setItemName("Apple");
//                item1.setPrice(40);
//
//                Item item2 = new Item();
//                item2.setItemId(62849);
//                item2.setItemName("Oranges");
//                item2.setPrice(50);
//
//                Item item3 = new Item();
//                item3.setItemId(42637);
//                item3.setItemName("Grapes");
//                item3.setPrice(80);
//
//                Customer customer1 = new Customer();
//                customer1.setCustId(1);
//                customer1.setCustName("Nuwan Perea");
//                customer1.setAddress("5D, Keselwatta road, Maharagama");
//
//                Customer customer2 = new Customer();
//                customer2.setCustId(2);
//                customer2.setCustName("Felix Mendis");
//                customer2.setAddress("8, Main Street, Colombo");
//
//                Customer customer3 = new Customer();
//                customer3.setCustId(3);
//                customer3.setCustName("Suresh De Silva");
//                customer3.setAddress("1C, 4th Lane, Rathmalana");
//
//                if (database.customerDAO().getCount() == 0)
//                    database.customerDAO().insertAll(customer1, customer2, customer3);
//                if (database.itemDAO().getCount() == 0)
//                    database.itemDAO().insertAll(item1, item2, item3);

                double totalCount = 0.0;
                int count = 0;
                List<Invoice> invoices = database.invoiceDAO().getAll();
                for (Invoice invoice : invoices) {
                    Customer customer = database.customerDAO().loadAllById(invoice.getCustomerId());
                    invoice.setCustomer(customer);
                    if (TimeFormatter.isToday(invoice.getDate())) {
                        totalCount += invoice.getTotal();
                        count++;
                    }
                }
                Message msg = new Message();
                msg.obj = invoices;
                msg.arg1 = (int) (totalCount * 100);
                msg.arg2 = count;
                invoiceHandler.sendMessage(msg);
            }
        }).start();
    }

    private void setViews(final View root) {
        final TextView txtTotal = root.findViewById(R.id.text_home_total);
        final TextView txtCount = root.findViewById(R.id.text_home_invoices_count);
        final TextView txtLastSync = root.findViewById(R.id.text_home_last_sync);
        recyclerView = root.findViewById(R.id.recycler_home_invoices);
        final ProgressBar progressSync = root.findViewById(R.id.progress_home_sync);

        homeViewModel.getTotal().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(@Nullable Double s) {
                String originalString = String.format(Locale.getDefault(), "Rs. %,.2f", s);
                Spannable span = new SpannableString(originalString);
                span.setSpan(new RelativeSizeSpan(1.5f), 3, originalString.length() - 3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                txtTotal.setText(span);
            }
        });
        homeViewModel.getCount().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer s) {
                txtCount.setText(String.format(Locale.getDefault(), "of %d invoices", s));
            }
        });
        homeViewModel.getInvoiceList().observe(getViewLifecycleOwner(), new Observer<List<Invoice>>() {
            @Override
            public void onChanged(List<Invoice> invoices) {
                adapter.notifyDataSetChanged();
            }
        });

        homeViewModel.getLastSync().observe(getViewLifecycleOwner(), new Observer<Date>() {
            @Override
            public void onChanged(Date date) {
                String time = TimeFormatter.getFormattedDate("yyyy", date);
                if (Integer.parseInt(time) == 1970) {
                    txtLastSync.setText(R.string.never);
                    callToSync(progressSync);
                } else {
                    time = TimeFormatter.getFormattedDate("dd-MM-yy HH:mm", date);
                    txtLastSync.setText(String.format(Locale.getDefault(), "Last sync %s", time));
                }
            }
        });

        root.findViewById(R.id.btn_home_create_invoice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = context.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
                        .getString(Constants.PREFERENCE_LAST_USER, "");
                if (!username.toLowerCase().equals("evergreen"))
                    startActivityForResult(new Intent(root.getContext(), InvoiceActivity.class), REQUEST_CODE_ADD);
            }
        });

        root.findViewById(R.id.btn_home_synchronize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressSync.setVisibility(View.VISIBLE);

                callToSync(progressSync);
            }
        });

        setInvoiceAdapter(root.getContext(), recyclerView);
    }

    private void setInvoiceAdapter(final Context context, RecyclerView recyclerView) {
        adapter = new InvoiceAdapter(context, homeViewModel.getInvoiceList().getValue());
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(Object object) {
                Intent intent = new Intent(context, InvoiceShowActivity.class);
                intent.putExtra(Constants.EXTRA_INVOICE_INFO, (Invoice) object);
                startActivity(intent);
            }
        });
    }

    private void callToSync(final ProgressBar progressSync) {

        AppUtil.checkInternetConnection(context, new AppUtil.OnConnectionCheck() {
            @Override
            public void onHasConnection(boolean hasConnection) {
                if (hasConnection) {

                    final InventoryDatabase database = InventoryDatabase.getInstance(context);
                    Gson gson = new GsonBuilder()
                            .setDateFormat("yyyy-MM-dd HH:mm:ss").create();

                    String json = gson.toJson(getSyncData(database));

                    try {
                        APIClient.getClient().create(APIInterface.class).syncData(new JSONObject(json)).enqueue(new Callback<SyncObject>() {
                            @Override
                            public void onResponse(@NonNull Call<SyncObject> call, @NonNull Response<SyncObject> response) {
                                progressSync.setVisibility(View.GONE);

                                Date synDate = new Date();
                                if (response.body() != null) {
                                    String timeString = response.body().getSync_data().getTime();
                                    try {
                                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                                        synDate = formatter.parse(timeString);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }

                                    preferences.edit().putLong(Constants.PREFERENCE_LAST_SYNC, synDate.getTime()).apply();
                                    homeViewModel.setLastSync(synDate);

                                    final SyncObject syncObject = response.body();

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            database.customerDAO().updateUserStatus();
                                            database.customerDAO().insertAll(syncObject.getBundle().getCustomers());
                                            database.itemDAO().insertAll(syncObject.getBundle().getItems());
                                        }
                                    }).start();

                                    Toast.makeText(context, "Sync Successful", Toast.LENGTH_LONG).show();
                                } else
                                    Toast.makeText(context, "Sync Failed", Toast.LENGTH_LONG).show();

                                getUsers();
                            }

                            @Override
                            public void onFailure(@NonNull Call<SyncObject> call, @NonNull Throwable t) {
                                progressSync.setVisibility(View.GONE);
                                if (context != null)
                                    Toast.makeText(context, "Sync Failed", Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Activity activity = getActivity();
                    if (activity != null)
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "No internet Connection!", Toast.LENGTH_LONG).show();
                            }
                        });
                }
            }
        });
    }

    private UploadData getSyncData(InventoryDatabase database) {
        long time = preferences.getLong(Constants.PREFERENCE_LAST_SYNC, 0);
        Date syncDate = new Date(time);

        List<Invoice> invoices = database.invoiceDAO().getAll(syncDate);
        for (Invoice invoice : invoices) {
            List<InvoiceItem> invoiceItems = database.invoiceItemDAO().loadAllByIds(invoice.getId());
            invoice.setItems(invoiceItems);
//                        invoice.setCustomer(database.customerDAO().loadAllById(invoice.getCustomerId()));
        }
        List<Customer> customers = database.customerDAO().getUserInserted();
        List<Payment> payments = database.paymentDAO().getAll(syncDate);

        String formattedDate = TimeFormatter.getFormattedDate("yyyy-MM-dd HH:mm", new Date(time));

        UploadData uploadData = new UploadData();
        uploadData.setCustomer(customers);
        uploadData.setInvoices(invoices);
        uploadData.setPayments(payments);
        uploadData.setLastSync(formattedDate);

        return uploadData;
    }

    private void getUsers() {
        APIClient.getClient().create(APIInterface.class).getUsers().enqueue(new Callback<LoginObject>() {
            @Override
            public void onResponse(Call<LoginObject> call, Response<LoginObject> response) {
                if (response.body() != null) {
                    final LoginObject object = response.body();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InventoryDatabase database = InventoryDatabase.getInstance(context);
                            database.loginDAO().insertAll(object.getLogin_data());
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Call<LoginObject> call, Throwable t) {

            }
        });
    }

    static class InvoiceHandler extends Handler {

        HomeViewModel homeViewModel;

        InvoiceHandler(HomeViewModel homeViewModel) {
            this.homeViewModel = homeViewModel;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            List<Invoice> invoices = new ArrayList<>();
            if (msg.obj instanceof List<?>) {
                List<?> obj = (List<?>) msg.obj;
                if (obj.size() > 0 && obj.get(0) instanceof Invoice)
                    invoices = (List<Invoice>) obj;
            }

            if (homeViewModel.getInvoiceList().getValue() != null) {
                homeViewModel.getInvoiceList().getValue().clear();
                homeViewModel.getInvoiceList().getValue().addAll(invoices);
                adapter.notifyDataSetChanged();
            }
            homeViewModel.setTotal(msg.arg1 / 100.0);
            homeViewModel.setCount(msg.arg2);
        }
    }
}
