package lk.sabri.inventory.activity.invoice;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lk.sabri.inventory.data.Customer;
import lk.sabri.inventory.data.Invoice;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.Item;

public class InvoiceViewModel extends ViewModel {

    private MutableLiveData<String> invoiceNo;
    private MutableLiveData<Date> date;
    private MutableLiveData<List<InvoiceItem>> items;
    private MutableLiveData<List<Item>> addItems;
    private MutableLiveData<Customer> customer;
    private MutableLiveData<List<Customer>> customerList;
    private MutableLiveData<Invoice> invoice;
    private MutableLiveData<Double> total;

    public InvoiceViewModel() {
        invoiceNo = new MutableLiveData<>();
        date = new MutableLiveData<>();
        items = new MutableLiveData<>();
        customer = new MutableLiveData<>();
        customerList = new MutableLiveData<>();
        addItems = new MutableLiveData<>();
        invoice = new MutableLiveData<>();
        total = new MutableLiveData<>();

        date.setValue(Calendar.getInstance().getTime());
    }

    public MutableLiveData<String> getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo.setValue(invoiceNo);
    }

    public MutableLiveData<Date> getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date.setValue(date);
    }

    public MutableLiveData<List<InvoiceItem>> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items.setValue(items);
    }

    public MutableLiveData<Customer> getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer.setValue(customer);
    }

    public MutableLiveData<List<Customer>> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Customer> customerList) {
        this.customerList.setValue(customerList);
    }

    public MutableLiveData<List<Item>> getAddItems() {
        return addItems;
    }

    public void setAddItems(List<Item> addItems) {
        this.addItems.setValue(addItems);
    }

    public MutableLiveData<Double> getTotal() {
        if(total.getValue() == null && items.getValue() != null)
            total.setValue(getTotal(items.getValue()));
        else if(items.getValue() == null)
            total.setValue(0.0);
        return total;
    }

    public void setTotal(MutableLiveData<Double> total) {
        this.total = total;
    }

    public MutableLiveData<Invoice> getInvoice() {
        Invoice invoiceObj = new Invoice();

        List<InvoiceItem> tempList;
        if (items.getValue() != null)
            tempList = new ArrayList<>(items.getValue().subList(0, items.getValue().size() - 1));
        else
            tempList = new ArrayList<>();
        invoiceObj.setItems(tempList);
        invoiceObj.setDate(date.getValue());
        invoiceObj.setInvoiceNo(invoiceNo.getValue());
        invoiceObj.setCustomer(customer.getValue());
        if (customer.getValue() != null)
            invoiceObj.setCustomerId(customer.getValue().getCustId());
        invoiceObj.setItemCount(tempList.size());
        invoiceObj.setTotal(getTotal(tempList));
        invoice.setValue(invoiceObj);
        return invoice;
    }

    public void setInvoice(MutableLiveData<Invoice> invoice) {
        this.invoice = invoice;
    }

    private double getTotal(List<InvoiceItem> itemList) {
        double total = 0.0;

        for (InvoiceItem item : itemList) {
            if (item != null) {
                total += item.getUnitPrice() * item.getQuantity();
            }
        }

        return total;
    }
}
