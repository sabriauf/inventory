package lk.sabri.inventory.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

@Dao
public interface InvoiceDAO {

    @Query("SELECT * FROM Invoice ORDER BY id DESC Limit 20")
    List<Invoice> getAll();

//    @Query("SELECT * FROM Invoice ORDER BY id DESC Limit 20")
//    List<Invoice> getTodayInvoices();

    @Query("SELECT * FROM Invoice WHERE date > :time")
    List<Invoice> getAll(Date time);

    @Query("SELECT * FROM Invoice WHERE invoiceNo IN (:invoices)")
    List<Invoice> loadAllByIds(int[] invoices);

    @Query("SELECT * FROM Invoice WHERE invoiceNo LIKE (:invoiceNo)")
    List<Invoice> searchForId(String invoiceNo);

    @Query("SELECT * FROM Invoice WHERE customerId = :custId")
    List<Invoice> getByCustomer(int custId);

    @Query("SELECT MAX(id) FROM invoice")
    int getCount();

    @Insert
    void insertAll(Invoice... invoices);

    @Delete
    void delete(Invoice invoice);

    @Query("SELECT * FROM INVOICE")
    List<Invoice> getAllInvoices();
}
