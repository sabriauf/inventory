package lk.sabri.inventory.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface InvoiceItemDAO {

    @Query("SELECT * FROM InvoiceItem")
    List<InvoiceItem> getAll();

    @Query("SELECT * FROM InvoiceItem WHERE invoiceId = (:invoice)")
    List<InvoiceItem> loadAllByIds(int invoice);

    @Query("SELECT COUNT(id) FROM InvoiceItem")
    int getCount();

    @Insert
    void insertAll(InvoiceItem... invoices);

    @Insert
    void insertAll(List<InvoiceItem> invoices);

    @Delete
    void delete(InvoiceItem invoice);
}
