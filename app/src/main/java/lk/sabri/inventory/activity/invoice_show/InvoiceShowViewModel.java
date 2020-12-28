package lk.sabri.inventory.activity.invoice_show;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import lk.sabri.inventory.data.Invoice;

public class InvoiceShowViewModel extends ViewModel {

    protected MutableLiveData<Invoice> invoice;

    public InvoiceShowViewModel() {
        invoice = new MutableLiveData<>();
    }

    public MutableLiveData<Invoice> getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoiceNo) {
        this.invoice.setValue(invoiceNo);
    }
}
