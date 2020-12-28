package lk.sabri.inventory.ui.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Date;
import java.util.List;

import lk.sabri.inventory.data.Invoice;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<Integer> count;
    private MutableLiveData<Double> total;
    private MutableLiveData<Date> lastSync;
    private MutableLiveData<List<Invoice>> invoiceList;

    public HomeViewModel() {
        count = new MutableLiveData<>();
        total = new MutableLiveData<>();
        lastSync = new MutableLiveData<>();
        invoiceList = new MutableLiveData<>();

        count.setValue(0);
        total.setValue(0.0);
    }

    MutableLiveData<Integer> getCount() {
        return count;
    }

    void setCount(int count) {
        this.count.setValue(count);
    }

    MutableLiveData<Double> getTotal() {
        return total;
    }

    void setTotal(double total) {
        this.total.setValue(total);
    }

    MutableLiveData<List<Invoice>> getInvoiceList() {
        return invoiceList;
    }

    void setInvoiceList(List<Invoice> invoiceList) {
        this.invoiceList.setValue(invoiceList);
    }

    public MutableLiveData<Date> getLastSync() {
        return lastSync;
    }

    public void setLastSync(Date lastSync) {
        this.lastSync.setValue(lastSync);
    }
}