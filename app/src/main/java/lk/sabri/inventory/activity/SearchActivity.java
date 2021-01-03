package lk.sabri.inventory.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import lk.sabri.inventory.R;
import lk.sabri.inventory.activity.invoice_show.InvoiceShowActivity;
import lk.sabri.inventory.adapter.OnItemClickListener;
import lk.sabri.inventory.adapter.invoice.InvoiceAdapter;
import lk.sabri.inventory.data.Customer;
import lk.sabri.inventory.data.InventoryDatabase;
import lk.sabri.inventory.data.Invoice;
import lk.sabri.inventory.util.Constants;

public class SearchActivity extends AppCompatActivity {

    //views
    private RecyclerView recyclerView;
    private TextView txtResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        recyclerView = findViewById(R.id.recycler_search_invoices);
        txtResult = findViewById(R.id.text_search_result);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            readDatabase(query);
        }
    }

    private void readDatabase(final String query) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                InventoryDatabase database = InventoryDatabase.getInstance(SearchActivity.this);

                //get for invoice no
                final List<Invoice> invoices = database.invoiceDAO().searchForId(String.format(Locale.getDefault(), "%%%s%%", query));
                for (Invoice invoice : invoices) {
                    Customer customer = database.customerDAO().loadAllById(invoice.getCustomerId());
                    invoice.setCustomer(customer);
                }

                //Get for customer name
                List<Customer> customers = database.customerDAO().findByName(String.format(Locale.getDefault(), "%%%s%%", query));
                for (Customer customer: customers) {
                    List<Invoice> custInvoice = database.invoiceDAO().getByCustomer(customer.getCustId());
                    for (Invoice invoice : custInvoice) {
                        invoice.setCustomer(customer);
                    }
                    invoices.addAll(custInvoice);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (invoices.size() > 0) {
                            setInvoiceAdapter(invoices);
                            txtResult.setVisibility(View.GONE);
                        } else
                            txtResult.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private void setInvoiceAdapter(List<Invoice> invoices) {
        InvoiceAdapter adapter = new InvoiceAdapter(this, invoices);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(Object object) {
                Intent intent = new Intent(SearchActivity.this, InvoiceShowActivity.class);
                intent.putExtra(Constants.EXTRA_INVOICE_INFO, (Invoice) object);
                startActivity(intent);
            }
        });
    }
}