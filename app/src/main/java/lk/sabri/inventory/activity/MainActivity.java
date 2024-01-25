package lk.sabri.inventory.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

import lk.sabri.inventory.R;
import lk.sabri.inventory.data.Customer;
import lk.sabri.inventory.data.InventoryDatabase;
import lk.sabri.inventory.data.Invoice;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.LoginObject;
import lk.sabri.inventory.data.Payment;
import lk.sabri.inventory.data.SyncObject;
import lk.sabri.inventory.data.UploadData;
import lk.sabri.inventory.ui.home.HomeFragment;
import lk.sabri.inventory.ui.home.HomeViewModel;
import lk.sabri.inventory.util.APIClient;
import lk.sabri.inventory.util.APIInterface;
import lk.sabri.inventory.util.Constants;
import lk.sabri.inventory.util.TimeFormatter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

//    private AppBarConfiguration mAppBarConfiguration;
    private HomeViewModel homeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
//        DrawerLayout drawer = findViewById(R.id.drawer_layout);
//        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        mAppBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.nav_home, R.id.nav_logout)
//                .setDrawerLayout(drawer)
//                .build();
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
//        NavigationUI.setupWithNavController(navigationView, navController);
//        navigationView.getMenu().findItem(R.id.nav_logout).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem menuItem) {
//                startActivity(new Intent(MainActivity.this, LoginActivity.class));
//                finish();
//                return true;
//            }
//        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Intent startIntent = new Intent(this, SearchActivity.class);
        startIntent.setAction(Intent.ACTION_SEARCH);
        startIntent.putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY));
        startActivity(startIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.sync_all){
            syncAllInvoices();
        }
        return true;
    }

    private void syncAllInvoices(){
        Executors.newSingleThreadExecutor()
                .execute(()->{
                    SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
                    long time = preferences.getLong(Constants.PREFERENCE_LAST_SYNC, 0);
                    Date syncDate = new Date(time);

                    final InventoryDatabase database = InventoryDatabase.getInstance(MainActivity.this);
                    Gson gson = new GsonBuilder()
                            .setDateFormat("yyyy-MM-dd HH:mm:ss").create();

                    List<Invoice> invoices = database.invoiceDAO().getAllInvoices();
                    for (Invoice invoice : invoices) {
                        List<InvoiceItem> invoiceItems = database.invoiceItemDAO().loadAllByIds(invoice.getId());
                        invoice.setItems(invoiceItems);
//                        invoice.setCustomer(database.customerDAO().loadAllById(invoice.getCustomerId()));
                    }
                    List<Customer> customers = database.customerDAO().getUserInserted();
                    List<Payment> payments = database.paymentDAO().getAllPayments();

                    String formattedDate = TimeFormatter.getFormattedDate("yyyy-MM-dd HH:mm", new Date(time));

                    UploadData uploadData = new UploadData();
                    uploadData.setCustomer(customers);
                    uploadData.setInvoices(invoices);
                    uploadData.setPayments(payments);
                    uploadData.setLastSync(formattedDate);

                    String json = gson.toJson(uploadData);
                    syncData(json,preferences,database);

                });
    }

    private void syncData(String json , SharedPreferences preferences,InventoryDatabase database){

        try {
            APIClient.getClient().create(APIInterface.class).syncData(new JSONObject(json)).enqueue(new Callback<SyncObject>() {
                @Override
                public void onResponse(@NonNull Call<SyncObject> call, @NonNull Response<SyncObject> response) {

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
                        runOnUiThread(()->{
                            Date dt = new Date();
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                            try {
                                dt = formatter.parse(timeString);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            homeViewModel.setLastSync(dt);
                        });

                        final SyncObject syncObject = response.body();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                database.customerDAO().updateUserStatus();
                                database.customerDAO().insertAll(syncObject.getBundle().getCustomers());
                                database.itemDAO().insertAll(syncObject.getBundle().getItems());
                            }
                        }).start();

                        runOnUiThread(()-> Toast.makeText(MainActivity.this, "Sync Successful", Toast.LENGTH_LONG).show());
                    } else
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Sync Failed", Toast.LENGTH_LONG).show());

                    getUsers();
                }

                @Override
                public void onFailure(@NonNull Call<SyncObject> call, @NonNull Throwable t) {
                   // progressSync.setVisibility(View.GONE);
                 //   if (context != null)
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Sync Failed", Toast.LENGTH_LONG).show());
                }
            });
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

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
                            InventoryDatabase database = InventoryDatabase.getInstance(MainActivity.this);
                            database.loginDAO().insertAll(object.getLogin_data());
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Call<LoginObject> call, Throwable t) {
                Log.d(HomeFragment.class.getSimpleName(), t.getMessage());
            }
        });
    }


    //    @Override
//    public boolean onSupportNavigateUp() {
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
//                || super.onSupportNavigateUp();
//    }
}
